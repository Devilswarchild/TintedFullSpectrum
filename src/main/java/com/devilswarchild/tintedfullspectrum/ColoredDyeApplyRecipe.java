package com.devilswarchild.tintedfullspectrum;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

// Mirrors vanilla's ArmorDyeRecipe (crafting_special_armordye), which requires the dye ingredient to
// literally be a DyeItem instance -- ours isn't (it carries an arbitrary RGB, not one of the 16 fixed
// DyeColors), so it needs its own recipe rather than hooking into that one. Applies the dye's exact
// stored color rather than vanilla's multi-dye color blending, since we already have a precise target.
public class ColoredDyeApplyRecipe extends CustomRecipe {
    public ColoredDyeApplyRecipe(CraftingBookCategory category) {
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
        // The output is a vanilla item, so it must carry vanilla's own DYED_COLOR to actually
        // render dyed through vanilla's leather-armor rendering pipeline.
        result.set(DataComponents.DYED_COLOR, new DyedItemColor(color.rgb(), true));
        return result;
    }

    // Returns {target, dye} if the grid holds exactly one DYEABLE item and one Colored Dye, else null.
    private static ItemStack[] findInputs(CraftingInput input) {
        ItemStack target = ItemStack.EMPTY;
        ItemStack dye = ItemStack.EMPTY;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.is(ItemTags.DYEABLE)) {
                if (!target.isEmpty()) {
                    return null;
                }
                target = stack;
            } else if (stack.getItem() instanceof ColoredDyeItem) {
                if (!dye.isEmpty()) {
                    return null;
                }
                dye = stack;
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
        return TintedFullSpectrum.COLORED_DYE_APPLY_SERIALIZER.get();
    }
}
