package com.devilswarchild.tintedfullspectrum;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

// One block entity type shared by both door families (Hourglass and the vanilla-parity door) --
// mirrors how TintedPlanksBlockEntity is shared across the plank shapes. Each half of a placed door
// (upper and lower) gets its own instance so either half can be broken/queried independently, kept
// in sync by each door Block's setPlacedBy coloring both positions.
public class TintedDoorBlockEntity extends AbstractTintableBlockEntity {
    public TintedDoorBlockEntity(BlockPos pos, BlockState state) {
        super(TintedFullSpectrum.TINTED_DOOR_BLOCK_ENTITY.get(), pos, state);
    }
}
