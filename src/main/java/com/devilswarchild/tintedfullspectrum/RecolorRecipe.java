package com.devilswarchild.tintedfullspectrum;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

// One generic recipe for recoloring ANY TintableItem (tinted torch, Tinted Planks and its
// stairs/slab/fence variants, and any future tintable material) with a Colored Dye -- not one
// recipe per material. Works on a plain/never-dyed tintable item too, since it doesn't require the
// target to already carry TintColorComponent, only to be eligible for one. See
// tintable_system_and_planks.md.
public class RecolorRecipe extends CustomRecipe {
    public RecolorRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return findInputs(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack[] found = findInputs(input);
        if (found == null) {
            return ItemStack.EMPTY;
        }
        TintColorComponent color = found[1].get(TintedFullSpectrum.TINT_COLOR.get());
        if (color == null) {
            return ItemStack.EMPTY;
        }
        ItemStack result = found[0].copyWithCount(1);
        result.set(TintedFullSpectrum.TINT_COLOR.get(), color);
        return result;
    }

    // Returns {target, dye} if the grid holds exactly one TintableItem and one Colored Dye, else null.
    private static ItemStack[] findInputs(CraftingInput input) {
        ItemStack target = ItemStack.EMPTY;
        ItemStack dye = ItemStack.EMPTY;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.getItem() instanceof ColoredDyeItem) {
                if (!dye.isEmpty()) {
                    return null;
                }
                dye = stack;
            } else if (stack.getItem() instanceof TintableItem) {
                if (!target.isEmpty()) {
                    return null;
                }
                target = stack;
            } else {
                return null;
            }
        }
        return !target.isEmpty() && !dye.isEmpty() ? new ItemStack[] {target, dye} : null;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return TintedFullSpectrum.RECOLOR_SERIALIZER.get();
    }
}
