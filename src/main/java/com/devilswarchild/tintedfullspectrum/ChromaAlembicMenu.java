package com.devilswarchild.tintedfullspectrum;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

// Server and client both construct this the same way: given a live ChromaAlembicBlockEntity (the
// server's real one, or the client's locally-synced mirror of it looked up from the block pos sent
// in the menu-open packet). No ContainerData is needed -- progress/selected-color/processing all
// ride the block entity's normal sync (see ChromaAlembicBlockEntity), which reaches every nearby
// client, not just whoever has the screen open, so the world-visible spin animation stays correct
// even while this menu is closed.
public class ChromaAlembicMenu extends AbstractContainerMenu {
    private static final int INV_ROW_COUNT = 3;
    private static final int INV_COL_COUNT = 9;
    private static final int PLAYER_INV_X = 8;
    private static final int PLAYER_INV_Y = 140;
    private static final int HOTBAR_Y = 198;

    private final ChromaAlembicBlockEntity blockEntity;

    public ChromaAlembicMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(containerId, playerInventory, resolveBlockEntity(playerInventory, extraData.readBlockPos()));
    }

    public ChromaAlembicMenu(int containerId, Inventory playerInventory, ChromaAlembicBlockEntity blockEntity) {
        super(TintedFullSpectrum.CHROMA_ALEMBIC_MENU.get(), containerId);
        this.blockEntity = blockEntity;

        addSlot(new Slot(blockEntity, ChromaAlembicBlockEntity.INPUT_SLOT, 55, 97) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(TintedFullSpectrum.BLANK_DYE_ITEM.get());
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        addSlot(new Slot(blockEntity, ChromaAlembicBlockEntity.OUTPUT_SLOT, 102, 97) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        for (int row = 0; row < INV_ROW_COUNT; row++) {
            for (int col = 0; col < INV_COL_COUNT; col++) {
                addSlot(new Slot(playerInventory, col + row * INV_COL_COUNT + INV_COL_COUNT, PLAYER_INV_X + col * 18, PLAYER_INV_Y + row * 18));
            }
        }
        for (int col = 0; col < INV_COL_COUNT; col++) {
            addSlot(new Slot(playerInventory, col, PLAYER_INV_X + col * 18, HOTBAR_Y));
        }
    }

    private static ChromaAlembicBlockEntity resolveBlockEntity(Inventory playerInventory, net.minecraft.core.BlockPos pos) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        if (be instanceof ChromaAlembicBlockEntity alembic) {
            return alembic;
        }
        throw new IllegalStateException("No ChromaAlembicBlockEntity at " + pos);
    }

    public ChromaAlembicBlockEntity getBlockEntity() {
        return blockEntity;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            result = stackInSlot.copy();
            if (index < 2) {
                if (!moveItemStackTo(stackInSlot, 2, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (stackInSlot.is(TintedFullSpectrum.BLANK_DYE_ITEM.get())) {
                if (!moveItemStackTo(stackInSlot, ChromaAlembicBlockEntity.INPUT_SLOT, ChromaAlembicBlockEntity.INPUT_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                return ItemStack.EMPTY;
            }

            if (stackInSlot.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity.stillValid(player);
    }
}
