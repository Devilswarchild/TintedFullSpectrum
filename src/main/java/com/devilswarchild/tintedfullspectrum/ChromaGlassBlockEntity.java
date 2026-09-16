package com.devilswarchild.tintedfullspectrum;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class ChromaGlassBlockEntity extends AbstractTintableBlockEntity {
    public ChromaGlassBlockEntity(BlockPos pos, BlockState state) {
        super(TintedFullSpectrum.CHROMA_GLASS_BLOCK_ENTITY.get(), pos, state);
    }
}
