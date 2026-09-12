package com.devilswarchild.tintedfullspectrum;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class ChromaGlassPaneBlockEntity extends AbstractTintableBlockEntity {
    public ChromaGlassPaneBlockEntity(BlockPos pos, BlockState state) {
        super(TintedFullSpectrum.CHROMA_GLASS_PANE_BLOCK_ENTITY.get(), pos, state);
    }
}
