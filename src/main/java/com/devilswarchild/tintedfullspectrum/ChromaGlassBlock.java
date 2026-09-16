package com.devilswarchild.tintedfullspectrum;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

// Extends TransparentBlock directly (the plain Blocks.GLASS class), not vanilla's StainedGlassBlock
// subclass -- that one exists only to let the block tint a beacon beam (BeaconBeamBlock), tied to the
// fixed 16-color DyeColor enum, same scoped-simplification category as ChromaGlassPaneBlock skipping
// StainedGlassPaneBlock (and copper doors, llama-wearable carpet before it): this block just won't
// color a beacon beam.
public class ChromaGlassBlock extends TransparentBlock implements EntityBlock {
    public ChromaGlassBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ChromaGlassBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        TintableBlocks.applyPlacementColor(level, pos, stack);
    }
}
