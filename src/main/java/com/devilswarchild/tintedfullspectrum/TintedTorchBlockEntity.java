package com.devilswarchild.tintedfullspectrum;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

// Stores the RGB tint applied to a placed torch's embers/flame (tintindex 0). Populated from the
// placing item's TintColorComponent (see TintableBlocks#applyPlacementColor, called from
// TintedFloorTorchBlock/TintedWallTorchBlock#setPlacedBy); defaults to untinted white otherwise.
public class TintedTorchBlockEntity extends AbstractTintableBlockEntity {
    public TintedTorchBlockEntity(BlockPos pos, BlockState state) {
        super(TintedFullSpectrum.TINTED_TORCH_BLOCK_ENTITY.get(), pos, state);
    }
}
