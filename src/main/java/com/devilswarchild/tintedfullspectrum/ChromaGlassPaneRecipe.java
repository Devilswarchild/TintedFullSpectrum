package com.devilswarchild.tintedfullspectrum;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

// 8x minecraft:glass_pane + 1x Colored Dye -> 8x Chroma Glass Pane, mirroring vanilla's own
// 8-raw-plus-1-dye batch ratio for dyeing glass, applied directly to the pane item since there's no
// intermediate tinted block to craft from first. A genuinely new recipe shape (batch 8-in/8-out) --
// distinct from the mod's other 1:1 ConvertAndDyeRecipe/RecolorRecipe/StringToWoolRecipe patterns, so
// it needed its own CustomRecipe rather than reusing one of those. See
// tinted_full_spectrum_glass_handoff.md (flags this same batch shape for Tinted Terracotta/Concrete
// later, which dye the same 8:1->8 way).
public class ChromaGlassPaneRecipe extends CustomRecipe {
    public ChromaGlassPaneRecipe(CraftingBookCategory category) {
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
        ItemStack result = new ItemStack(TintedFullSpectrum.CHROMA_GLASS_PANE_ITEM.get(), 8);
        result.set(TintedFullSpectrum.TINT_COLOR.get(), color);
        return result;
    }

    // Returns the dye's color if the grid holds exactly 8 plain glass panes and exactly 1 Colored Dye.
    private static TintColorComponent findColor(CraftingInput input) {
        TintColorComponent color = null;
        int paneCount = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.getItem() == Items.GLASS_PANE) {
                paneCount++;
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
        return paneCount == 8 && color != null ? color : null;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 9;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return TintedFullSpectrum.CHROMA_GLASS_PANE_SERIALIZER.get();
    }
}
