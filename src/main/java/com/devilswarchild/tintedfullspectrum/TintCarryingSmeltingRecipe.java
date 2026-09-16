package com.devilswarchild.tintedfullspectrum;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

// A real furnace smelting recipe -- extends AbstractCookingRecipe (not just Recipe<SingleRecipeInput>
// directly) because AbstractFurnaceBlockEntity hard-casts every recipe it processes to
// AbstractCookingRecipe internally (confirmed via the real decompiled source -- its RecipeType field
// is literally typed RecipeType<? extends AbstractCookingRecipe>), so anything else would throw a
// ClassCastException the moment a player actually tried to smelt with it. Registers under vanilla's
// own RecipeType.SMELTING (via the inherited constructor), so it works natively in the furnace UI and
// is enumerable/JEI-visible like any normal smelting recipe -- unlike this mod's other, dynamic
// CustomRecipe-based conversions. The one actual difference from vanilla's own SmeltingRecipe: this
// overrides assemble() to copy the input's TintColorComponent onto the result instead of vanilla's
// plain result.copy() (which would silently produce a blank/default-colored output, discarding
// whatever color the input actually had), and matches() additionally requires the input to actually
// carry that component. Used for Tinted Sandstone -> Tinted Smooth Sandstone and Tinted Red Sandstone
// -> Tinted Smooth Red Sandstone -- one Java class, two registered recipe JSON instances (source/
// result live in each recipe's own JSON via the inherited ingredient/result fields, not hardcoded
// here) -- matching vanilla's real "smelting is the only path to Smooth" mechanic exactly (confirmed
// via the real smooth_sandstone.json/smooth_red_sandstone.json, not guessed).
public class TintCarryingSmeltingRecipe extends AbstractCookingRecipe {
    public TintCarryingSmeltingRecipe(String group, CookingBookCategory category, Ingredient ingredient, ItemStack result, float experience, int cookingTime) {
        super(RecipeType.SMELTING, group, category, ingredient, result, experience, cookingTime);
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return this.ingredient.test(input.item()) && input.item().has(TintedFullSpectrum.TINT_COLOR.get());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
        TintColorComponent color = input.item().get(TintedFullSpectrum.TINT_COLOR.get());
        if (color == null) {
            return ItemStack.EMPTY;
        }
        ItemStack output = this.result.copy();
        output.set(TintedFullSpectrum.TINT_COLOR.get(), color);
        return output;
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(net.minecraft.world.level.block.Blocks.FURNACE);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return TintedFullSpectrum.TINT_CARRYING_SMELTING_SERIALIZER.get();
    }
}
