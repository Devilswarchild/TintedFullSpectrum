package com.devilswarchild.tintedfullspectrum;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

// Generic reusable tintable FenceBlock, parameterized by its own BlockEntityType -- same pattern as
// TintedPlanksFenceBlock, just parameterized instead of dedicated, for families beyond Planks (Bricks).
public class SimpleTintableFenceBlock extends FenceBlock implements EntityBlock {
    private final Supplier<BlockEntityType<SimpleTintableBlockEntity>> blockEntityType;

    public SimpleTintableFenceBlock(BlockBehaviour.Properties properties, Supplier<BlockEntityType<SimpleTintableBlockEntity>> blockEntityType) {
        super(properties);
        this.blockEntityType = blockEntityType;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SimpleTintableBlockEntity(blockEntityType.get(), pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        TintableBlocks.applyPlacementColor(level, pos, stack);
    }
}
