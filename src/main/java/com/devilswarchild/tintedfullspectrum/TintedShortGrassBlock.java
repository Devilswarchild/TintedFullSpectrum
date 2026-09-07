package com.devilswarchild.tintedfullspectrum;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.TallGrassBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

// Vanilla-parity Short Grass: same TallGrassBlock class as Blocks.SHORT_GRASS (vanilla's name for
// this class is misleading -- it's the single-height decorative grass, not the 2-tall one), just
// tintable. Bonemeal still grows vanilla's own untinted Blocks.TALL_GRASS, same "spreads as plain
// vanilla" limitation as TintedGrassBlock -- not worth overriding for a cosmetic edge case.
public class TintedShortGrassBlock extends TallGrassBlock implements EntityBlock {
    public TintedShortGrassBlock(BlockBehaviour.Properties properties) {
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
}
