package com.devilswarchild.tintedfullspectrum;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

// Stage 0 of the Bricks chain -- dyes the raw material, batch of 4 (chosen so the rest of the chain,
// 4 Tinted Clay Ball -> 4 Tinted Brick -> 1 Tinted Bricks, comes out even with nothing left over).
// Vanilla never dyes Clay Ball at all, so only the mod's own Colored Dye is accepted here (no real
// vanilla dye recipe to mirror, same bucket Sandstone's base recipe sat in) -- see
// tinted_full_spectrum_bricks_handoff.md.
//
// Two independent input shapes produce the identical output, for world-gen-independent access:
// - Standard: 1 Clay Ball + 1 Colored Dye.
// - Fallback: 1 Mud + 1 Gravel + 1 Colored Dye (built on vanilla's real Dirt->Mud step, with Gravel
//   chosen for maximum world-gen availability). Distinct from the separate, dye-less "1 Mud + 1
//   Gravel -> 4 minecraft:clay_ball" bonus recipe, which is an ordinary static ShapelessRecipe (see
//   data/tinted_full_spectrum/recipe/mud_gravel_to_clay_ball.json), not this class.
public class TintedClayBallRecipe extends CustomRecipe {
    public TintedClayBallRecipe(CraftingBookCategory category) {
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
        ItemStack result = new ItemStack(TintedFullSpectrum.TINTED_CLAY_BALL_ITEM.get(), 4);
        result.set(TintedFullSpectrum.TINT_COLOR.get(), color);
        return result;
    }

    // Returns the dye's color if the grid holds exactly {1 Clay Ball, 1 Colored Dye} or exactly
    // {1 Mud, 1 Gravel, 1 Colored Dye}, and nothing else.
    private static TintColorComponent findColor(CraftingInput input) {
        TintColorComponent color = null;
        int clayBallCount = 0;
        int mudCount = 0;
        int gravelCount = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.getItem() instanceof ColoredDyeItem) {
                if (color != null) {
                    return null;
                }
                color = stack.get(TintedFullSpectrum.TINT_COLOR.get());
                if (color == null) {
                    return null;
                }
            } else if (stack.getItem() == Items.CLAY_BALL) {
                clayBallCount++;
            } else if (stack.getItem() == Items.MUD) {
                mudCount++;
            } else if (stack.getItem() == Items.GRAVEL) {
                gravelCount++;
            } else {
                return null;
            }
        }
        if (color == null) {
            return null;
        }
        boolean standard = clayBallCount == 1 && mudCount == 0 && gravelCount == 0;
        boolean fallback = clayBallCount == 0 && mudCount == 1 && gravelCount == 1;
        return standard || fallback ? color : null;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return TintedFullSpectrum.TINTED_CLAY_BALL_SERIALIZER.get();
    }
}
