package com.devilswarchild.tintedfullspectrum;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class TintedTerracottaBlockEntity extends AbstractTintableBlockEntity {
    public TintedTerracottaBlockEntity(BlockPos pos, BlockState state) {
        super(TintedFullSpectrum.TINTED_TERRACOTTA_BLOCK_ENTITY.get(), pos, state);
    }
}
