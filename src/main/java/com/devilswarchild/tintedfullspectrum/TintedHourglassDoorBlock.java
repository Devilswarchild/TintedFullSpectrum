package com.devilswarchild.tintedfullspectrum;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;

// DoorBlock's own setPlacedBy places the upper half directly (level.setBlock, not another
// setPlacedBy call), so both halves need coloring explicitly here rather than relying on the usual
// single-position TintableBlocks.applyPlacementColor call other tintable blocks use.
public class TintedHourglassDoorBlock extends DoorBlock implements EntityBlock {
    public TintedHourglassDoorBlock(BlockBehaviour.Properties properties) {
        super(BlockSetType.OAK, properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TintedDoorBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        TintableBlocks.applyPlacementColor(level, pos, stack);
        TintableBlocks.applyPlacementColor(level, pos.above(), stack);
    }
}
