package com.devilswarchild.tintedfullspectrum;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

// Vanilla-parity counterpart to TintedFloorTorchBlock: same vanilla TorchBlock shape/behavior, just
// with tintable geometry instead of the original's fully custom bracket model. animateTick IS
// overridden here (unlike TintedFloorTorchBlock) -- vanilla's own flame particle has a fixed color,
// which would look wrong floating above a differently-colored flame, so this spawns
// TintedFlameParticleOptions instead, carrying the block entity's own stored color.
public class TintedVanillaTorchBlock extends TorchBlock implements EntityBlock {
    public TintedVanillaTorchBlock(SimpleParticleType flameParticle, BlockBehaviour.Properties properties) {
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
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.7;
        double z = pos.getZ() + 0.5;
        level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0, 0.0, 0.0);
        int color = level.getBlockEntity(pos) instanceof TintableBlockEntity tintable ? tintable.getColor() : TintColorComponent.DEFAULT_COLOR;
        level.addParticle(new TintedFlameParticleOptions(color), x, y, z, 0.0, 0.0, 0.0);
    }
}
