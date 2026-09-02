package com.devilswarchild.tintedfullspectrum;

import net.minecraft.world.item.component.DyedItemColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = TintedFullSpectrum.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = TintedFullSpectrum.MODID, value = Dist.CLIENT)
public class TintedFullSpectrumClient {
    // Reads the tint (tintindex 0, embers + flame) from the placed block's block entity.
    // Falls back to DEFAULT_COLOR (white/untinted) when there's no block entity yet (e.g. inventory context).
    @SubscribeEvent
    static void onRegisterBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) -> {
            if (level != null && pos != null && level.getBlockEntity(pos) instanceof TintedTorchBlockEntity tintedTorch) {
                return tintedTorch.getColor();
            }
            return TintedTorchBlockEntity.DEFAULT_COLOR;
        }, TintedFullSpectrum.TINTED_FLOOR_TORCH.get(), TintedFullSpectrum.TINTED_WALL_TORCH.get());

        // The Chroma Alembic's tint-preview overlay/lens has no color storage of its own yet, so it
        // stays untinted. White here matters beyond the lens itself -- TerrainParticle multiplies
        // break/dig particles by this same tintIndex-0 color regardless of which texture they
        // sample, so anything but white would tint the pedestal's break particles too.
        event.register((state, level, pos, tintIndex) -> 0xFFFFFF, TintedFullSpectrum.CHROMA_ALEMBIC.get());
    }

    @SubscribeEvent
    static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> TintedTorchBlockEntity.DEFAULT_COLOR, TintedFullSpectrum.TINTED_TORCH_ITEM.get());
        event.register((stack, tintIndex) -> 0xFFFFFF, TintedFullSpectrum.CHROMA_ALEMBIC_ITEM.get());

        // Colored Dye reuses blank_dye.png untinted (layer0, tintindex 0) and carries its RGB via the
        // same DataComponents.DYED_COLOR component vanilla uses for dyed leather armor.
        event.register((stack, tintIndex) -> DyedItemColor.getOrDefault(stack, 0xFFFFFF), TintedFullSpectrum.COLORED_DYE_ITEM.get());
    }

    // The static/platform halves aren't referenced by any blockstate (the placed block's model is
    // invisible; ChromaAlembicRenderer draws them directly), so they need to be side-loaded here.
    @SubscribeEvent
    static void onRegisterAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(ChromaAlembicRenderer.STATIC_MODEL);
        event.register(ChromaAlembicRenderer.PLATFORM_MODEL);
    }

    @SubscribeEvent
    static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(TintedFullSpectrum.CHROMA_ALEMBIC_BLOCK_ENTITY.get(), ChromaAlembicRenderer::new);
    }

    @SubscribeEvent
    static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(TintedFullSpectrum.CHROMA_ALEMBIC_MENU.get(), ChromaAlembicScreen::new);
    }
}
