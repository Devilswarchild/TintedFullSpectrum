package com.devilswarchild.tintedfullspectrum;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

// Carries the packed RGB color for the vanilla-parity Torch's flame particle (see
// TintedFullSpectrumClient's onRegisterParticleProviders and TintedVanillaTorchBlock's animateTick
// override). Block#animateTick only ever runs client-side, so this never actually crosses the
// network, but ParticleType registration still requires a full codec/stream codec pair, same as
// TintColorComponent's own.
public record TintedFlameParticleOptions(int rgb) implements ParticleOptions {
    public static final MapCodec<TintedFlameParticleOptions> CODEC = Codec.INT.fieldOf("rgb")
            .xmap(TintedFlameParticleOptions::new, TintedFlameParticleOptions::rgb);
    public static final StreamCodec<io.netty.buffer.ByteBuf, TintedFlameParticleOptions> STREAM_CODEC = ByteBufCodecs.INT.map(
            TintedFlameParticleOptions::new, TintedFlameParticleOptions::rgb);

    @Override
    public ParticleType<TintedFlameParticleOptions> getType() {
        return TintedFullSpectrum.TINTED_FLAME_PARTICLE.get();
    }
}
