package com.devilswarchild.tintedfullspectrum;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.Direction;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.block.SoundType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
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
    public static final DeferredRegister<net.minecraft.core.particles.ParticleType<?>> PARTICLE_TYPES = DeferredRegister.create(Registries.PARTICLE_TYPE, MODID);

    // The vanilla-parity Torch's flame particle, tinted to match the torch's stored color -- see
    // TintedFlameParticleOptions/TintedFlameParticle.
    public static final DeferredHolder<net.minecraft.core.particles.ParticleType<?>, net.minecraft.core.particles.ParticleType<TintedFlameParticleOptions>> TINTED_FLAME_PARTICLE = PARTICLE_TYPES.register(
            "tinted_flame", () -> new net.minecraft.core.particles.ParticleType<TintedFlameParticleOptions>(false) {
                @Override
                public com.mojang.serialization.MapCodec<TintedFlameParticleOptions> codec() {
                    return TintedFlameParticleOptions.CODEC;
                }

                @Override
                public net.minecraft.network.codec.StreamCodec<? super net.minecraft.network.RegistryFriendlyByteBuf, TintedFlameParticleOptions> streamCodec() {
                    return TintedFlameParticleOptions.STREAM_CODEC;
                }
            });

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

    // Vanilla-parity Torch: vanilla's own torch/wall_torch cross-billboard shape, tintable, alongside
    // the fully custom bracket-shaped torch above (same one-custom/one-vanilla-parity pairing as the
    // two door families). Only the flame and the charred top of the stick tint; the rest of the stick
    // stays a fixed real color -- see models/block/custom/tinted_vanilla_torch(_wall).json.
    public static final DeferredBlock<Block> TINTED_VANILLA_TORCH = BLOCKS.register("tinted_vanilla_torch",
            () -> new TintedVanillaTorchBlock(ParticleTypes.FLAME,
                    Properties.of().noCollission().instabreak().lightLevel(state -> 14).sound(SoundType.WOOD).pushReaction(PushReaction.DESTROY)));
    public static final DeferredBlock<Block> TINTED_VANILLA_WALL_TORCH = BLOCKS.register("tinted_vanilla_wall_torch",
            () -> new TintedVanillaWallTorchBlock(ParticleTypes.FLAME,
                    Properties.of().noCollission().instabreak().lightLevel(state -> 14).sound(SoundType.WOOD)
                            .dropsLike(TINTED_VANILLA_TORCH.get()).pushReaction(PushReaction.DESTROY)));

    public static final DeferredItem<TintableStandingAndWallBlockItem> TINTED_VANILLA_TORCH_ITEM = ITEMS.register("tinted_vanilla_torch",
            () -> new TintableStandingAndWallBlockItem(TINTED_VANILLA_TORCH.get(), TINTED_VANILLA_WALL_TORCH.get(), new Item.Properties(), Direction.DOWN));

    // Shared with the custom torch's own block entity type -- both just store a color, no torch-shape-specific behavior.
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TintedTorchBlockEntity>> TINTED_TORCH_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
            "tinted_torch", () -> BlockEntityType.Builder.of(TintedTorchBlockEntity::new,
                    TINTED_FLOOR_TORCH.get(), TINTED_WALL_TORCH.get(),
                    TINTED_VANILLA_TORCH.get(), TINTED_VANILLA_WALL_TORCH.get()).build(null));

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

    // Two tintable door families, sharing one block entity type: the Hourglass Door (fully custom
    // geometry) and the vanilla-parity Door (vanilla's own silhouette, just recolorable). See
    // tinted_full_spectrum_doors_handoff.md.
    public static final DeferredBlock<Block> HOURGLASS_DOOR = BLOCKS.register("hourglass_door",
            () -> new TintedHourglassDoorBlock(Properties.ofFullCopy(Blocks.OAK_DOOR)));
    public static final DeferredItem<TintableBlockItem> HOURGLASS_DOOR_ITEM = ITEMS.register("hourglass_door",
            () -> new TintableBlockItem(HOURGLASS_DOOR.get(), new Item.Properties()));

    // The vanilla-parity Tinted Door comes in one variant per vanilla door material (11 wood species
    // + iron, matching vanilla's own per-material door blocks) so a converted door keeps its source
    // material's own grain/look, just tinted -- unlike Tinted Planks, which is deliberately
    // material-agnostic. Geometry is identical across all variants (vanilla's plain door box +
    // tintindex); only the texture differs, via per-material child models parenting to the 4 shared
    // parameterized templates under models/block/custom/tinted_door_*.json. Copper (all 4 oxidation
    // stages + waxed) was deliberately left out entirely (confirmed with user) -- the stages have
    // real, subtly distinct colors of their own, different enough that an arbitrary RGB tint doesn't
    // add anything worth the complexity; those doors and their vanilla icons are untouched.
    public static final java.util.List<String> TINTED_DOOR_MATERIALS = java.util.List.of(
            "oak", "spruce", "birch", "jungle", "acacia", "cherry", "dark_oak", "mangrove", "bamboo",
            "crimson", "warped", "iron");

    public static final java.util.Map<String, DeferredBlock<Block>> TINTED_DOOR_BLOCKS = registerTintedDoorBlocks();
    public static final java.util.Map<String, DeferredItem<TintableBlockItem>> TINTED_DOOR_ITEMS = registerTintedDoorItems();

    private static java.util.Map<String, DeferredBlock<Block>> registerTintedDoorBlocks() {
        java.util.Map<String, DeferredBlock<Block>> map = new java.util.LinkedHashMap<>();
        for (String material : TINTED_DOOR_MATERIALS) {
            map.put(material, BLOCKS.register("tinted_" + material + "_door",
                    () -> new TintedDoorBlock(Properties.ofFullCopy(Blocks.OAK_DOOR))));
        }
        return map;
    }

    private static java.util.Map<String, DeferredItem<TintableBlockItem>> registerTintedDoorItems() {
        java.util.Map<String, DeferredItem<TintableBlockItem>> map = new java.util.LinkedHashMap<>();
        for (String material : TINTED_DOOR_MATERIALS) {
            DeferredBlock<Block> block = TINTED_DOOR_BLOCKS.get(material);
            map.put(material, ITEMS.register("tinted_" + material + "_door",
                    () -> new TintableBlockItem(block.get(), new Item.Properties())));
        }
        return map;
    }

    // Maps each vanilla door Block to the material name its Tinted Door equivalent is registered
    // under -- used by ConvertAndDyeRecipe to pick the matching output. Keyed by Block identity
    // rather than a tag/name string since that's what's actually available at match time.
    public static final java.util.Map<Block, String> VANILLA_DOOR_MATERIAL = java.util.Map.ofEntries(
            java.util.Map.entry(Blocks.OAK_DOOR, "oak"), java.util.Map.entry(Blocks.SPRUCE_DOOR, "spruce"),
            java.util.Map.entry(Blocks.BIRCH_DOOR, "birch"), java.util.Map.entry(Blocks.JUNGLE_DOOR, "jungle"),
            java.util.Map.entry(Blocks.ACACIA_DOOR, "acacia"), java.util.Map.entry(Blocks.CHERRY_DOOR, "cherry"),
            java.util.Map.entry(Blocks.DARK_OAK_DOOR, "dark_oak"), java.util.Map.entry(Blocks.MANGROVE_DOOR, "mangrove"),
            java.util.Map.entry(Blocks.BAMBOO_DOOR, "bamboo"), java.util.Map.entry(Blocks.CRIMSON_DOOR, "crimson"),
            java.util.Map.entry(Blocks.WARPED_DOOR, "warped"), java.util.Map.entry(Blocks.IRON_DOOR, "iron"));
    // Copper doors (all 4 oxidation stages, plus waxed) are deliberately NOT in this map -- confirmed
    // with user, their natural oxidation colors are distinct enough on their own that an RGB tint
    // isn't worth it. ConvertAndDyeRecipe treats "not in this map" as "not convertible" for doors, so
    // the recipe just doesn't match at all for these -- no phantom empty-result preview.

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TintedDoorBlockEntity>> TINTED_DOOR_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
            "tinted_door", () -> {
                java.util.List<Block> valid = new java.util.ArrayList<>();
                valid.add(HOURGLASS_DOOR.get());
                for (DeferredBlock<Block> block : TINTED_DOOR_BLOCKS.values()) {
                    valid.add(block.get());
                }
                return BlockEntityType.Builder.of(TintedDoorBlockEntity::new, valid.toArray(new Block[0])).build(null);
            });

    // Vanilla-parity Grass Block / Short Grass / Tall Grass -- each a direct extension of vanilla's
    // own class (GrassBlock/TallGrassBlock/DoublePlantBlock), so spreading, dying without light,
    // bonemeal, and shearing all still work exactly like the real thing, just tintable. Only one
    // material each (unlike the 12-material door family), so no per-material map/loop is needed.
    public static final DeferredBlock<Block> TINTED_GRASS_BLOCK = BLOCKS.register("tinted_grass_block",
            () -> new TintedGrassBlock(Properties.ofFullCopy(Blocks.GRASS_BLOCK)));
    public static final DeferredBlock<Block> TINTED_SHORT_GRASS = BLOCKS.register("tinted_short_grass",
            () -> new TintedShortGrassBlock(Properties.ofFullCopy(Blocks.SHORT_GRASS)));
    public static final DeferredBlock<Block> TINTED_TALL_GRASS = BLOCKS.register("tinted_tall_grass",
            () -> new TintedTallGrassBlock(Properties.ofFullCopy(Blocks.TALL_GRASS)));

    public static final DeferredItem<TintableBlockItem> TINTED_GRASS_BLOCK_ITEM = ITEMS.register("tinted_grass_block",
            () -> new TintableBlockItem(TINTED_GRASS_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<TintableBlockItem> TINTED_SHORT_GRASS_ITEM = ITEMS.register("tinted_short_grass",
            () -> new TintableBlockItem(TINTED_SHORT_GRASS.get(), new Item.Properties()));
    public static final DeferredItem<TintableDoubleHighBlockItem> TINTED_TALL_GRASS_ITEM = ITEMS.register("tinted_tall_grass",
            () -> new TintableDoubleHighBlockItem(TINTED_TALL_GRASS.get(), new Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TintedGrassBlockEntity>> TINTED_GRASS_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
            "tinted_grass", () -> BlockEntityType.Builder.of(TintedGrassBlockEntity::new,
                    TINTED_GRASS_BLOCK.get(), TINTED_SHORT_GRASS.get(), TINTED_TALL_GRASS.get()).build(null));

    // Vanilla-parity Wool / Carpet -- vanilla itself has 16 separately-painted colors for each with
    // no shared tinting mechanism, so this is arguably more "vanilla-parity in spirit" than in
    // mechanism: one tintable block covers what vanilla needed 16 fixed textures for.
    public static final DeferredBlock<Block> TINTED_WOOL = BLOCKS.register("tinted_wool",
            () -> new TintedWoolBlock(Properties.ofFullCopy(Blocks.WHITE_WOOL)));
    public static final DeferredBlock<Block> TINTED_CARPET = BLOCKS.register("tinted_carpet",
            () -> new TintedCarpetBlock(Properties.ofFullCopy(Blocks.WHITE_CARPET)));

    public static final DeferredItem<TintableBlockItem> TINTED_WOOL_ITEM = ITEMS.register("tinted_wool",
            () -> new TintableBlockItem(TINTED_WOOL.get(), new Item.Properties()));
    public static final DeferredItem<TintableBlockItem> TINTED_CARPET_ITEM = ITEMS.register("tinted_carpet",
            () -> new TintableBlockItem(TINTED_CARPET.get(), new Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TintedWoolBlockEntity>> TINTED_WOOL_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
            "tinted_wool", () -> BlockEntityType.Builder.of(TintedWoolBlockEntity::new,
                    TINTED_WOOL.get(), TINTED_CARPET.get()).build(null));

    // Chroma Glass Pane: vanilla-parity glass pane, panes only (no solid glass block -- deliberate
    // scope call, see tinted_full_spectrum_glass_handoff.md). Named to avoid colliding in spirit with
    // vanilla's own real minecraft:tinted_glass (the copper-frosted block).
    public static final DeferredBlock<Block> CHROMA_GLASS_PANE = BLOCKS.register("chroma_glass_pane",
            () -> new ChromaGlassPaneBlock(Properties.ofFullCopy(Blocks.GLASS_PANE)));
    public static final DeferredItem<TintableBlockItem> CHROMA_GLASS_PANE_ITEM = ITEMS.register("chroma_glass_pane",
            () -> new TintableBlockItem(CHROMA_GLASS_PANE.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ChromaGlassPaneBlockEntity>> CHROMA_GLASS_PANE_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
            "chroma_glass_pane", () -> BlockEntityType.Builder.of(ChromaGlassPaneBlockEntity::new, CHROMA_GLASS_PANE.get()).build(null));

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

    // Blank Dye's own acquisition chain: smelt Bone -> Bone Ash, wet it in a water cauldron -> Dye
    // Paste (see CauldronInteraction registration in the constructor), smelt that -> Blank Dye
    // (drying it out). Both are plain intermediate items, no special behavior of their own.
    public static final DeferredItem<Item> BONE_ASH_ITEM = ITEMS.registerSimpleItem("bone_ash", new Item.Properties());
    public static final DeferredItem<Item> DYE_PASTE_ITEM = ITEMS.registerSimpleItem("dye_paste", new Item.Properties());

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

    // 2 Tinted Wool (same color) -> 3 Tinted Carpet (that color) -- see WoolToCarpetRecipe.
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<WoolToCarpetRecipe>> WOOL_TO_CARPET_SERIALIZER = RECIPE_SERIALIZERS.register(
            "wool_to_carpet", () -> new SimpleCraftingRecipeSerializer<>(WoolToCarpetRecipe::new));

    // 4 String + Colored Dye -> Tinted Wool -- see StringToWoolRecipe.
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<StringToWoolRecipe>> STRING_TO_WOOL_SERIALIZER = RECIPE_SERIALIZERS.register(
            "string_to_wool", () -> new SimpleCraftingRecipeSerializer<>(StringToWoolRecipe::new));

    // 8 glass pane + Colored Dye -> 8 Chroma Glass Pane -- see ChromaGlassPaneRecipe.
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ChromaGlassPaneRecipe>> CHROMA_GLASS_PANE_SERIALIZER = RECIPE_SERIALIZERS.register(
            "chroma_glass_pane", () -> new SimpleCraftingRecipeSerializer<>(ChromaGlassPaneRecipe::new));


    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN_TAB = CREATIVE_MODE_TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.tinted_full_spectrum"))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> CHROMA_ALEMBIC_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                // Only items this mod personally originated go in this tab: the custom-geometry
                // Tinted Torch, the Hourglass Door, the Chroma Alembic, and the two dyes. Every
                // vanilla-parity conversion output (Tinted Vanilla Torch, all 5 Tinted Planks shapes,
                // all 12 Tinted Door materials) is deliberately excluded -- those are only ever meant
                // to be obtained by converting the matching vanilla item + a Colored Dye (see
                // ConvertAndDyeRecipe), not browsed/taken blank from creative.
                output.accept(TINTED_TORCH_ITEM.get());
                output.accept(HOURGLASS_DOOR_ITEM.get());
                output.accept(CHROMA_ALEMBIC_ITEM.get());
                output.accept(BONE_ASH_ITEM.get());
                output.accept(DYE_PASTE_ITEM.get());
                output.accept(BLANK_DYE_ITEM.get());
                // COLORED_DYE_ITEM deliberately not listed -- it's the Chroma Alembic's OUTPUT (any
                // RGB a player mixes), not a pre-made item to browse; Blank Dye is the raw input.
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
        PARTICLE_TYPES.register(modEventBus);

        // Register the Chroma Alembic's client->server "Selected" color payload
        modEventBus.addListener(this::registerPayloads);

        modEventBus.addListener(this::commonSetup);

        modEventBus.addListener(TintedDataGenerators::gatherData);
    }

    // Deliberately no BuildCreativeModeTabContentsEvent listener -- nothing from this mod belongs in
    // any vanilla creative tab (Building Blocks, Ingredients, etc). Every item this mod has to offer
    // is reachable from MAIN_TAB.displayItems above, and only from there.

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(ChromaAlembicSetColorPayload.TYPE, ChromaAlembicSetColorPayload.STREAM_CODEC, ChromaAlembicSetColorPayload::handle);
    }

    // Right-clicking Bone Ash into a water cauldron wets it into Dye Paste -- the middle step of Blank
    // Dye's acquisition chain (smelt Bone -> Bone Ash -> [this] -> Dye Paste -> smelt -> Blank Dye).
    // Mirrors vanilla's own CauldronInteraction.WATER entries (e.g. the GLASS_BOTTLE -> water-bottle
    // interaction) closely: consumes one cauldron water level, transforms exactly one item out of the
    // held stack (not the whole stack), server-side only.
    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> CauldronInteraction.WATER.map().put(BONE_ASH_ITEM.get(),
                (state, level, pos, player, hand, stack) -> {
                    if (!level.isClientSide) {
                        Item item = stack.getItem();
                        player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(DYE_PASTE_ITEM.get())));
                        player.awardStat(Stats.USE_CAULDRON);
                        player.awardStat(Stats.ITEM_USED.get(item));
                        LayeredCauldronBlock.lowerFillLevel(state, level, pos);
                        level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
                        level.gameEvent(null, GameEvent.FLUID_PICKUP, pos);
                    }
                    return ItemInteractionResult.sidedSuccess(level.isClientSide);
                }));
    }
}
