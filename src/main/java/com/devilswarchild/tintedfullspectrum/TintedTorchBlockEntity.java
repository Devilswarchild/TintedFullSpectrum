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
import net.minecraft.world.level.block.state.BlockState;

// Stores the RGB tint applied to a placed torch's embers/flame (tintindex 0).
// Newly placed torches keep DEFAULT_COLOR (untinted white -- looks like a normal torch) until dyed
// with a Colored Dye (see ColoredDyeItem).
public class TintedTorchBlockEntity extends BlockEntity implements TintableBlockEntity {
    public static final int DEFAULT_COLOR = 0xFFFFFF;

    private int color = DEFAULT_COLOR;

    public TintedTorchBlockEntity(BlockPos pos, BlockState state) {
        super(TintedFullSpectrum.TINTED_TORCH_BLOCK_ENTITY.get(), pos, state);
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

    // The color is embers/flame vertex tint baked into the chunk mesh at compile time via
    // BlockColors, not read live like a BlockEntityRenderer would. Since the block's actual
    // BlockState never changes when only the color does, the updated color data arrives here fine
    // (that's what loads it) but nothing tells the renderer to rebake the mesh, so the old tint
    // keeps showing until an unrelated nearby change forces a rebake anyway. Two separate vanilla
    // equality guards stand in the way of the "obvious" fixes:
    //   - Level#markAndNotifyBlock only calls setBlocksDirty when oldState != newState, so going
    //     through the normal setBlock/sendBlockUpdated path never reaches the renderer at all here.
    //   - Even calling Level#setBlocksDirty directly doesn't help: ClientLevel routes it to
    //     LevelRenderer#setBlockDirty, which itself calls ModelManager#requiresRender(old, new) and
    //     that returns false outright whenever old == new (same instance, since our BlockState
    //     never changes) -- so it silently no-ops too.
    // Forcing LevelRenderer#setSectionDirty directly is the one path with no such guard.
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
