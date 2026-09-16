package com.devilswarchild.tintedfullspectrum;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

// Generic reusable plain Block for any tintable family with no special behavior beyond tint storage
// (no falling, no connections, no shape-derived state) -- same pattern as TintedTerracottaBlock/
// TintedWoolBlock/TintedConcreteBlock, just parameterized by its own BlockEntityType instead of one
// dedicated class per block. Introduced for the Sandstone family (8 near-identical shapes across two
// parallel materials) rather than writing 8 near-byte-identical dedicated classes; safe to reuse for
// any future plain-cube tintable family too.
public class SimpleTintableBlock extends Block implements EntityBlock {
    private final Supplier<BlockEntityType<SimpleTintableBlockEntity>> blockEntityType;

    public SimpleTintableBlock(BlockBehaviour.Properties properties, Supplier<BlockEntityType<SimpleTintableBlockEntity>> blockEntityType) {
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
