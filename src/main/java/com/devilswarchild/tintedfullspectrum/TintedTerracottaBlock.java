package com.devilswarchild.tintedfullspectrum;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

// Vanilla's own plain Terracotta is a plain Block (no special class, same as Wool) -- Glazed
// Terracotta is a different, explicitly out-of-scope block (GlazedTerracottaBlock, whose per-color
// identity comes from a hand-designed directional mosaic pattern baked per color, structurally
// incompatible with arbitrary RGB -- see tinted_full_spectrum_terracotta_handoff.md).
public class TintedTerracottaBlock extends Block implements EntityBlock {
    public TintedTerracottaBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TintedTerracottaBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        TintableBlocks.applyPlacementColor(level, pos, stack);
    }
}
