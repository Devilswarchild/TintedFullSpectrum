package com.devilswarchild.tintedfullspectrum;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class ChromaStainedGlassBlockEntity extends AbstractTintableBlockEntity {
    public ChromaStainedGlassBlockEntity(BlockPos pos, BlockState state) {
        super(TintedFullSpectrum.CHROMA_STAINED_GLASS_BLOCK_ENTITY.get(), pos, state);
    }
}
