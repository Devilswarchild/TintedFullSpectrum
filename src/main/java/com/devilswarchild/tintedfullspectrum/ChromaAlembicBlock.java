package com.devilswarchild.tintedfullspectrum;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

// The Chroma Alembic (see chroma_alembic_full_build.md). Faces the player at placement like a
// furnace, then stays fixed. Right-click opens the dye-crafting GUI.
public class ChromaAlembicBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<ChromaAlembicBlock> CODEC = simpleCodec(ChromaAlembicBlock::new);

    public ChromaAlembicBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<ChromaAlembicBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ChromaAlembicBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return level.isClientSide ? null : createTickerHelper(blockEntityType, TintedFullSpectrum.CHROMA_ALEMBIC_BLOCK_ENTITY.get(), ChromaAlembicBlockEntity.ticker());
    }

    @SuppressWarnings("unchecked")
    private static <T extends BlockEntity, E extends BlockEntity> BlockEntityTicker<T> createTickerHelper(
            BlockEntityType<T> type, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker) {
        return type == expected ? (BlockEntityTicker<T>) ticker : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof ChromaAlembicBlockEntity alembic) {
            player.openMenu(new SimpleMenuProvider(
                    (windowId, inv, p) -> new ChromaAlembicMenu(windowId, inv, alembic),
                    Component.translatable("container.tinted_full_spectrum.chroma_alembic")), pos);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof ChromaAlembicBlockEntity alembic) {
                Containers.dropContents(level, pos, alembic);
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }
}
