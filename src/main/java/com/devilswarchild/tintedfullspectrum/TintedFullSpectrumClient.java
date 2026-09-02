package com.devilswarchild.tintedfullspectrum;

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
    // One handler for every tintable block (reads its live block entity color), shared rather than
    // duplicated per material -- see tintable_system_and_planks.md.
    @SubscribeEvent
    static void onRegisterBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) -> {
            if (level != null && pos != null && level.getBlockEntity(pos) instanceof TintableBlockEntity tintable) {
                return tintable.getColor();
            }
            return TintColorComponent.DEFAULT_COLOR;
        }, TintedFullSpectrum.TINTED_FLOOR_TORCH.get(), TintedFullSpectrum.TINTED_WALL_TORCH.get(),
                TintedFullSpectrum.TINTED_PLANKS.get(), TintedFullSpectrum.TINTED_PLANKS_STAIRS.get(),
                TintedFullSpectrum.TINTED_PLANKS_SLAB.get(), TintedFullSpectrum.TINTED_PLANKS_FENCE.get(),
                TintedFullSpectrum.TINTED_PLANKS_FENCE_GATE.get());

        // The lens/tint-preview overlay tracks whatever color is currently selected in the GUI (not
        // just the last completed craft), read straight off the block entity -- ChromaAlembicRenderer
        // re-queries this every frame rather than baking it into a chunk mesh, so it updates live as
        // the player drags sliders, no rebake workaround needed (see blockcolor_rebake_gotcha memory,
        // which only applies to BlockColors-baked chunk meshes, not per-frame BlockEntityRenderer
        // reads). Note this same tintIndex-0 color also multiplies the pedestal's break/dig particles
        // (TerrainParticle), which is an acceptable side effect here.
        event.register((state, level, pos, tintIndex) -> {
            if (level != null && pos != null && level.getBlockEntity(pos) instanceof ChromaAlembicBlockEntity alembic) {
                return alembic.getSelectedColor();
            }
            return 0xFFFFFF;
        }, TintedFullSpectrum.CHROMA_ALEMBIC.get());
    }

    // One handler for every tintable item (reads its stored TintColorComponent), shared rather than
    // duplicated per material.
    @SubscribeEvent
    static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> TintColorComponent.getOrDefault(stack, TintColorComponent.DEFAULT_COLOR),
                TintedFullSpectrum.TINTED_TORCH_ITEM.get(), TintedFullSpectrum.COLORED_DYE_ITEM.get(),
                TintedFullSpectrum.TINTED_PLANKS_ITEM.get(), TintedFullSpectrum.TINTED_PLANKS_STAIRS_ITEM.get(),
                TintedFullSpectrum.TINTED_PLANKS_SLAB_ITEM.get(), TintedFullSpectrum.TINTED_PLANKS_FENCE_ITEM.get(),
                TintedFullSpectrum.TINTED_PLANKS_FENCE_GATE_ITEM.get());
        event.register((stack, tintIndex) -> 0xFFFFFF, TintedFullSpectrum.CHROMA_ALEMBIC_ITEM.get());
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
