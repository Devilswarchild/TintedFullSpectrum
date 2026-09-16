package com.devilswarchild.tintedfullspectrum;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

// 8x minecraft:glass_pane + 1x dye -> 8x Chroma Stained Glass Pane, mirroring vanilla's own real
// red_stained_glass_pane_from_glass_pane.json recipe (8 glass_pane + 1 dye -> 8, confirmed via the
// real recipe JSON, not guessed) exactly -- vanilla only ever dyes INTO stained glass, never clear,
// so this is the stained pane's own direct-dye acquisition path (clear Chroma Glass Pane instead
// comes from cutting Chroma Glass, see ChromaGlassToPaneRecipe -- no dye involved there either,
// matching vanilla's own real clear-pane recipe). The dye slot accepts either a real vanilla DyeItem
// (any of the 16, untouched vanilla-shape parity) or this mod's own Colored Dye -- same "player's
// choice, same base materials either way" pattern as TintedConcretePowderRecipe. A genuinely new
// recipe shape (batch 8-in/8-out) -- distinct from the mod's other 1:1 ConvertAndDyeRecipe/
// RecolorRecipe/StringToWoolRecipe patterns, so it needed its own CustomRecipe rather than reusing
// one of those.
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
        ItemStack result = new ItemStack(TintedFullSpectrum.CHROMA_STAINED_GLASS_PANE_ITEM.get(), 8);
        result.set(TintedFullSpectrum.TINT_COLOR.get(), color);
        return result;
    }

    // Returns the resulting color if the grid holds exactly 8 plain glass panes and exactly 1 dye --
    // either this mod's own Colored Dye, or a real vanilla DyeItem (mirroring vanilla's own identical
    // 8:1 pane-dyeing recipe, see the class doc above).
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
            } else if (stack.getItem() instanceof DyeItem vanillaDye) {
                if (color != null) {
                    return null;
                }
                color = new TintColorComponent(vanillaDye.getDyeColor().getTextureDiffuseColor());
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
