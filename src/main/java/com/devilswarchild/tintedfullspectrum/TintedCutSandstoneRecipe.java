package com.devilswarchild.tintedfullspectrum;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

// Mirrors vanilla's own real "2x2 sandstone -> 4 cut sandstone" shaped crafting recipe
// (cut_sandstone.json/cut_red_sandstone.json, confirmed via the real recipe JSON -- the handoff brief
// incorrectly assumed this crafting-table route didn't exist and only the 1:1 stonecutter conversion
// did; both are real and both are mirrored here and in TintCarryingStonecuttingRecipe respectively).
// A genuinely new recipe shape for this mod (4-in/4-out, tint-carrying, no dye involved at all -- the
// color comes from the SOLE input material, not a dye ingredient), covering both families in one
// class via a small dispatch table rather than two near-identical dedicated classes.
public class TintedCutSandstoneRecipe extends CustomRecipe {
    public TintedCutSandstoneRecipe(CraftingBookCategory category) {
        super(category);
    }

    private static Item cutResultFor(Item source) {
        if (source == TintedFullSpectrum.TINTED_SANDSTONE_ITEM.get()) {
            return TintedFullSpectrum.TINTED_CUT_SANDSTONE_ITEM.get();
        } else if (source == TintedFullSpectrum.TINTED_RED_SANDSTONE_ITEM.get()) {
            return TintedFullSpectrum.TINTED_CUT_RED_SANDSTONE_ITEM.get();
        }
        return null;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return findResult(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        Object[] found = findResult(input);
        if (found == null) {
            return ItemStack.EMPTY;
        }
        Item result = (Item) found[0];
        TintColorComponent color = (TintColorComponent) found[1];
        ItemStack output = new ItemStack(result, 4);
        output.set(TintedFullSpectrum.TINT_COLOR.get(), color);
        return output;
    }

    // Returns {resultItem, color} if the grid holds exactly 4 stacks of the SAME Tinted Sandstone or
    // Tinted Red Sandstone item, all carrying the same color.
    private static Object[] findResult(CraftingInput input) {
        Item source = null;
        TintColorComponent color = null;
        int count = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (source != null && stack.getItem() != source) {
                return null;
            }
            source = stack.getItem();
            TintColorComponent stackColor = stack.get(TintedFullSpectrum.TINT_COLOR.get());
            if (stackColor == null || (color != null && !color.equals(stackColor))) {
                return null;
            }
            color = stackColor;
            count++;
        }
        if (count != 4 || source == null) {
            return null;
        }
        Item result = cutResultFor(source);
        return result != null ? new Object[] {result, color} : null;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 4;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return TintedFullSpectrum.TINTED_CUT_SANDSTONE_SERIALIZER.get();
    }
}
