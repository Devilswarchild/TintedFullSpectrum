package com.devilswarchild.tintedfullspectrum;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class TintedFloorTorchBlock extends TorchBlock implements EntityBlock {
    public TintedFloorTorchBlock(SimpleParticleType flameParticle, BlockBehaviour.Properties properties) {
        super(flameParticle, properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TintedTorchBlockEntity(pos, state);
    }

    // The custom flame/embers model already animates via its texture strip; skip vanilla's smoke+flame particles.
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
    }
}
