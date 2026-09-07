package com.devilswarchild.tintedfullspectrum;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

// Shared by TintedGrassBlock, TintedShortGrassBlock, and TintedTallGrassBlock -- all three just
// store a color, same as every other tintable block entity in this mod.
public class TintedGrassBlockEntity extends AbstractTintableBlockEntity {
    public TintedGrassBlockEntity(BlockPos pos, BlockState state) {
        super(TintedFullSpectrum.TINTED_GRASS_BLOCK_ENTITY.get(), pos, state);
    }
}
