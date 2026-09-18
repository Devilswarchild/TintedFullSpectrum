package com.devilswarchild.tintedfullspectrum;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.Level;

// A real vanilla shaped-crafting recipe (extends ShapedRecipe, gaining vanilla's own exact
// shape/mirroring match logic via ShapedRecipePattern) -- fixes a genuine collision bug the earlier
// CustomRecipe-based Bricks shape recipes had: those only checked total ingredient COUNT ("exactly 6
// Tinted Bricks somewhere in the grid"), not actual POSITION, so 6 Tinted Bricks arranged as a wall
// (2x3) also satisfied the stairs recipe's own loose count check (and vice versa) -- whichever recipe
// the RecipeManager happened to check first silently won, which is why crafting a wall was producing
// stairs. Real ShapedRecipePattern matching (including vanilla's own mirroring support) makes the
// position matter, so the two shapes can no longer collide.
//
// On top of the real shape check, matches() additionally requires every TINTABLE ingredient actually
// present in the grid to carry the exact same color -- untintable ingredients (Fence Gate's plain
// Stick) are ignored. One Java class, many registered recipe JSON instances (Stage 2 assembly, Slab/
// Stairs/Wall crafting, Fence, Fence Gate) -- pattern/key/result all live in each recipe's own JSON,
// not hardcoded here. See tinted_full_spectrum_bricks_handoff.md's "same-color-match requirement".
public class TintCarryingShapedRecipe extends ShapedRecipe {
    // Own copies -- ShapedRecipe's own pattern/result fields are package-private (net.minecraft.world.
    // item.crafting only), not accessible from this subclass in a different package.
    private final ShapedRecipePattern pattern;
    private final ItemStack result;

    public TintCarryingShapedRecipe(String group, CraftingBookCategory category, ShapedRecipePattern pattern, ItemStack result, boolean showNotification) {
        super(group, category, pattern, result, showNotification);
        this.pattern = pattern;
        this.result = result;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return super.matches(input, level) && findColor(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        TintColorComponent color = findColor(input);
        if (color == null) {
            return ItemStack.EMPTY;
        }
        ItemStack output = result.copy();
        output.set(TintedFullSpectrum.TINT_COLOR.get(), color);
        return output;
    }

    // Every non-empty TintableItem stack in the grid must share the same color; non-tintable
    // ingredients are ignored entirely. Safe to trust the shape is otherwise correct here --
    // super.matches() already confirmed every non-empty cell matches its pattern ingredient.
    private static TintColorComponent findColor(CraftingInput input) {
        TintColorComponent color = null;
        boolean foundTintable = false;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof TintableItem)) {
                continue;
            }
            TintColorComponent stackColor = stack.get(TintedFullSpectrum.TINT_COLOR.get());
            if (stackColor == null || (color != null && !color.equals(stackColor))) {
                return null;
            }
            color = stackColor;
            foundTintable = true;
        }
        return foundTintable ? color : null;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return TintedFullSpectrum.TINT_CARRYING_SHAPED_SERIALIZER.get();
    }

    // Own serializer, not ShapedRecipe.Serializer -- that one's codec constructs plain ShapedRecipe
    // instances directly, not this subtype (same non-reusability as SingleItemRecipe.Serializer for
    // stonecutting). Mirrors its real codec shape (group/category/pattern/result/show_notification)
    // exactly, confirmed via the real decompiled ShapedRecipe.Serializer/CraftingBookCategory.
    public static class Serializer implements RecipeSerializer<TintCarryingShapedRecipe> {
        private static final MapCodec<TintCarryingShapedRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(ShapedRecipe::getGroup),
                CraftingBookCategory.CODEC.optionalFieldOf("category", CraftingBookCategory.MISC).forGetter(ShapedRecipe::category),
                ShapedRecipePattern.MAP_CODEC.forGetter(r -> r.pattern),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(r -> r.result),
                Codec.BOOL.optionalFieldOf("show_notification", true).forGetter(ShapedRecipe::showNotification))
                .apply(instance, TintCarryingShapedRecipe::new));
        private static final StreamCodec<RegistryFriendlyByteBuf, TintCarryingShapedRecipe> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, ShapedRecipe::getGroup,
                CraftingBookCategory.STREAM_CODEC, ShapedRecipe::category,
                ShapedRecipePattern.STREAM_CODEC, r -> r.pattern,
                ItemStack.STREAM_CODEC, r -> r.result,
                ByteBufCodecs.BOOL, ShapedRecipe::showNotification,
                TintCarryingShapedRecipe::new);

        @Override
        public MapCodec<TintCarryingShapedRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, TintCarryingShapedRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
