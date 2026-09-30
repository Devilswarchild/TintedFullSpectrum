package com.devilswarchild.tintedfullspectrum;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;

// A real furnace smelting recipe -- extends vanilla's own CONCRETE SmeltingRecipe, not just its
// AbstractCookingRecipe base, because AbstractFurnaceBlockEntity hard-casts every recipe it processes
// to AbstractCookingRecipe internally (confirmed via the real decompiled source -- its RecipeType field
// is literally typed RecipeType<? extends AbstractCookingRecipe>), so anything that didn't extend that
// would throw a ClassCastException the moment a player actually tried to smelt with it -- extending
// SmeltingRecipe satisfies that too, since SmeltingRecipe IS an AbstractCookingRecipe.
//
// Originally this extended AbstractCookingRecipe directly (matching the minimum vanilla requires), but
// that broke compatibility with MineColonies, whose own FurnaceRecipes#loadRecipes hard-casts every
// RecipeType.SMELTING entry to the CONCRETE net.minecraft.world.item.crafting.SmeltingRecipe class,
// not just AbstractCookingRecipe (confirmed via the real crash report -- a genuine
// ClassCastException at DataPackSyncEventHandler$ServerEvents#discoverCompatLists). Extending the
// concrete class instead is a strictly safer choice for any other mod making the same reasonable-
// looking assumption ("smelting recipes are SmeltingRecipe instances"), and costs nothing here since
// SmeltingRecipe's own constructor already hardcodes RecipeType.SMELTING for us.
//
// Registers under vanilla's own RecipeType.SMELTING (via the inherited constructor), so it works
// natively in the furnace UI and is enumerable/JEI-visible like any normal smelting recipe -- unlike
// this mod's other, dynamic CustomRecipe-based conversions. The one actual difference from vanilla's
// own SmeltingRecipe: this overrides assemble() to copy the input's TintColorComponent onto the result
// instead of vanilla's plain result.copy() (which would silently produce a blank/default-colored
// output, discarding whatever color the input actually had), and matches() additionally requires the
// input to actually carry that component. Used for Tinted Sandstone -> Tinted Smooth Sandstone,
// Tinted Red Sandstone -> Tinted Smooth Red Sandstone, and Tinted Clay Ball -> Tinted Brick -- one Java
// class, several registered recipe JSON instances (source/result live in each recipe's own JSON via
// the inherited ingredient/result fields, not hardcoded here).
public class TintCarryingSmeltingRecipe extends SmeltingRecipe {
    public TintCarryingSmeltingRecipe(String group, CookingBookCategory category, Ingredient ingredient, ItemStack result, float experience, int cookingTime) {
        super(group, category, ingredient, result, experience, cookingTime);
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
