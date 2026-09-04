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

// Vanilla's own door silhouette, just recolorable -- see TintedHourglassDoorBlock for why both
// halves need explicit coloring here.
public class TintedDoorBlock extends DoorBlock implements EntityBlock {
    public TintedDoorBlock(BlockBehaviour.Properties properties) {
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
