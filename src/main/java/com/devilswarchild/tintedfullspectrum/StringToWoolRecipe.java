package com.devilswarchild.tintedfullspectrum;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

// Mirrors vanilla's own "4 string -> 1 white wool" shapeless recipe, but takes a Colored Dye too and
// produces Tinted Wool in that color directly -- the base acquisition path for wool, same role
// ConvertAndDyeRecipe plays for wood shapes/torch/grass (vanilla ingredient + dye, one step), just
// starting from string instead of an existing wool block since there's no "convert this vanilla wool
// to tinted wool" step separate from this one -- ConvertAndDyeRecipe already covers that case (any of
// the 16 vanilla wool colors + dye -> Tinted Wool), so this is specifically for making it from raw
// materials instead.
public class StringToWoolRecipe extends CustomRecipe {
    public StringToWoolRecipe(CraftingBookCategory category) {
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
        ItemStack result = new ItemStack(TintedFullSpectrum.TINTED_WOOL_ITEM.get());
        result.set(TintedFullSpectrum.TINT_COLOR.get(), color);
        return result;
    }

    // Returns the dye's color if the grid holds exactly 4 String and exactly 1 Colored Dye.
    private static TintColorComponent findColor(CraftingInput input) {
        TintColorComponent color = null;
        int stringCount = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.getItem() == Items.STRING) {
                stringCount++;
            } else if (stack.getItem() instanceof ColoredDyeItem) {
                if (color != null) {
                    return null;
                }
                color = stack.get(TintedFullSpectrum.TINT_COLOR.get());
                if (color == null) {
                    return null;
                }
            } else {
                return null;
            }
        }
        return stringCount == 4 && color != null ? color : null;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 5;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return TintedFullSpectrum.STRING_TO_WOOL_SERIALIZER.get();
    }
}
