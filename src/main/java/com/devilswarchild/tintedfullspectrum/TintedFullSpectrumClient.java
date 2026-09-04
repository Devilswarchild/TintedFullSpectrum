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
        }, tintableBlocksArray());

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
                tintableItemsArray());

        // Door items specifically: a real player-crafted/dyed door always carries an explicit
        // TintColorComponent (even a "white" one is set explicitly by the recolor/convert recipes),
        // so its absence means this ItemStack is a template display -- the creative tab, JEI, or the
        // recipe book's "any color works" grid slot -- not a real item. For that case, show each
        // material's own natural color (e.g. oak shows oak-brown) instead of a flat white default --
        // our own textures are grayscale/tint-ready, so a plain white multiply would just look flat
        // gray rather than resembling the real material the way vanilla's own doors do. An earlier
        // version animated a rainbow hue cycle here instead; reverted per explicit user request in
        // favor of this more predictable, JEI/EMI/REI-screenshot-friendly static look.
        java.util.Map<net.minecraft.world.item.Item, Integer> doorDefaultColors = doorDefaultColorsMap();
        event.register((stack, tintIndex) -> {
            try {
                if (stack.has(TintedFullSpectrum.TINT_COLOR.get())) {
                    return TintColorComponent.getOrDefault(stack, TintColorComponent.DEFAULT_COLOR);
                }
                return doorDefaultColors.getOrDefault(stack.getItem(), 0xFFFFFF);
            } catch (Throwable t) {
                TintedFullSpectrum.LOGGER.error("Door ItemColor handler threw for {}", stack.getItem(), t);
                return 0xFFFFFF;
            }
        }, doorItemsArray());

        event.register((stack, tintIndex) -> 0xFFFFFF, TintedFullSpectrum.CHROMA_ALEMBIC_ITEM.get());
    }

    // Average pixel color of each material's real vanilla door texture (computed once from the
    // actual game assets, not eyeballed) -- gives each tinted door a "looks like the real material"
    // default instead of a generic white/gray. Hourglass Door isn't tied to any one wood species, so
    // it reuses oak's color per explicit user request ("hourglass should render brown to match oak").
    private static java.util.Map<net.minecraft.world.item.Item, Integer> doorDefaultColorsMap() {
        java.util.Map<String, Integer> byMaterial = java.util.Map.ofEntries(
                java.util.Map.entry("oak", 0x8C6D40), java.util.Map.entry("spruce", 0x6A5031),
                java.util.Map.entry("birch", 0xD6CAA0), java.util.Map.entry("jungle", 0xA17452),
                java.util.Map.entry("acacia", 0xA55E3B), java.util.Map.entry("cherry", 0xE1AEA7),
                java.util.Map.entry("dark_oak", 0x4B3219), java.util.Map.entry("mangrove", 0x71302F),
                java.util.Map.entry("bamboo", 0xC4AF53), java.util.Map.entry("crimson", 0x73374F),
                java.util.Map.entry("warped", 0x2C7B75), java.util.Map.entry("iron", 0xC3C2C2));
        java.util.Map<net.minecraft.world.item.Item, Integer> byItem = new java.util.HashMap<>();
        for (var entry : TintedFullSpectrum.TINTED_DOOR_ITEMS.entrySet()) {
            Integer color = byMaterial.get(entry.getKey());
            if (color != null) {
                byItem.put(entry.getValue().get(), color);
            }
        }
        byItem.put(TintedFullSpectrum.HOURGLASS_DOOR_ITEM.get(), byMaterial.get("oak"));
        return byItem;
    }

    private static net.minecraft.world.level.block.Block[] tintableBlocksArray() {
        java.util.List<net.minecraft.world.level.block.Block> blocks = new java.util.ArrayList<>(java.util.List.of(
                TintedFullSpectrum.TINTED_FLOOR_TORCH.get(), TintedFullSpectrum.TINTED_WALL_TORCH.get(),
                TintedFullSpectrum.TINTED_PLANKS.get(), TintedFullSpectrum.TINTED_PLANKS_STAIRS.get(),
                TintedFullSpectrum.TINTED_PLANKS_SLAB.get(), TintedFullSpectrum.TINTED_PLANKS_FENCE.get(),
                TintedFullSpectrum.TINTED_PLANKS_FENCE_GATE.get(), TintedFullSpectrum.HOURGLASS_DOOR.get()));
        for (var block : TintedFullSpectrum.TINTED_DOOR_BLOCKS.values()) {
            blocks.add(block.get());
        }
        return blocks.toArray(new net.minecraft.world.level.block.Block[0]);
    }

    private static net.minecraft.world.item.Item[] tintableItemsArray() {
        return new net.minecraft.world.item.Item[] {
                TintedFullSpectrum.TINTED_TORCH_ITEM.get(), TintedFullSpectrum.COLORED_DYE_ITEM.get(),
                TintedFullSpectrum.TINTED_PLANKS_ITEM.get(), TintedFullSpectrum.TINTED_PLANKS_STAIRS_ITEM.get(),
                TintedFullSpectrum.TINTED_PLANKS_SLAB_ITEM.get(), TintedFullSpectrum.TINTED_PLANKS_FENCE_ITEM.get(),
                TintedFullSpectrum.TINTED_PLANKS_FENCE_GATE_ITEM.get(),
        };
    }

    // Door items only -- see the cycling-color comment above for why these need their own handler.
    private static net.minecraft.world.item.Item[] doorItemsArray() {
        java.util.List<net.minecraft.world.item.Item> items = new java.util.ArrayList<>();
        items.add(TintedFullSpectrum.HOURGLASS_DOOR_ITEM.get());
        for (var item : TintedFullSpectrum.TINTED_DOOR_ITEMS.values()) {
            items.add(item.get());
        }
        return items.toArray(new net.minecraft.world.item.Item[0]);
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
    // Built once at client setup (after all registries are populated), not per-frame -- avoids
    // re-deriving the door block set (or worse, a Stream) on every RenderHighlightEvent, which fires
    // for every single block the player looks at, every frame.
    private static java.util.Set<net.minecraft.world.level.block.Block> allDoorBlocks;

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemBlockRenderTypes.setRenderLayer(TintedFullSpectrum.HOURGLASS_DOOR.get(), RenderType.cutout());
            for (var block : TintedFullSpectrum.TINTED_DOOR_BLOCKS.values()) {
                ItemBlockRenderTypes.setRenderLayer(block.get(), RenderType.cutout());
            }
        });

        allDoorBlocks = new java.util.HashSet<>();
        allDoorBlocks.add(TintedFullSpectrum.HOURGLASS_DOOR.get());
        for (var block : TintedFullSpectrum.TINTED_DOOR_BLOCKS.values()) {
            allDoorBlocks.add(block.get());
        }

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
        if (!allDoorBlocks.contains(state.getBlock())) {
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
