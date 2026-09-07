package com.devilswarchild.tintedfullspectrum;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.RisingParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

// Identical behavior to vanilla's own FlameParticle (which TintedVanillaTorchBlock/WallTorchBlock
// would otherwise spawn via ParticleTypes.FLAME), just multiplied by the torch's own stored color --
// reuses vanilla's own flame sprite (see particles/tinted_flame.json), no new particle texture needed.
@OnlyIn(Dist.CLIENT)
public class TintedFlameParticle extends RisingParticle {
    protected TintedFlameParticle(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, int rgb) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.rCol = ((rgb >> 16) & 0xFF) / 255.0F;
        this.gCol = ((rgb >> 8) & 0xFF) / 255.0F;
        this.bCol = (rgb & 0xFF) / 255.0F;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    @Override
    public void move(double x, double y, double z) {
        this.setBoundingBox(this.getBoundingBox().move(x, y, z));
        this.setLocationFromBoundingbox();
    }

    @Override
    public float getQuadSize(float scaleFactor) {
        float f = ((float) this.age + scaleFactor) / (float) this.lifetime;
        return this.quadSize * (1.0F - f * f * 0.5F);
    }

    @Override
    public int getLightColor(float partialTick) {
        float f = ((float) this.age + partialTick) / (float) this.lifetime;
        f = Mth.clamp(f, 0.0F, 1.0F);
        int i = super.getLightColor(partialTick);
        int j = i & 0xFF;
        int k = i >> 16 & 0xFF;
        j += (int) (f * 15.0F * 16.0F);
        if (j > 240) {
            j = 240;
        }
        return j | k << 16;
    }

    public static class Provider implements ParticleProvider<TintedFlameParticleOptions> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(TintedFlameParticleOptions type, ClientLevel level,
                double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            TintedFlameParticle particle = new TintedFlameParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, type.rgb());
            particle.pickSprite(this.sprites);
            return particle;
        }
    }
}
