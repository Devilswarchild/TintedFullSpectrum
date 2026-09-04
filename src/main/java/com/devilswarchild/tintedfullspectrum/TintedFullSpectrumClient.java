package com.devilswarchild.tintedfullspectrum;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import net.neoforged.neoforge.common.NeoForge;

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
                TintedFullSpectrum.TINTED_PLANKS_FENCE_GATE.get(), TintedFullSpectrum.HOURGLASS_DOOR.get(),
                TintedFullSpectrum.TINTED_DOOR.get());

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
                TintedFullSpectrum.TINTED_PLANKS_FENCE_GATE_ITEM.get(), TintedFullSpectrum.HOURGLASS_DOOR_ITEM.get(),
                TintedFullSpectrum.TINTED_DOOR_ITEM.get());
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

    // Neither door block registers a render type otherwise, so both default to solid, which ignores
    // alpha entirely -- that's why the Hourglass Door's window wasn't see-through and (per the wider
    // "solid rendering handles partial-transparency content inconsistently" issue) the tinted panel
    // looked wrong too. Vanilla's own doors register cutout the same way, just internally.
    //
    // The Hourglass Door now uses a single tinted element per state (no separate untinted overlay,
    // matching MCreator's original single-texture design) so there's no more coplanar-quad
    // z-fighting risk -- translucent is safe again, letting window art stay genuinely see-through.
    // The model JSON's own "render_type" field (matching MCreator's approach) is the actual source
    // of truth per-block; this Java registration just keeps it consistent as a fallback.
    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemBlockRenderTypes.setRenderLayer(TintedFullSpectrum.HOURGLASS_DOOR.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(TintedFullSpectrum.TINTED_DOOR.get(), RenderType.cutout());
        });
        // RenderHighlightEvent fires on the main game event bus, not this mod's own bus, so it
        // needs an explicit registration here rather than the usual @EventBusSubscriber class-level
        // annotation (which only covers one bus per class).
        NeoForge.EVENT_BUS.addListener(TintedFullSpectrumClient::onRenderBlockHighlight);
    }

    // Vanilla's own block-targeting outline is already solid black (0,0,0) -- but at only 40% alpha,
    // which visibly warps/washes out when blended against our doors' own translucent rendering
    // (double alpha-blending between the outline pass and the door's own translucent pass). Redraw
    // it at full opacity for just these two blocks instead of vanilla's default 40%.
    private static void onRenderBlockHighlight(RenderHighlightEvent.Block event) {
        BlockPos pos = event.getTarget().getBlockPos();
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() == TintedFullSpectrum.HOURGLASS_DOOR.get() || state.getBlock() == TintedFullSpectrum.TINTED_DOOR.get())) {
            return;
        }
        event.setCanceled(true);

        Vec3 camPos = event.getCamera().getPosition();
        VoxelShape shape = state.getShape(level, pos, CollisionContext.of(event.getCamera().getEntity()));
        MultiBufferSource bufferSource = event.getMultiBufferSource();
        LevelRenderer.renderVoxelShape(event.getPoseStack(), bufferSource.getBuffer(RenderType.lines()), shape,
                pos.getX() - camPos.x, pos.getY() - camPos.y, pos.getZ() - camPos.z, 0f, 0f, 0f, 1.0f, false);
    }
}
