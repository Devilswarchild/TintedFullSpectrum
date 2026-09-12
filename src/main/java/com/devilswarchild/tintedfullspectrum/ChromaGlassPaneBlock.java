package com.devilswarchild.tintedfullspectrum;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

// Extends IronBarsBlock directly (the plain Blocks.GLASS_PANE class), not vanilla's
// StainedGlassPaneBlock subclass -- that one exists to let the pane tint a beacon beam
// (BeaconBeamBlock), tied to the fixed 16-color DyeColor enum, which has no clean equivalent for an
// arbitrary-RGB pane. Same scoped-simplification category as skipping copper doors and llama-wearable
// carpet: this pane just won't color a beacon beam. See tinted_full_spectrum_glass_handoff.md.
public class ChromaGlassPaneBlock extends IronBarsBlock implements EntityBlock {
    public ChromaGlassPaneBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ChromaGlassPaneBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        TintableBlocks.applyPlacementColor(level, pos, stack);
    }
}
