package com.devilswarchild.tintedfullspectrum;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class TintedConcreteBlockEntity extends AbstractTintableBlockEntity {
    public TintedConcreteBlockEntity(BlockPos pos, BlockState state) {
        super(TintedFullSpectrum.TINTED_CONCRETE_BLOCK_ENTITY.get(), pos, state);
    }
}
