package com.devilswarchild.tintedfullspectrum;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

// One block entity type shared by Tinted Planks and its stairs/slab/fence variants -- mirrors how
// TintedTorchBlockEntity is shared by the floor and wall torch blocks.
public class TintedPlanksBlockEntity extends AbstractTintableBlockEntity {
    public TintedPlanksBlockEntity(BlockPos pos, BlockState state) {
        super(TintedFullSpectrum.TINTED_PLANKS_BLOCK_ENTITY.get(), pos, state);
    }
}
