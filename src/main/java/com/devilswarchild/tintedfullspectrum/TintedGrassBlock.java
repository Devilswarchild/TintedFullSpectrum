package com.devilswarchild.tintedfullspectrum;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

// Vanilla-parity Grass Block: same GrassBlock class as Blocks.GRASS_BLOCK, just tintable.
// Deliberately NOT vanilla-parity on two points, both because this mod has no terrain generation of
// its own and this block exists purely as a hand-placed decorative "build your own biome" material:
// (1) SpreadingSnowyDirtBlock's randomTick (spreading onto nearby dirt, or dying back to plain dirt
// without enough light) is a no-op here; (2) performBonemeal is overridden below to scatter Tinted
// Short Grass instead of vanilla's real Blocks.SHORT_GRASS + biome flower features -- a simpler,
// smaller-radius scatter than vanilla's own algorithm, since there's no tinted flower to spawn yet.
public class TintedGrassBlock extends GrassBlock implements EntityBlock {
    public TintedGrassBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TintedGrassBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        TintableBlocks.applyPlacementColor(level, pos, stack);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
    }

    // Scatters Tinted Short Grass in a small radius above whatever Tinted Grass Block it actually
    // lands on, colored to match THAT block (not necessarily the one bonemealed) -- so bonemealing
    // near a boundary between two differently-dyed patches blends correctly instead of painting
    // everything the bonemealed block's own color.
    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        BlockState shortGrass = TintedFullSpectrum.TINTED_SHORT_GRASS.get().defaultBlockState();
        for (int i = 0; i < 8; i++) {
            BlockPos below = pos.offset(random.nextInt(7) - 3, random.nextInt(3) - 1, random.nextInt(7) - 3);
            BlockPos target = below.above();
            if (!level.getBlockState(below).is(this) || !level.isEmptyBlock(target) || !shortGrass.canSurvive(level, target)) {
                continue;
            }
            level.setBlock(target, shortGrass, 3);
            if (level.getBlockEntity(below) instanceof TintableBlockEntity source
                    && level.getBlockEntity(target) instanceof TintableBlockEntity grown) {
                grown.setColor(source.getColor());
            }
        }
    }
}
