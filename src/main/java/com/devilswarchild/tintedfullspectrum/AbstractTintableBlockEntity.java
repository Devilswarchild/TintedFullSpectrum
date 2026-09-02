package com.devilswarchild.tintedfullspectrum;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

// Shared base for every tintable block's BlockEntity (currently the tinted torch and Tinted Planks
// + its stairs/slab/fence variants): stores one packed RGB int, NBT-persists it, syncs it to
// clients, and forces the chunk mesh to actually rebake when it changes -- see
// blockcolor_rebake_gotcha.md for why the rebake needs forcing at all (two separate vanilla
// equality guards silently skip it otherwise, since BlockColors tinting is baked into the mesh at
// compile time and our BlockState never changes when only the color does).
public abstract class AbstractTintableBlockEntity extends BlockEntity implements TintableBlockEntity {
    private int color = TintColorComponent.DEFAULT_COLOR;

    protected AbstractTintableBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public int getColor() {
        return color;
    }

    @Override
    public void setColor(int color) {
        this.color = color;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Color", color);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Color")) {
            color = tag.getInt("Color");
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
        int oldColor = this.color;
        super.onDataPacket(connection, packet, registries);
        if (level != null && level.isClientSide && this.color != oldColor) {
            net.minecraft.client.Minecraft.getInstance().levelRenderer.setSectionDirty(
                    SectionPos.blockToSectionCoord(worldPosition.getX()),
                    SectionPos.blockToSectionCoord(worldPosition.getY()),
                    SectionPos.blockToSectionCoord(worldPosition.getZ()));
        }
    }
}
