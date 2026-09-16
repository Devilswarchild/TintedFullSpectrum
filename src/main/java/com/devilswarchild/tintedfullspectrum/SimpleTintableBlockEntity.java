package com.devilswarchild.tintedfullspectrum;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

// Generic reusable block entity for SimpleTintableBlock -- see that class's doc for why this is
// parameterized rather than one dedicated subclass per block.
public class SimpleTintableBlockEntity extends AbstractTintableBlockEntity {
    public SimpleTintableBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
}
