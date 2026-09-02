package com.devilswarchild.tintedfullspectrum;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.block.SoundType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

// The value here must match the entry in the META-INF/neoforge.mods.toml file
@Mod(TintedFullSpectrum.MODID)
public class TintedFullSpectrum {
    public static final String MODID = "tinted_full_spectrum";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(BuiltInRegistries.MENU, MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, MODID);
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, MODID);

    // The one packed-RGB component every tintable item/block in this mod shares -- see
    // tintable_system_and_planks.md and TintColorComponent.
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<TintColorComponent>> TINT_COLOR = DATA_COMPONENTS.register(
            "tint_color", () -> DataComponentType.<TintColorComponent>builder()
                    .persistent(TintColorComponent.CODEC)
                    .networkSynchronized(TintColorComponent.STREAM_CODEC)
                    .build());

    // Floor and wall variants of the RGB-tintable torch. Placement rules/behavior come from vanilla's
    // TorchBlock/WallTorchBlock; properties mirror vanilla's torch exactly (see Blocks.TORCH/WALL_TORCH).
    public static final DeferredBlock<Block> TINTED_FLOOR_TORCH = BLOCKS.register("tinted_floor_torch",
            () -> new TintedFloorTorchBlock(ParticleTypes.FLAME,
                    Properties.of().noCollission().instabreak().lightLevel(state -> 14).sound(SoundType.WOOD).pushReaction(PushReaction.DESTROY)));
    public static final DeferredBlock<Block> TINTED_WALL_TORCH = BLOCKS.register("tinted_wall_torch",
            () -> new TintedWallTorchBlock(ParticleTypes.FLAME,
                    Properties.of().noCollission().instabreak().lightLevel(state -> 14).sound(SoundType.WOOD)
                            .dropsLike(TINTED_FLOOR_TORCH.get()).pushReaction(PushReaction.DESTROY)));

    // A single item places either variant depending on where the player clicks, exactly like vanilla's "torch" item.
    public static final DeferredItem<TintableStandingAndWallBlockItem> TINTED_TORCH_ITEM = ITEMS.register("tinted_torch",
            () -> new TintableStandingAndWallBlockItem(TINTED_FLOOR_TORCH.get(), TINTED_WALL_TORCH.get(), new Item.Properties(), Direction.DOWN));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TintedTorchBlockEntity>> TINTED_TORCH_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
            "tinted_torch", () -> BlockEntityType.Builder.of(TintedTorchBlockEntity::new, TINTED_FLOOR_TORCH.get(), TINTED_WALL_TORCH.get()).build(null));

    // Tinted Planks: a plain tintable cube plus its stairs/slab/fence/fence-gate shape variants.
    // Base cube + slab blockstates/models are datagen'd (TintedDataGenerators); stairs/fence/fence
    // gate are hand-authored static files instead, since fully tinting their geometry needed custom
    // per-face tintindex that NeoForge's stairsBlock/fenceBlock/fenceGateBlock datagen helpers can't
    // produce (they always reference vanilla's own untinted templates).
    public static final DeferredBlock<Block> TINTED_PLANKS = BLOCKS.register("tinted_planks",
            () -> new TintedPlanksBlock(Properties.ofFullCopy(Blocks.OAK_PLANKS)));
    public static final DeferredBlock<Block> TINTED_PLANKS_STAIRS = BLOCKS.register("tinted_planks_stairs",
            () -> new TintedPlanksStairsBlock(TINTED_PLANKS.get().defaultBlockState(), Properties.ofFullCopy(Blocks.OAK_PLANKS)));
    public static final DeferredBlock<Block> TINTED_PLANKS_SLAB = BLOCKS.register("tinted_planks_slab",
            () -> new TintedPlanksSlabBlock(Properties.ofFullCopy(Blocks.OAK_PLANKS)));
    public static final DeferredBlock<Block> TINTED_PLANKS_FENCE = BLOCKS.register("tinted_planks_fence",
            () -> new TintedPlanksFenceBlock(Properties.ofFullCopy(Blocks.OAK_PLANKS)));
    public static final DeferredBlock<Block> TINTED_PLANKS_FENCE_GATE = BLOCKS.register("tinted_planks_fence_gate",
            () -> new TintedPlanksFenceGateBlock(Properties.ofFullCopy(Blocks.OAK_FENCE_GATE)));

    public static final DeferredItem<TintableBlockItem> TINTED_PLANKS_ITEM = ITEMS.register("tinted_planks",
            () -> new TintableBlockItem(TINTED_PLANKS.get(), new Item.Properties()));
    public static final DeferredItem<TintableBlockItem> TINTED_PLANKS_STAIRS_ITEM = ITEMS.register("tinted_planks_stairs",
            () -> new TintableBlockItem(TINTED_PLANKS_STAIRS.get(), new Item.Properties()));
    public static final DeferredItem<TintableBlockItem> TINTED_PLANKS_SLAB_ITEM = ITEMS.register("tinted_planks_slab",
            () -> new TintableBlockItem(TINTED_PLANKS_SLAB.get(), new Item.Properties()));
    public static final DeferredItem<TintableBlockItem> TINTED_PLANKS_FENCE_ITEM = ITEMS.register("tinted_planks_fence",
            () -> new TintableBlockItem(TINTED_PLANKS_FENCE.get(), new Item.Properties()));
    public static final DeferredItem<TintableBlockItem> TINTED_PLANKS_FENCE_GATE_ITEM = ITEMS.register("tinted_planks_fence_gate",
            () -> new TintableBlockItem(TINTED_PLANKS_FENCE_GATE.get(), new Item.Properties()));

    // One block entity type shared by all five plank shapes -- mirrors the torch's floor/wall sharing.
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TintedPlanksBlockEntity>> TINTED_PLANKS_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
            "tinted_planks", () -> BlockEntityType.Builder.of(TintedPlanksBlockEntity::new,
                    TINTED_PLANKS.get(), TINTED_PLANKS_STAIRS.get(), TINTED_PLANKS_SLAB.get(),
                    TINTED_PLANKS_FENCE.get(), TINTED_PLANKS_FENCE_GATE.get()).build(null));

    // The Chroma Alembic: faces the player at placement like a furnace; right-click opens the
    // dye-crafting GUI. See chroma_alembic_full_build.md.
    public static final DeferredBlock<Block> CHROMA_ALEMBIC = BLOCKS.register("chroma_alembic",
            () -> new ChromaAlembicBlock(Properties.of().mapColor(MapColor.METAL).strength(3.5f).sound(SoundType.METAL).noOcclusion()));
    public static final DeferredItem<BlockItem> CHROMA_ALEMBIC_ITEM = ITEMS.registerSimpleBlockItem("chroma_alembic", CHROMA_ALEMBIC);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ChromaAlembicBlockEntity>> CHROMA_ALEMBIC_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
            "chroma_alembic", () -> BlockEntityType.Builder.of(ChromaAlembicBlockEntity::new, CHROMA_ALEMBIC.get()).build(null));

    public static final DeferredHolder<MenuType<?>, MenuType<ChromaAlembicMenu>> CHROMA_ALEMBIC_MENU = MENU_TYPES.register("chroma_alembic",
            () -> IMenuTypeExtension.create((windowId, inv, buf) -> new ChromaAlembicMenu(windowId, inv, buf)));

    // Crafting input/output for the Chroma Alembic. Colored Dye reuses blank_dye.png untinted and
    // carries its RGB via this mod's own TINT_COLOR component (see above), read through an
    // ItemColor handler instead of the fixed 16-color DyeColor enum.
    public static final DeferredItem<Item> BLANK_DYE_ITEM = ITEMS.registerSimpleItem("blank_dye", new Item.Properties());
    public static final DeferredItem<ColoredDyeItem> COLORED_DYE_ITEM = ITEMS.register("colored_dye", () -> new ColoredDyeItem(new Item.Properties()));

    // Applies a Colored Dye's stored RGB to a vanilla DYEABLE item via crafting (see
    // ColoredDyeApplyRecipe) -- ColoredDyeItem#useOn handles the block-targeted case (right-click).
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ColoredDyeApplyRecipe>> COLORED_DYE_APPLY_SERIALIZER = RECIPE_SERIALIZERS.register(
            "colored_dye_apply", () -> new SimpleCraftingRecipeSerializer<>(ColoredDyeApplyRecipe::new));

    // Applies a Colored Dye's stored RGB to any TintableItem of this mod's own (torch, planks and its
    // shape variants, and any future material) -- see RecolorRecipe.
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<RecolorRecipe>> RECOLOR_SERIALIZER = RECIPE_SERIALIZERS.register(
            "recolor", () -> new SimpleCraftingRecipeSerializer<>(RecolorRecipe::new));

    // Converts any vanilla wood stairs/slab/fence/fence gate + Colored Dye into the equivalent
    // Tinted Planks shape in that color -- see ConvertAndDyeRecipe.
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ConvertAndDyeRecipe>> CONVERT_AND_DYE_SERIALIZER = RECIPE_SERIALIZERS.register(
            "convert_and_dye", () -> new SimpleCraftingRecipeSerializer<>(ConvertAndDyeRecipe::new));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN_TAB = CREATIVE_MODE_TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.tinted_full_spectrum"))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> CHROMA_ALEMBIC_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(TINTED_TORCH_ITEM.get());
                output.accept(TINTED_PLANKS_ITEM.get());
                output.accept(TINTED_PLANKS_STAIRS_ITEM.get());
                output.accept(TINTED_PLANKS_SLAB_ITEM.get());
                output.accept(TINTED_PLANKS_FENCE_ITEM.get());
                output.accept(TINTED_PLANKS_FENCE_GATE_ITEM.get());
                output.accept(CHROMA_ALEMBIC_ITEM.get());
                output.accept(BLANK_DYE_ITEM.get());
                output.accept(COLORED_DYE_ITEM.get());
            }).build());

    // FML recognizes some parameter types like IEventBus or ModContainer and passes them in automatically.
    public TintedFullSpectrum(IEventBus modEventBus, ModContainer modContainer) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
        MENU_TYPES.register(modEventBus);
        RECIPE_SERIALIZERS.register(modEventBus);
        DATA_COMPONENTS.register(modEventBus);

        // Register the Chroma Alembic's client->server "Selected" color payload
        modEventBus.addListener(this::registerPayloads);

        modEventBus.addListener(this::addCreative);
        modEventBus.addListener(TintedDataGenerators::gatherData);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(TINTED_TORCH_ITEM);
            event.accept(TINTED_PLANKS_ITEM);
            event.accept(TINTED_PLANKS_STAIRS_ITEM);
            event.accept(TINTED_PLANKS_SLAB_ITEM);
            event.accept(TINTED_PLANKS_FENCE_ITEM);
            event.accept(TINTED_PLANKS_FENCE_GATE_ITEM);
            event.accept(CHROMA_ALEMBIC_ITEM);
        }
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(ChromaAlembicSetColorPayload.TYPE, ChromaAlembicSetColorPayload.STREAM_CODEC, ChromaAlembicSetColorPayload::handle);
    }
}
