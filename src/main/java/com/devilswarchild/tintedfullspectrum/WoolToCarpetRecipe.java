package com.devilswarchild.tintedfullspectrum;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

// Mirrors vanilla's own "2 wool -> 3 carpet" shape, but for Tinted Wool -> Tinted Carpet, carrying
// the wool's own dyed color over to the carpet -- vanilla's real per-DyeColor wool/carpet recipes
// don't recognize Tinted Wool as an ingredient at all (it isn't any of the 16 fixed colors), so this
// needed its own recipe rather than being coverable by ConvertAndDyeRecipe/RecolorRecipe (neither of
// those turns one of this mod's own tintable items into a different tintable shape). Both wool
// stacks must carry the SAME color, matching vanilla's own same-color-only requirement.
public class WoolToCarpetRecipe extends CustomRecipe {
    public WoolToCarpetRecipe(CraftingBookCategory category) {
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
        ItemStack result = new ItemStack(TintedFullSpectrum.TINTED_CARPET_ITEM.get(), 3);
        result.set(TintedFullSpectrum.TINT_COLOR.get(), color);
        return result;
    }

    // Returns the shared color if the grid holds exactly two Tinted Wool stacks with matching colors.
    private static TintColorComponent findColor(CraftingInput input) {
        TintColorComponent color = null;
        int count = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.getItem() != TintedFullSpectrum.TINTED_WOOL_ITEM.get()) {
                return null;
            }
            TintColorComponent stackColor = stack.get(TintedFullSpectrum.TINT_COLOR.get());
            if (stackColor == null || (color != null && !color.equals(stackColor))) {
                return null;
            }
            color = stackColor;
            count++;
        }
        return count == 2 ? color : null;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return TintedFullSpectrum.WOOL_TO_CARPET_SERIALIZER.get();
    }
}
