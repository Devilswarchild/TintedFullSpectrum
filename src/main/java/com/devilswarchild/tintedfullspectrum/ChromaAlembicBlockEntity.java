package com.devilswarchild.tintedfullspectrum;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;

// The Chroma Alembic's dye-crafting machine: a 2-slot Container (0=Blank Dye input, 1=Colored Dye
// output) that auto-starts a 300-tick craft the instant a Blank Dye is inserted, locking in whatever
// RGB is currently "selected" as the craft's target color. See chroma_alembic_full_build.md.
//
// The platform-spin animation (see ChromaAlembicRenderer) is driven by craftStartGameTime/baseAngle
// rather than a per-tick-synced progress counter: craftStartGameTime is synced once when a craft
// starts, and every client can independently compute a smooth, partial-tick-interpolated elapsed time
// from it using its own (already server-lockstepped) level game time -- no per-tick network traffic
// needed. baseAngle accumulates each completed cycle's final angle so consecutive crafts (and GUI
// arrow resets) never cause the platform to visibly snap back.
public class ChromaAlembicBlockEntity extends BlockEntity implements Container {
    public static final int RAMP_TICKS = 30;
    public static final int SUSTAIN_TICKS = 240;
    public static final int TOTAL_TICKS = RAMP_TICKS + SUSTAIN_TICKS + RAMP_TICKS;
    public static final float FULL_SPEED_DEGREES_PER_TICK = 54f;

    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;

    private final NonNullList<ItemStack> items = NonNullList.withSize(2, ItemStack.EMPTY);

    private boolean processing;
    private long craftStartGameTime;
    private float baseAngle;
    private int targetColor = 0xFFFFFF;
    private int selectedColor = 0xFFFFFF;

    public ChromaAlembicBlockEntity(BlockPos pos, BlockState state) {
        super(TintedFullSpectrum.CHROMA_ALEMBIC_BLOCK_ENTITY.get(), pos, state);
    }

    public boolean isProcessing() {
        return processing;
    }

    public long getCraftStartGameTime() {
        return craftStartGameTime;
    }

    public float getBaseAngle() {
        return baseAngle;
    }

    public int getSelectedColor() {
        return selectedColor;
    }

    public void setSelectedColor(int rgb) {
        this.selectedColor = rgb;
        setChanged();
        syncToClients();
    }

    // Trapezoidal velocity profile: linear ramp up, constant full speed, linear ramp down.
    // elapsedTicks may include a fractional partial-tick component for smooth client-side interpolation.
    public static float computeAngle(float elapsedTicks) {
        float t = Math.max(0f, Math.min(elapsedTicks, TOTAL_TICKS));
        if (t <= RAMP_TICKS) {
            return FULL_SPEED_DEGREES_PER_TICK / (2f * RAMP_TICKS) * t * t;
        }
        float rampUpAngle = FULL_SPEED_DEGREES_PER_TICK * RAMP_TICKS / 2f;
        if (t <= RAMP_TICKS + SUSTAIN_TICKS) {
            return rampUpAngle + FULL_SPEED_DEGREES_PER_TICK * (t - RAMP_TICKS);
        }
        float sustainedAngle = rampUpAngle + FULL_SPEED_DEGREES_PER_TICK * SUSTAIN_TICKS;
        float s = t - RAMP_TICKS - SUSTAIN_TICKS;
        return sustainedAngle + FULL_SPEED_DEGREES_PER_TICK * s - FULL_SPEED_DEGREES_PER_TICK / (2f * RAMP_TICKS) * s * s;
    }

    private boolean canStartCraft() {
        ItemStack input = items.get(INPUT_SLOT);
        if (input.isEmpty() || !input.is(TintedFullSpectrum.BLANK_DYE_ITEM.get())) {
            return false;
        }
        ItemStack output = items.get(OUTPUT_SLOT);
        if (output.isEmpty()) {
            return true;
        }
        if (!output.is(TintedFullSpectrum.COLORED_DYE_ITEM.get()) || output.getCount() >= output.getMaxStackSize()) {
            return false;
        }
        DyedItemColor existing = output.get(DataComponents.DYED_COLOR);
        return existing != null && existing.rgb() == selectedColor;
    }

    private void startCraft() {
        processing = true;
        craftStartGameTime = level.getGameTime();
        targetColor = selectedColor;
        setChanged();
        syncToClients();
    }

    private void completeCraft() {
        items.get(INPUT_SLOT).shrink(1);
        ItemStack output = items.get(OUTPUT_SLOT);
        if (output.isEmpty()) {
            ItemStack newOutput = new ItemStack(TintedFullSpectrum.COLORED_DYE_ITEM.get());
            newOutput.set(DataComponents.DYED_COLOR, new DyedItemColor(targetColor, true));
            items.set(OUTPUT_SLOT, newOutput);
        } else {
            output.grow(1);
        }
        processing = false;
        baseAngle = (baseAngle + computeAngle(TOTAL_TICKS)) % 360f;
        setChanged();
        syncToClients();
    }

    private void syncToClients() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public static BlockEntityTicker<ChromaAlembicBlockEntity> ticker() {
        return (level, pos, state, be) -> {
            if (be.processing) {
                if (level.getGameTime() - be.craftStartGameTime >= TOTAL_TICKS) {
                    be.completeCraft();
                }
            } else if (be.canStartCraft()) {
                be.startCraft();
            }
        };
    }

    // --- Container ---

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        return items.get(INPUT_SLOT).isEmpty() && items.get(OUTPUT_SLOT).isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(items, slot, amount);
        if (!result.isEmpty()) {
            setChanged();
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        stack.limitSize(getMaxStackSize(stack));
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        items.clear();
    }

    // --- Persistence / sync ---

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putBoolean("Processing", processing);
        tag.putLong("CraftStartGameTime", craftStartGameTime);
        tag.putFloat("BaseAngle", baseAngle);
        tag.putInt("TargetColor", targetColor);
        tag.putInt("SelectedColor", selectedColor);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.clear();
        ContainerHelper.loadAllItems(tag, items, registries);
        processing = tag.getBoolean("Processing");
        craftStartGameTime = tag.getLong("CraftStartGameTime");
        baseAngle = tag.getFloat("BaseAngle");
        targetColor = tag.contains("TargetColor") ? tag.getInt("TargetColor") : 0xFFFFFF;
        selectedColor = tag.contains("SelectedColor") ? tag.getInt("SelectedColor") : 0xFFFFFF;
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
}
