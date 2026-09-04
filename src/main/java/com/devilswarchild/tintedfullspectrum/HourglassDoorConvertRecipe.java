package com.devilswarchild.tintedfullspectrum;

import net.minecraft.core.HolderLookup;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

// Any vanilla wooden door with a glass pane on either side of it in the same row, plus a Colored
// Dye anywhere else in the grid, converts directly into the Hourglass Door in that color -- the
// glass panes make sense here since the Hourglass Door's own geometry has a real window, unlike the
// plain vanilla-parity Tinted Door (which just goes through the generic ConvertAndDyeRecipe with no
// extra ingredients, same as planks/stairs/slab/fence/fence gate).
public class HourglassDoorConvertRecipe extends CustomRecipe {
    public HourglassDoorConvertRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return findMatch(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack[] found = findMatch(input);
        if (found == null) {
            return ItemStack.EMPTY;
        }
        TintColorComponent color = found[1].get(TintedFullSpectrum.TINT_COLOR.get());
        if (color == null) {
            return ItemStack.EMPTY;
        }
        ItemStack result = new ItemStack(TintedFullSpectrum.HOURGLASS_DOOR_ITEM.get());
        result.set(TintedFullSpectrum.TINT_COLOR.get(), color);
        return result;
    }

    // Returns {door, dye} if the grid has a wooden door flanked by a glass pane on each side in one
    // row, plus exactly one Colored Dye somewhere else and nothing else, else null.
    private static ItemStack[] findMatch(CraftingInput input) {
        int width = input.width();
        int height = input.height();
        if (width < 3) {
            return null;
        }
        for (int row = 0; row < height; row++) {
            for (int col = 1; col < width - 1; col++) {
                // CraftingInput#getItem(a, b) indexes as a + b*width -- the first argument is the
                // fast/column axis (< width), the second the slow/row axis (< height), the reverse of
                // what the names suggest.
                ItemStack door = input.getItem(col, row);
                ItemStack left = input.getItem(col - 1, row);
                ItemStack right = input.getItem(col + 1, row);
                if (isWoodenDoor(door) && isGlassPane(left) && isGlassPane(right)) {
                    ItemStack dye = findSingleDyeElsewhere(input, row, col);
                    if (dye != null) {
                        return new ItemStack[] {door, dye};
                    }
                }
            }
        }
        return null;
    }

    private static boolean isWoodenDoor(ItemStack stack) {
        return !stack.isEmpty() && stack.is(ItemTags.WOODEN_DOORS);
    }

    private static boolean isGlassPane(ItemStack stack) {
        return !stack.isEmpty() && stack.is(Items.GLASS_PANE);
    }

    private static ItemStack findSingleDyeElsewhere(CraftingInput input, int doorRow, int doorCol) {
        int width = input.width();
        int height = input.height();
        ItemStack dye = ItemStack.EMPTY;
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                if (row == doorRow && col >= doorCol - 1 && col <= doorCol + 1) {
                    continue;
                }
                ItemStack stack = input.getItem(col, row);
                if (stack.isEmpty()) {
                    continue;
                }
                if (!(stack.getItem() instanceof ColoredDyeItem) || !dye.isEmpty()) {
                    return null;
                }
                dye = stack;
            }
        }
        return dye.isEmpty() ? null : dye;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 3 && height >= 1;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return TintedFullSpectrum.HOURGLASS_DOOR_CONVERT_SERIALIZER.get();
    }
}
