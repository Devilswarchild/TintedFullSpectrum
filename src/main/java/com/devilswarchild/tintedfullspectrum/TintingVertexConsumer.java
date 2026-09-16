package com.devilswarchild.tintedfullspectrum;

import com.mojang.blaze3d.vertex.VertexConsumer;

// Wraps a real VertexConsumer and multiplies every vertex color written through it by a fixed RGB
// tint, applied on top of whatever color the wrapped call chain already produced. Used by
// TintedFallingBlockRenderer to tint a falling block's mesh with this mod's own stored color: the
// normal block-tesselation path always resolves tintindex-0 faces through BlockColors keyed off
// (state, level, pos), which has nothing to read mid-air (no real block entity exists at a falling
// entity's position) and falls back to plain white -- i.e. the texture's own neutral gray. Multiplying
// that by our synced entity color here, after the fact, produces the correct final color without
// needing BlockColors to know anything about the falling entity at all.
class TintingVertexConsumer implements VertexConsumer {
    private final VertexConsumer delegate;
    private final int tintR;
    private final int tintG;
    private final int tintB;

    TintingVertexConsumer(VertexConsumer delegate, int rgb) {
        this.delegate = delegate;
        this.tintR = (rgb >> 16) & 0xFF;
        this.tintG = (rgb >> 8) & 0xFF;
        this.tintB = rgb & 0xFF;
    }

    @Override
    public VertexConsumer addVertex(float x, float y, float z) {
        delegate.addVertex(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer setColor(int red, int green, int blue, int alpha) {
        delegate.setColor(red * tintR / 255, green * tintG / 255, blue * tintB / 255, alpha);
        return this;
    }

    @Override
    public VertexConsumer setUv(float u, float v) {
        delegate.setUv(u, v);
        return this;
    }

    @Override
    public VertexConsumer setUv1(int u, int v) {
        delegate.setUv1(u, v);
        return this;
    }

    @Override
    public VertexConsumer setUv2(int u, int v) {
        delegate.setUv2(u, v);
        return this;
    }

    @Override
    public VertexConsumer setNormal(float normalX, float normalY, float normalZ) {
        delegate.setNormal(normalX, normalY, normalZ);
        return this;
    }
}
