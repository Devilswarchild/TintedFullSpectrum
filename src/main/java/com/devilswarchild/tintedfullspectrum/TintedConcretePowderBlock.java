package com.devilswarchild.tintedfullspectrum;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

// Full vanilla parity for Tinted Concrete Powder -- gravity-affected, hardens into solid Tinted
// Concrete on water contact, mirroring ConcretePowderBlock/FallingBlock exactly (logic copied from
// the real decompiled sources, not guessed). The one genuinely new problem vanilla never had to solve
// is carrying the tint color across every transition a plain color-less powder never needed to
// survive:
//
// 1. Stationary powder touched by water (never falls) -- handled by overriding onRemove to keep the
//    SAME TintedConcreteBlockEntity instance alive across the powder->concrete state swap instead of
//    letting vanilla's default onRemove destroy it (default behavior destroys the block entity any
//    time the block TYPE changes, powder vs solid are different Block instances). Both blocks share
//    one BlockEntityType (TINTED_CONCRETE_BLOCK_ENTITY) specifically so this is valid.
// 2. Powder starts falling -- the ORIGINAL position becomes air/fluid, which has no block entity, so
//    there is nothing to preserve in place; the color has to travel WITH the falling entity itself.
//    TintedFallingBlockEntity (a custom subclass, see that class) carries it as a synced field, so it
//    even renders correctly WHILE airborne (TintedFallingBlockRenderer). Landing itself is handled
//    entirely by vanilla's own inherited FallingBlockEntity#tick() -- once it places the actual block
//    (and calls this class's onLand below), it also tries to restore whatever NBT the falling entity
//    carried in its `blockData` field, but does so via a raw loadWithComponents call that bypasses
//    this mod's own AbstractTintableBlockEntity#setColor(int) entirely -- and setColor is where the
//    forced resync + chunk-rebake trick lives (see blockcolor_rebake_gotcha.md: BlockEntity-only
//    changes silently skip a rebake unless forced). Relying on vanilla's raw restore left a visible
//    split-second white flash right as the block landed, on both the falling-and-landing path AND (an
//    earlier attempt at fixing just the block-entity-creation timing) still flashed, since the
//    CLIENT's own local block entity is built independently when it processes the network packet, not
//    shared server-side JVM state. Fixed properly below: onLand explicitly calls setColor() itself,
//    using the falling entity's own carried color -- the exact same, already-proven, flicker-free
//    mechanism every other tintable block in this mod already uses for normal placement.
public class TintedConcretePowderBlock extends FallingBlock implements EntityBlock {
    public static final MapCodec<TintedConcretePowderBlock> CODEC = simpleCodec(TintedConcretePowderBlock::new);

    @Override
    public MapCodec<? extends FallingBlock> codec() {
        return CODEC;
    }

    public TintedConcretePowderBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TintedConcreteBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        TintableBlocks.applyPlacementColor(level, pos, stack);
    }

    // Same condition as BlockBehaviour's own default onRemove, except the one exception this mod
    // needs: don't tear down the block entity when hardening into our own solid Tinted Concrete,
    // since that's a deliberate in-place conversion, not a genuine block removal.
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (state.hasBlockEntity() && !state.is(newState.getBlock()) && newState.getBlock() != TintedFullSpectrum.TINTED_CONCRETE.get()) {
            level.removeBlockEntity(pos);
        }
    }

    @Override
    public void onLand(Level level, BlockPos pos, BlockState state, BlockState replaceableState, FallingBlockEntity fallingBlock) {
        if (shouldSolidify(level, pos, replaceableState.getFluidState())) {
            level.setBlock(pos, TintedFullSpectrum.TINTED_CONCRETE.get().defaultBlockState(), 3);
        }
        // The one place that actually applies the falling entity's carried color to the landed block
        // -- via setColor(), not vanilla's own blockData/loadWithComponents restore, since only
        // setColor() forces the resync + chunk rebake that makes this appear correctly. Runs after the
        // possible solidify swap above so there's only one resync, on the final state.
        //
        // setColor()'s own resync alone still isn't enough to fully close the visible gap, though --
        // it queues the block entity's data through the SAME lazy, batched per-tick sync every normal
        // block entity uses. Vanilla's own landing code (FallingBlockEntity#tick()) explicitly
        // broadcasts the block STATE packet immediately, bypassing that batching entirely
        // (chunkMap.broadcast(this, new ClientboundBlockUpdatePacket(...)) right before discard()) --
        // but has no equivalent immediate path for block ENTITY data, since vanilla never needed one.
        // That asymmetry (state arrives immediately, color arrives a batch cycle later) is what shows
        // up as a brief white flash. Closed by mirroring vanilla's own immediate-broadcast trick for
        // the block entity's update packet too, sent directly to every player currently watching this
        // chunk, instead of waiting on the normal lazy sync.
        if (fallingBlock instanceof TintedFallingBlockEntity tinted
                && level.getBlockEntity(pos) instanceof TintableBlockEntity tintable) {
            tintable.setColor(tinted.getColor());
            if (level instanceof ServerLevel serverLevel && level.getBlockEntity(pos) instanceof BlockEntity blockEntity) {
                net.minecraft.network.protocol.Packet<?> packet = blockEntity.getUpdatePacket();
                if (packet != null) {
                    for (net.minecraft.server.level.ServerPlayer player
                            : serverLevel.getChunkSource().chunkMap.getPlayers(new net.minecraft.world.level.ChunkPos(pos), false)) {
                        player.connection.send(packet);
                    }
                }
            }
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockGetter blockgetter = context.getLevel();
        BlockPos blockpos = context.getClickedPos();
        BlockState blockstate = blockgetter.getBlockState(blockpos);
        return shouldSolidify(blockgetter, blockpos, blockstate.getFluidState()) || touchesLiquid(blockgetter, blockpos)
                ? TintedFullSpectrum.TINTED_CONCRETE.get().defaultBlockState()
                : super.getStateForPlacement(context);
    }

    private static boolean shouldSolidify(BlockGetter level, BlockPos pos, FluidState fluidState) {
        return fluidState.is(FluidTags.WATER);
    }

    private static boolean touchesLiquid(BlockGetter level, BlockPos pos) {
        BlockPos.MutableBlockPos mutablePos = pos.mutable();
        for (Direction direction : Direction.values()) {
            mutablePos.setWithOffset(pos, direction);
            if (level.getFluidState(mutablePos).is(FluidTags.WATER)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor level, BlockPos currentPos, BlockPos facingPos) {
        return touchesLiquid(level, currentPos)
                ? TintedFullSpectrum.TINTED_CONCRETE.get().defaultBlockState()
                : super.updateShape(state, facing, facingState, level, currentPos, facingPos);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (isFree(level.getBlockState(pos.below())) && pos.getY() >= level.getMinBuildHeight()) {
            BlockEntity existing = level.getBlockEntity(pos);
            int color = existing instanceof TintableBlockEntity tintable ? tintable.getColor() : TintColorComponent.DEFAULT_COLOR;
            TintedFallingBlockEntity fallingBlockEntity = TintedFallingBlockEntity.fall(level, pos, state, color);
            this.falling(fallingBlockEntity);
        }
    }

    @Override
    public int getDustColor(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getMapColor(level, pos).col;
    }
}
