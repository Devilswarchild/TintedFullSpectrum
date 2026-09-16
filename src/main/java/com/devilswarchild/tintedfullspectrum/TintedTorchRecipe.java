package com.devilswarchild.tintedfullspectrum;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

// 1 vanilla Torch + 1 Iron Ingot + 1 dye -> 1 Tinted Torch (the mod's own original custom-bracket
// design, distinct from the vanilla-parity Tinted Vanilla Torch, which needs no iron at all -- see
// ConvertAndDyeRecipe's torch branch). The extra iron ingot is the metal bracket this design has that
// vanilla's plain torch doesn't. The dye slot accepts either a real vanilla DyeItem (any of the 16) or
// this mod's own Colored Dye, same "player's choice, same base materials either way" pattern as
// TintedConcretePowderRecipe/ChromaGlassPaneRecipe/the Terracotta branch of ConvertAndDyeRecipe --
// collapses the old two-step "static torch+iron+Blank Dye recipe, then a separate RecolorRecipe"
// pattern into one step that's already correctly colored. Blank Dye is dropped from this recipe
// entirely (it carried no color at all here, just as filler -- a holdover from before the
// Alembic/Colored Dye system existed; Blank Dye's only real role now is the Alembic's own raw input).
// Re-dyeing an ALREADY-owned Tinted Torch with a different color still goes through the generic
// RecolorRecipe unaffected -- this only replaces how you get the FIRST one.
public class TintedTorchRecipe extends CustomRecipe {
    public TintedTorchRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return findColor(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        TintColorComponent color = findColor(input);
        if (color == null) {
            return ItemStack.EMPTY;
        }
        ItemStack result = new ItemStack(TintedFullSpectrum.TINTED_TORCH_ITEM.get());
        result.set(TintedFullSpectrum.TINT_COLOR.get(), color);
        return result;
    }

    // Returns the resulting color if the grid holds exactly 1 vanilla Torch, exactly 1 Iron Ingot, and
    // exactly 1 dye (this mod's own Colored Dye, or a real vanilla DyeItem).
    private static TintColorComponent findColor(CraftingInput input) {
        TintColorComponent color = null;
        int torchCount = 0;
        int ironCount = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.getItem() == Items.TORCH) {
                torchCount++;
            } else if (stack.getItem() == Items.IRON_INGOT) {
                ironCount++;
            } else if (stack.getItem() instanceof ColoredDyeItem) {
                if (color != null) {
                    return null;
                }
                color = stack.get(TintedFullSpectrum.TINT_COLOR.get());
                if (color == null) {
                    return null;
                }
            } else if (stack.getItem() instanceof DyeItem vanillaDye) {
                if (color != null) {
                    return null;
                }
                color = new TintColorComponent(vanillaDye.getDyeColor().getTextureDiffuseColor());
            } else {
                return null;
            }
        }
        return torchCount == 1 && ironCount == 1 && color != null ? color : null;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 3;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return TintedFullSpectrum.TINTED_TORCH_SERIALIZER.get();
    }
}
