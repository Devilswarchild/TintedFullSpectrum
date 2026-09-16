package com.devilswarchild.tintedfullspectrum;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

// Gravity-affected, vanilla-parity Tinted Sand -- one class shared by both Tinted Sand and Tinted Red
// Sand (mirroring vanilla's own real Sand/Red Sand split, same one-class-two-registrations pattern as
// TintedConcretePowderBlock, just without the water-hardening half of that block's job: plain sand
// never converts into anything on landing, it just... lands. Reuses the exact same falling-block
// color-carry infrastructure built for Concrete Powder (TintedFallingBlockEntity/
// TintedFallingBlockRenderer, both already fully generic, not Concrete-Powder-specific) and the
// generic SimpleTintableBlockEntity from the Sandstone family -- no new block-entity/entity/renderer
// classes needed for this one at all. vanilla's real Sand/Red Sand use a ColoredFallingBlock class
// (fixed per-block dust-particle color via a baked ColorRGBA), which doesn't fit an arbitrary-RGB
// tint, so this mirrors TintedConcretePowderBlock's own approach instead: plain FallingBlock plus a
// getDustColor() override reading the block's actual map color.
public class TintedSandBlock extends FallingBlock implements EntityBlock {
    public static final MapCodec<TintedSandBlock> CODEC = simpleCodec(TintedSandBlock::new);

    @Override
    public MapCodec<? extends FallingBlock> codec() {
        return CODEC;
    }

    public TintedSandBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SimpleTintableBlockEntity(TintedFullSpectrum.TINTED_SAND_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        TintableBlocks.applyPlacementColor(level, pos, stack);
    }

    // Same color-application + immediate-broadcast fix as TintedConcretePowderBlock#onLand -- see that
    // class's own doc comment for the full mechanism (vanilla's landing code force-broadcasts the
    // block STATE packet immediately but not block ENTITY data, so without this the block would land
    // correctly-shaped but visibly flash white for a moment before the color catches up). No
    // solidify/water logic at all here -- sand just lands as itself.
    @Override
    public void onLand(Level level, BlockPos pos, BlockState state, BlockState replaceableState, FallingBlockEntity fallingBlock) {
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
