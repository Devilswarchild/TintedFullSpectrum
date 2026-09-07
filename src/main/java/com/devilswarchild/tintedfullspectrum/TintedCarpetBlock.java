package com.devilswarchild.tintedfullspectrum;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

// Extends CarpetBlock directly, not vanilla's WoolCarpetBlock -- that subclass exists purely to let
// llamas equip one of the 16 fixed DyeColor carpets as decor (Equipable/getEquipmentSlot), which has
// no clean equivalent for an arbitrary-RGB-tinted carpet, so it's left out (llamas just won't
// recognize this as wearable, same kind of scoped simplification as skipping copper doors).
public class TintedCarpetBlock extends CarpetBlock implements EntityBlock {
    public TintedCarpetBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TintedWoolBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        TintableBlocks.applyPlacementColor(level, pos, stack);
    }
}
