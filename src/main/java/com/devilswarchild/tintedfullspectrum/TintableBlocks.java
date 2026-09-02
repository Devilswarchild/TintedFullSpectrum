package com.devilswarchild.tintedfullspectrum;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

// Shared placement logic for every tintable block: on placement, read the placing item's stored
// TintColorComponent (or the default if it was never dyed) and write it into the freshly-created
// block entity. Called from each tintable Block's Block#setPlacedBy override.
public final class TintableBlocks {
    private TintableBlocks() {
    }

    public static void applyPlacementColor(Level level, BlockPos pos, ItemStack stack) {
        if (level.getBlockEntity(pos) instanceof TintableBlockEntity tintable) {
            tintable.setColor(TintColorComponent.getOrDefault(stack, TintColorComponent.DEFAULT_COLOR));
        }
    }
}
