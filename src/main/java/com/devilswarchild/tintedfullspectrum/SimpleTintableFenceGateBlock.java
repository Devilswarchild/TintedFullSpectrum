package com.devilswarchild.tintedfullspectrum;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;

// Generic reusable tintable FenceGateBlock, parameterized by its own BlockEntityType -- same pattern
// as TintedPlanksFenceGateBlock, just parameterized instead of dedicated, for families beyond Planks.
// FenceGateBlock has no constructor that skips WoodType (it only controls the open/close sound pair,
// not material logic), so the caller supplies whichever WoodType actually fits the material -- see
// TintedFullSpectrum.BRICK_WOOD_TYPE for the Bricks family's non-wood choice.
public class SimpleTintableFenceGateBlock extends FenceGateBlock implements EntityBlock {
    private final Supplier<BlockEntityType<SimpleTintableBlockEntity>> blockEntityType;

    public SimpleTintableFenceGateBlock(WoodType woodType, BlockBehaviour.Properties properties, Supplier<BlockEntityType<SimpleTintableBlockEntity>> blockEntityType) {
        super(woodType, properties);
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
