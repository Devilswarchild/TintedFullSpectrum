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

// 8x minecraft:glass + 1x dye -> 8x Chroma Stained Glass, mirroring vanilla's own real
// red_stained_glass.json (8 glass + 1 dye -> 8, confirmed via the real recipe JSON) exactly -- vanilla
// only ever dyes plain glass INTO stained glass, never keeps it clear, so this is Chroma Stained
// Glass's own acquisition recipe (clear Chroma Glass has no dye mechanic at all, matching vanilla,
// where plain glass is simply never dyed). Same pattern as ChromaGlassPaneRecipe/the Terracotta
// branch of ConvertAndDyeRecipe. The dye slot accepts either a real vanilla DyeItem (any of the 16,
// untouched vanilla-shape parity) or this mod's own Colored Dye.
public class ChromaGlassRecipe extends CustomRecipe {
    public ChromaGlassRecipe(CraftingBookCategory category) {
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
        ItemStack result = new ItemStack(TintedFullSpectrum.CHROMA_STAINED_GLASS_ITEM.get(), 8);
        result.set(TintedFullSpectrum.TINT_COLOR.get(), color);
        return result;
    }

    // Returns the resulting color if the grid holds exactly 8 plain glass blocks and exactly 1 dye --
    // either this mod's own Colored Dye, or a real vanilla DyeItem.
    private static TintColorComponent findColor(CraftingInput input) {
        TintColorComponent color = null;
        int glassCount = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.getItem() == Items.GLASS) {
                glassCount++;
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
        return glassCount == 8 && color != null ? color : null;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 9;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return TintedFullSpectrum.CHROMA_GLASS_SERIALIZER.get();
    }
}
