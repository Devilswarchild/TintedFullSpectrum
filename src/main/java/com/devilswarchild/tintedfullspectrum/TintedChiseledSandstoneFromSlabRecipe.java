package com.devilswarchild.tintedfullspectrum;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

// Mirrors vanilla's own real chiseled_sandstone.json/chiseled_red_sandstone.json shaped recipe (2
// Sandstone Slabs stacked vertically -> 1 Chiseled Sandstone). This was flagged as a real dependency
// gap in the original Sandstone handoff (Tinted [Red] Sandstone Slab didn't exist yet) -- now that the
// slabs exist, this closes it. Tint-carrying, no dye involved.
public class TintedChiseledSandstoneFromSlabRecipe extends CustomRecipe {
    public TintedChiseledSandstoneFromSlabRecipe(CraftingBookCategory category) {
        super(category);
    }

    private static Item resultFor(Item source) {
        if (source == TintedFullSpectrum.TINTED_SANDSTONE_SLAB_ITEM.get()) {
            return TintedFullSpectrum.TINTED_CHISELED_SANDSTONE_ITEM.get();
        } else if (source == TintedFullSpectrum.TINTED_RED_SANDSTONE_SLAB_ITEM.get()) {
            return TintedFullSpectrum.TINTED_CHISELED_RED_SANDSTONE_ITEM.get();
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
        ItemStack output = new ItemStack(result, 1);
        output.set(TintedFullSpectrum.TINT_COLOR.get(), color);
        return output;
    }

    // Returns {resultItem, color} if the grid holds exactly 2 stacks of the SAME Tinted [Red]
    // Sandstone Slab item, both carrying the same color.
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
        if (count != 2 || source == null) {
            return null;
        }
        Item result = resultFor(source);
        return result != null ? new Object[] {result, color} : null;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return TintedFullSpectrum.TINTED_CHISELED_SANDSTONE_FROM_SLAB_SERIALIZER.get();
    }
}
