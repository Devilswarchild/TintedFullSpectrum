package com.devilswarchild.tintedfullspectrum;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

// Vanilla-parity Tall Grass: same DoublePlantBlock class as Blocks.TALL_GRASS, just tintable. Both
// halves are independently EntityBlock (each stores its own color), so setPlacedBy has to color both
// -- DoublePlantBlock#setPlacedBy auto-places the upper half, same pattern as TintedDoorBlock's own
// two-position setPlacedBy for its bottom/top halves.
public class TintedTallGrassBlock extends DoublePlantBlock implements EntityBlock {
    public TintedTallGrassBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TintedGrassBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        TintableBlocks.applyPlacementColor(level, pos, stack);
        TintableBlocks.applyPlacementColor(level, pos.above(), stack);
    }
}
