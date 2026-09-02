package com.devilswarchild.tintedfullspectrum;

import com.mojang.serialization.Codec;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

// A single packed RGB int (no alpha) attached to any item this mod considers "tintable" -- Colored
// Dye (the value a player mixed), and any TintableItem (tinted torch, tinted planks and its shape
// variants, future materials). Generic and reusable on purpose: one component type, not one per
// material, so adding a new tintable item is just "register it as a TintableBlockItem/etc" -- see
// tintable_system_and_planks.md.
public record TintColorComponent(int rgb) {
    public static final int DEFAULT_COLOR = 0xFFFFFF;

    public static final Codec<TintColorComponent> CODEC = Codec.INT.xmap(TintColorComponent::new, TintColorComponent::rgb);
    public static final StreamCodec<ByteBuf, TintColorComponent> STREAM_CODEC = ByteBufCodecs.INT.map(TintColorComponent::new, TintColorComponent::rgb);

    public static int getOrDefault(ItemStack stack, int defaultValue) {
        TintColorComponent component = stack.get(TintedFullSpectrum.TINT_COLOR.get());
        return component != null ? component.rgb() : defaultValue;
    }
}
