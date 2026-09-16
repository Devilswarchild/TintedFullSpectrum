package com.devilswarchild.tintedfullspectrum;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

// Mirrors vanilla's own real "6 stained glass -> 16 stained glass pane" shape
// (red_stained_glass_pane.json, confirmed via the real recipe JSON), for Chroma Stained Glass ->
// Chroma Stained Glass Pane -- a separate recipe from ChromaGlassToPaneRecipe (clear Chroma Glass ->
// clear Chroma Glass Pane), since that one only matches the CHROMA_GLASS_ITEM identity and doesn't
// recognize Chroma Stained Glass at all. All 6 glass stacks must carry the SAME color, matching
// vanilla's own same-color-only requirement (same pattern as WoolToCarpetRecipe/
// ChromaGlassToPaneRecipe).
public class ChromaStainedGlassToPaneRecipe extends CustomRecipe {
    public ChromaStainedGlassToPaneRecipe(CraftingBookCategory category) {
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
        ItemStack result = new ItemStack(TintedFullSpectrum.CHROMA_STAINED_GLASS_PANE_ITEM.get(), 16);
        result.set(TintedFullSpectrum.TINT_COLOR.get(), color);
        return result;
    }

    // Returns the shared color if the grid holds exactly six Chroma Stained Glass stacks with
    // matching colors.
    private static TintColorComponent findColor(CraftingInput input) {
        TintColorComponent color = null;
        int count = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.getItem() != TintedFullSpectrum.CHROMA_STAINED_GLASS_ITEM.get()) {
                return null;
            }
            TintColorComponent stackColor = stack.get(TintedFullSpectrum.TINT_COLOR.get());
            if (stackColor == null || (color != null && !color.equals(stackColor))) {
                return null;
            }
            color = stackColor;
            count++;
        }
        return count == 6 ? color : null;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 6;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return TintedFullSpectrum.CHROMA_STAINED_GLASS_TO_PANE_SERIALIZER.get();
    }
}
