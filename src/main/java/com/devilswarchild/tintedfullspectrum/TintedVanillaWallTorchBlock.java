package com.devilswarchild.tintedfullspectrum;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

// Wall counterpart to TintedVanillaTorchBlock -- see that class for context on the tinted-particle
// animateTick override (position/offset math copied from vanilla's own WallTorchBlock).
public class TintedVanillaWallTorchBlock extends WallTorchBlock implements EntityBlock {
    public TintedVanillaWallTorchBlock(SimpleParticleType flameParticle, BlockBehaviour.Properties properties) {
        super(flameParticle, properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TintedTorchBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        TintableBlocks.applyPlacementColor(level, pos, stack);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        Direction facing = state.getValue(FACING);
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.7;
        double z = pos.getZ() + 0.5;
        Direction away = facing.getOpposite();
        double px = x + 0.27 * away.getStepX();
        double py = y + 0.22;
        double pz = z + 0.27 * away.getStepZ();
        level.addParticle(ParticleTypes.SMOKE, px, py, pz, 0.0, 0.0, 0.0);
        int color = level.getBlockEntity(pos) instanceof TintableBlockEntity tintable ? tintable.getColor() : TintColorComponent.DEFAULT_COLOR;
        level.addParticle(new TintedFlameParticleOptions(color), px, py, pz, 0.0, 0.0, 0.0);
    }
}
