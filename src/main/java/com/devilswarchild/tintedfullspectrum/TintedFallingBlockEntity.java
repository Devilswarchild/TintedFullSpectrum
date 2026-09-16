package com.devilswarchild.tintedfullspectrum;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

// Vanilla's real FallingBlockEntity has no way to carry an arbitrary tint color to the CLIENT while
// airborne -- its `blockState` field is private with no public setter (only reachable via a private
// constructor, or the NBT/network round-trips), and there is no synced color field at all, since
// vanilla's own falling blocks (sand, gravel, colorless concrete powder) never needed one. This
// subclass adds one: a synced `DATA_COLOR` field, automatically replicated to every client the same
// way vanilla's own entity data already is, no custom packet needed. See TintedFallingBlockRenderer
// for the other half (actually painting this color onto the falling block's rendered mesh) and
// TintedConcretePowderBlock#tick(), the only place this gets created.
public class TintedFallingBlockEntity extends FallingBlockEntity {
    private static final EntityDataAccessor<Integer> DATA_COLOR =
            SynchedEntityData.defineId(TintedFallingBlockEntity.class, EntityDataSerializers.INT);

    public TintedFallingBlockEntity(EntityType<? extends TintedFallingBlockEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_COLOR, 0xFFFFFF);
    }

    public int getColor() {
        return this.entityData.get(DATA_COLOR);
    }

    public void setColor(int rgb) {
        this.entityData.set(DATA_COLOR, rgb);
    }

    // Mirrors vanilla's own FallingBlockEntity.fall(Level, BlockPos, BlockState) exactly (same
    // waterlogged-stripping, same block-at-origin-replaced-with-air/fluid behavior), just producing
    // this mod's own entity type/class instead of vanilla's hardcoded EntityType.FALLING_BLOCK, and
    // additionally stamping the tint color on before the entity is ever added to the level (so it's
    // present in the very first sync packet, not a later update). `blockState` has no public setter,
    // so it's set through the class's own official NBT deserialization path (readAdditionalSaveData)
    // rather than reflection.
    public static TintedFallingBlockEntity fall(Level level, BlockPos pos, BlockState state, int color) {
        BlockState fallingState = state.hasProperty(BlockStateProperties.WATERLOGGED)
                ? state.setValue(BlockStateProperties.WATERLOGGED, Boolean.FALSE)
                : state;
        TintedFallingBlockEntity entity = new TintedFallingBlockEntity(TintedFullSpectrum.TINTED_FALLING_BLOCK.get(), level);
        entity.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        entity.setDeltaMovement(Vec3.ZERO);
        entity.xo = entity.getX();
        entity.yo = entity.getY();
        entity.zo = entity.getZ();
        entity.setStartPos(pos);
        CompoundTag stateTag = new CompoundTag();
        stateTag.put("BlockState", NbtUtils.writeBlockState(fallingState));
        entity.readAdditionalSaveData(stateTag);
        entity.blocksBuilding = true;
        entity.setColor(color);
        level.setBlock(pos, state.getFluidState().createLegacyBlock(), 3);
        level.addFreshEntity(entity);
        return entity;
    }
}
