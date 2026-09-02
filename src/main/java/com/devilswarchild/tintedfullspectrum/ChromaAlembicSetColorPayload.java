package com.devilswarchild.tintedfullspectrum;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

// Client -> server: live "Selected" RGB updates from the GUI's hex field/sliders. The block entity
// is shared, machine-scoped state (not per-player), so this writes straight into it -- see
// ChromaAlembicBlockEntity#setSelectedColor and chroma_alembic_full_build.md's Section 1 spec.
public record ChromaAlembicSetColorPayload(BlockPos pos, int rgb) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ChromaAlembicSetColorPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(TintedFullSpectrum.MODID, "chroma_alembic_set_color"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ChromaAlembicSetColorPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ChromaAlembicSetColorPayload::pos,
            ByteBufCodecs.INT, ChromaAlembicSetColorPayload::rgb,
            ChromaAlembicSetColorPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ChromaAlembicSetColorPayload payload, IPayloadContext context) {
        if (context.player().containerMenu instanceof ChromaAlembicMenu menu
                && menu.getBlockEntity().getBlockPos().equals(payload.pos())) {
            menu.getBlockEntity().setSelectedColor(payload.rgb());
        }
    }
}
