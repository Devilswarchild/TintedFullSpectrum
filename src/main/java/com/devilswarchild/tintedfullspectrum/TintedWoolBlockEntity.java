package com.devilswarchild.tintedfullspectrum;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

// Shared by TintedWoolBlock and TintedCarpetBlock -- both just store a color.
public class TintedWoolBlockEntity extends AbstractTintableBlockEntity {
    public TintedWoolBlockEntity(BlockPos pos, BlockState state) {
        super(TintedFullSpectrum.TINTED_WOOL_BLOCK_ENTITY.get(), pos, state);
    }
}
