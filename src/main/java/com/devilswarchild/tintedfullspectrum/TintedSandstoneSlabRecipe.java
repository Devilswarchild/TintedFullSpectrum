package com.devilswarchild.tintedfullspectrum;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

// Mirrors vanilla's own real sandstone_slab.json/red_sandstone_slab.json shaped recipe (3 of
// [Sandstone OR Chiseled Sandstone], interchangeably, -> 6 slabs -- confirmed via the real recipe
// JSON). Tint-carrying, no dye involved -- covers both families via a small dispatch table.
public class TintedSandstoneSlabRecipe extends CustomRecipe {
    public TintedSandstoneSlabRecipe(CraftingBookCategory category) {
        super(category);
    }

    // Returns 0 for a tan-family eligible item, 1 for red-family, -1 if not eligible at all.
    private static int familyOf(Item item) {
        if (item == TintedFullSpectrum.TINTED_SANDSTONE_ITEM.get()
                || item == TintedFullSpectrum.TINTED_CHISELED_SANDSTONE_ITEM.get()) {
            return 0;
        }
        if (item == TintedFullSpectrum.TINTED_RED_SANDSTONE_ITEM.get()
                || item == TintedFullSpectrum.TINTED_CHISELED_RED_SANDSTONE_ITEM.get()) {
            return 1;
        }
        return -1;
    }

    private static Item resultFor(int family) {
        return family == 0 ? TintedFullSpectrum.TINTED_SANDSTONE_SLAB_ITEM.get()
                : family == 1 ? TintedFullSpectrum.TINTED_RED_SANDSTONE_SLAB_ITEM.get() : null;
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
        ItemStack output = new ItemStack(result, 6);
        output.set(TintedFullSpectrum.TINT_COLOR.get(), color);
        return output;
    }

    // Returns {resultItem, color} if the grid holds exactly 3 stacks, all from the SAME family
    // (tan or red, any mix of base/Chiseled within that family) and all carrying the same color.
    private static Object[] findResult(CraftingInput input) {
        int family = -1;
        TintColorComponent color = null;
        int count = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            int stackFamily = familyOf(stack.getItem());
            if (stackFamily < 0) {
                return null;
            }
            if (family < 0) {
                family = stackFamily;
            } else if (family != stackFamily) {
                return null;
            }
            TintColorComponent stackColor = stack.get(TintedFullSpectrum.TINT_COLOR.get());
            if (stackColor == null || (color != null && !color.equals(stackColor))) {
                return null;
            }
            color = stackColor;
            count++;
        }
        if (count != 3 || family < 0) {
            return null;
        }
        Item result = resultFor(family);
        return result != null ? new Object[] {result, color} : null;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 3;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return TintedFullSpectrum.TINTED_SANDSTONE_SLAB_SERIALIZER.get();
    }
}
