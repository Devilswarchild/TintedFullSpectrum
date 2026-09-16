package com.devilswarchild.tintedfullspectrum;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

// Mirrors vanilla's own real concrete powder recipe (e.g. red_concrete_powder.json: 4 sand + 4
// gravel + 1 dye -> 8 concrete powder) exactly, ingredient-for-ingredient, with a Colored Dye
// standing in for one of the 16 fixed DyeColors -- the vanilla recipes themselves are untouched, so
// a player can still freely choose a plain vanilla dye for plain vanilla concrete powder, or swap in
// a Colored Dye for this mod's Tinted Concrete Powder in that exact color, same base materials
// either way. A genuinely new recipe shape (4+4+1 -> 8, all three ingredient types distinct) --
// unlike ConvertAndDyeRecipe's "existing colored item + dye" batches, this one crafts straight from
// raw sand and gravel like vanilla itself does.
public class TintedConcretePowderRecipe extends CustomRecipe {
    public TintedConcretePowderRecipe(CraftingBookCategory category) {
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
        ItemStack result = new ItemStack(TintedFullSpectrum.TINTED_CONCRETE_POWDER_ITEM.get(), 8);
        result.set(TintedFullSpectrum.TINT_COLOR.get(), color);
        return result;
    }

    // Returns the dye's color if the grid holds exactly 4 sand, exactly 4 gravel, and exactly 1
    // Colored Dye -- vanilla's own real ratio, just with our dye in the dye slot.
    private static TintColorComponent findColor(CraftingInput input) {
        TintColorComponent color = null;
        int sandCount = 0;
        int gravelCount = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.getItem() == Items.SAND) {
                sandCount++;
            } else if (stack.getItem() == Items.GRAVEL) {
                gravelCount++;
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
        return sandCount == 4 && gravelCount == 4 && color != null ? color : null;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 9;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return TintedFullSpectrum.TINTED_CONCRETE_POWDER_SERIALIZER.get();
    }
}
