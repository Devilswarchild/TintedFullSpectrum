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
import net.minecraft.world.level.block.state.properties.WoodType;
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
    public static final DeferredRegister<net.minecraft.world.entity.EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, MODID);

    // A tinted twin of vanilla's own EntityType.FALLING_BLOCK, same sizing/tracking parameters --
    // needed because vanilla's FallingBlockEntity has no way to carry an arbitrary color to the
    // client while airborne (see TintedFallingBlockEntity). Only ever spawned by
    // TintedConcretePowderBlock#tick(); rendered by TintedFallingBlockRenderer.
    public static final DeferredHolder<net.minecraft.world.entity.EntityType<?>, net.minecraft.world.entity.EntityType<TintedFallingBlockEntity>> TINTED_FALLING_BLOCK = ENTITY_TYPES.register(
            "tinted_falling_block", () -> net.minecraft.world.entity.EntityType.Builder
                    .<TintedFallingBlockEntity>of(TintedFallingBlockEntity::new, net.minecraft.world.entity.MobCategory.MISC)
                    .sized(0.98F, 0.98F).clientTrackingRange(10).updateInterval(20)
                    .build("tinted_falling_block"));

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
            // Iron gets IRON_DOOR's own properties (harder, blast-resistant, metal sound, non-flammable,
            // requires a pickaxe) -- every wood material shares OAK_DOOR's properties, matching vanilla
            // where every wood door type uses the same base hardness/sound regardless of species.
            Block vanillaSource = material.equals("iron") ? Blocks.IRON_DOOR : Blocks.OAK_DOOR;
            map.put(material, BLOCKS.register("tinted_" + material + "_door",
                    () -> new TintedDoorBlock(Properties.ofFullCopy(vanillaSource))));
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

    // Tinted Terracotta: vanilla-parity, plain Terracotta only -- Glazed Terracotta is explicitly out
    // of scope, its per-color identity comes from a hand-designed directional mosaic pattern baked
    // per color, structurally incompatible with arbitrary RGB (no finite palette to ever finish
    // designing against). See tinted_full_spectrum_terracotta_handoff.md.
    public static final DeferredBlock<Block> TINTED_TERRACOTTA = BLOCKS.register("tinted_terracotta",
            () -> new TintedTerracottaBlock(Properties.ofFullCopy(Blocks.TERRACOTTA)));
    public static final DeferredItem<TintableBlockItem> TINTED_TERRACOTTA_ITEM = ITEMS.register("tinted_terracotta",
            () -> new TintableBlockItem(TINTED_TERRACOTTA.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TintedTerracottaBlockEntity>> TINTED_TERRACOTTA_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
            "tinted_terracotta", () -> BlockEntityType.Builder.of(TintedTerracottaBlockEntity::new, TINTED_TERRACOTTA.get()).build(null));

    // Tinted Sandstone family -- vanilla-parity, TWO parallel families (Sandstone / Red Sandstone,
    // matching vanilla's own real split) x 4 shapes each (base, Cut, Chiseled, Smooth). See
    // tinted_full_spectrum_sandstone_handoff.md. Each shape uses Properties.ofFullCopy from its OWN
    // exact matching vanilla block (not one shared source per family) -- Smooth in particular has a
    // genuinely different hardness (2.0/6.0) than the other three shapes (0.8), confirmed via the
    // real Blocks.java, so reusing one Properties source across a shape family would have been wrong
    // (same lesson as the Tinted Iron Door bug last session). All 8 use the generic
    // SimpleTintableBlock/SimpleTintableBlockEntity pair (see those classes) rather than 8 dedicated
    // near-identical classes -- none of these shapes have any special behavior beyond tint storage.
    public static final DeferredBlock<Block> TINTED_SANDSTONE = BLOCKS.register("tinted_sandstone",
            () -> new SimpleTintableBlock(Properties.ofFullCopy(Blocks.SANDSTONE), TintedFullSpectrum.TINTED_SANDSTONE_BLOCK_ENTITY::get));
    public static final DeferredItem<TintableBlockItem> TINTED_SANDSTONE_ITEM = ITEMS.register("tinted_sandstone",
            () -> new TintableBlockItem(TINTED_SANDSTONE.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SimpleTintableBlockEntity>> TINTED_SANDSTONE_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
            "tinted_sandstone", () -> BlockEntityType.Builder.of((pos, state) -> new SimpleTintableBlockEntity(TintedFullSpectrum.TINTED_SANDSTONE_BLOCK_ENTITY.get(), pos, state), TINTED_SANDSTONE.get()).build(null));

    public static final DeferredBlock<Block> TINTED_CUT_SANDSTONE = BLOCKS.register("tinted_cut_sandstone",
            () -> new SimpleTintableBlock(Properties.ofFullCopy(Blocks.CUT_SANDSTONE), TintedFullSpectrum.TINTED_CUT_SANDSTONE_BLOCK_ENTITY::get));
    public static final DeferredItem<TintableBlockItem> TINTED_CUT_SANDSTONE_ITEM = ITEMS.register("tinted_cut_sandstone",
            () -> new TintableBlockItem(TINTED_CUT_SANDSTONE.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SimpleTintableBlockEntity>> TINTED_CUT_SANDSTONE_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
            "tinted_cut_sandstone", () -> BlockEntityType.Builder.of((pos, state) -> new SimpleTintableBlockEntity(TintedFullSpectrum.TINTED_CUT_SANDSTONE_BLOCK_ENTITY.get(), pos, state), TINTED_CUT_SANDSTONE.get()).build(null));

    public static final DeferredBlock<Block> TINTED_CHISELED_SANDSTONE = BLOCKS.register("tinted_chiseled_sandstone",
            () -> new SimpleTintableBlock(Properties.ofFullCopy(Blocks.CHISELED_SANDSTONE), TintedFullSpectrum.TINTED_CHISELED_SANDSTONE_BLOCK_ENTITY::get));
    public static final DeferredItem<TintableBlockItem> TINTED_CHISELED_SANDSTONE_ITEM = ITEMS.register("tinted_chiseled_sandstone",
            () -> new TintableBlockItem(TINTED_CHISELED_SANDSTONE.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SimpleTintableBlockEntity>> TINTED_CHISELED_SANDSTONE_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
            "tinted_chiseled_sandstone", () -> BlockEntityType.Builder.of((pos, state) -> new SimpleTintableBlockEntity(TintedFullSpectrum.TINTED_CHISELED_SANDSTONE_BLOCK_ENTITY.get(), pos, state), TINTED_CHISELED_SANDSTONE.get()).build(null));

    public static final DeferredBlock<Block> TINTED_SMOOTH_SANDSTONE = BLOCKS.register("tinted_smooth_sandstone",
            () -> new SimpleTintableBlock(Properties.ofFullCopy(Blocks.SMOOTH_SANDSTONE), TintedFullSpectrum.TINTED_SMOOTH_SANDSTONE_BLOCK_ENTITY::get));
    public static final DeferredItem<TintableBlockItem> TINTED_SMOOTH_SANDSTONE_ITEM = ITEMS.register("tinted_smooth_sandstone",
            () -> new TintableBlockItem(TINTED_SMOOTH_SANDSTONE.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SimpleTintableBlockEntity>> TINTED_SMOOTH_SANDSTONE_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
            "tinted_smooth_sandstone", () -> BlockEntityType.Builder.of((pos, state) -> new SimpleTintableBlockEntity(TintedFullSpectrum.TINTED_SMOOTH_SANDSTONE_BLOCK_ENTITY.get(), pos, state), TINTED_SMOOTH_SANDSTONE.get()).build(null));

    public static final DeferredBlock<Block> TINTED_RED_SANDSTONE = BLOCKS.register("tinted_red_sandstone",
            () -> new SimpleTintableBlock(Properties.ofFullCopy(Blocks.RED_SANDSTONE), TintedFullSpectrum.TINTED_RED_SANDSTONE_BLOCK_ENTITY::get));
    public static final DeferredItem<TintableBlockItem> TINTED_RED_SANDSTONE_ITEM = ITEMS.register("tinted_red_sandstone",
            () -> new TintableBlockItem(TINTED_RED_SANDSTONE.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SimpleTintableBlockEntity>> TINTED_RED_SANDSTONE_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
            "tinted_red_sandstone", () -> BlockEntityType.Builder.of((pos, state) -> new SimpleTintableBlockEntity(TintedFullSpectrum.TINTED_RED_SANDSTONE_BLOCK_ENTITY.get(), pos, state), TINTED_RED_SANDSTONE.get()).build(null));

    public static final DeferredBlock<Block> TINTED_CUT_RED_SANDSTONE = BLOCKS.register("tinted_cut_red_sandstone",
            () -> new SimpleTintableBlock(Properties.ofFullCopy(Blocks.CUT_RED_SANDSTONE), TintedFullSpectrum.TINTED_CUT_RED_SANDSTONE_BLOCK_ENTITY::get));
    public static final DeferredItem<TintableBlockItem> TINTED_CUT_RED_SANDSTONE_ITEM = ITEMS.register("tinted_cut_red_sandstone",
            () -> new TintableBlockItem(TINTED_CUT_RED_SANDSTONE.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SimpleTintableBlockEntity>> TINTED_CUT_RED_SANDSTONE_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
            "tinted_cut_red_sandstone", () -> BlockEntityType.Builder.of((pos, state) -> new SimpleTintableBlockEntity(TintedFullSpectrum.TINTED_CUT_RED_SANDSTONE_BLOCK_ENTITY.get(), pos, state), TINTED_CUT_RED_SANDSTONE.get()).build(null));

    public static final DeferredBlock<Block> TINTED_CHISELED_RED_SANDSTONE = BLOCKS.register("tinted_chiseled_red_sandstone",
            () -> new SimpleTintableBlock(Properties.ofFullCopy(Blocks.CHISELED_RED_SANDSTONE), TintedFullSpectrum.TINTED_CHISELED_RED_SANDSTONE_BLOCK_ENTITY::get));
    public static final DeferredItem<TintableBlockItem> TINTED_CHISELED_RED_SANDSTONE_ITEM = ITEMS.register("tinted_chiseled_red_sandstone",
            () -> new TintableBlockItem(TINTED_CHISELED_RED_SANDSTONE.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SimpleTintableBlockEntity>> TINTED_CHISELED_RED_SANDSTONE_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
            "tinted_chiseled_red_sandstone", () -> BlockEntityType.Builder.of((pos, state) -> new SimpleTintableBlockEntity(TintedFullSpectrum.TINTED_CHISELED_RED_SANDSTONE_BLOCK_ENTITY.get(), pos, state), TINTED_CHISELED_RED_SANDSTONE.get()).build(null));

    public static final DeferredBlock<Block> TINTED_SMOOTH_RED_SANDSTONE = BLOCKS.register("tinted_smooth_red_sandstone",
            () -> new SimpleTintableBlock(Properties.ofFullCopy(Blocks.SMOOTH_RED_SANDSTONE), TintedFullSpectrum.TINTED_SMOOTH_RED_SANDSTONE_BLOCK_ENTITY::get));
    public static final DeferredItem<TintableBlockItem> TINTED_SMOOTH_RED_SANDSTONE_ITEM = ITEMS.register("tinted_smooth_red_sandstone",
            () -> new TintableBlockItem(TINTED_SMOOTH_RED_SANDSTONE.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SimpleTintableBlockEntity>> TINTED_SMOOTH_RED_SANDSTONE_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
            "tinted_smooth_red_sandstone", () -> BlockEntityType.Builder.of((pos, state) -> new SimpleTintableBlockEntity(TintedFullSpectrum.TINTED_SMOOTH_RED_SANDSTONE_BLOCK_ENTITY.get(), pos, state), TINTED_SMOOTH_RED_SANDSTONE.get()).build(null));

    // Tinted Sand / Tinted Red Sand -- gravity-affected, vanilla-parity, one class (TintedSandBlock)
    // shared by both, mirroring vanilla's own real Sand/Red Sand split (and matching the user's
    // confirmation that the ONLY genuine art difference anywhere in this whole sand+sandstone family
    // tree is Chiseled Sandstone -- both sand variants share one texture). No mineable/pickaxe tag
    // needed -- confirmed via the real Blocks.java, neither vanilla Sand nor Red Sand has
    // requiresCorrectToolForDrops() at all, unlike the Sandstone shapes above.
    public static final DeferredBlock<Block> TINTED_SAND = BLOCKS.register("tinted_sand",
            () -> new TintedSandBlock(Properties.ofFullCopy(Blocks.SAND)));
    public static final DeferredItem<TintableBlockItem> TINTED_SAND_ITEM = ITEMS.register("tinted_sand",
            () -> new TintableBlockItem(TINTED_SAND.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SimpleTintableBlockEntity>> TINTED_SAND_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
            "tinted_sand", () -> BlockEntityType.Builder.of((pos, state) -> new SimpleTintableBlockEntity(TintedFullSpectrum.TINTED_SAND_BLOCK_ENTITY.get(), pos, state), TINTED_SAND.get()).build(null));

    public static final DeferredBlock<Block> TINTED_RED_SAND = BLOCKS.register("tinted_red_sand",
            () -> new TintedSandBlock(Properties.ofFullCopy(Blocks.RED_SAND)));
    public static final DeferredItem<TintableBlockItem> TINTED_RED_SAND_ITEM = ITEMS.register("tinted_red_sand",
            () -> new TintableBlockItem(TINTED_RED_SAND.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SimpleTintableBlockEntity>> TINTED_RED_SAND_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
            "tinted_red_sand", () -> BlockEntityType.Builder.of((pos, state) -> new SimpleTintableBlockEntity(TintedFullSpectrum.TINTED_RED_SAND_BLOCK_ENTITY.get(), pos, state), TINTED_RED_SAND.get()).build(null));

    // Tinted [Red] Sandstone Stairs/Slab -- closes the dependency gap the original Sandstone handoff
    // explicitly flagged (Chiseled's real slab-stacking crafting recipe needed these to exist first).
    public static final DeferredBlock<Block> TINTED_SANDSTONE_STAIRS = BLOCKS.register("tinted_sandstone_stairs",
            () -> new SimpleTintableStairBlock(TINTED_SANDSTONE.get().defaultBlockState(), Properties.ofFullCopy(Blocks.SANDSTONE_STAIRS),
                    TintedFullSpectrum.TINTED_SANDSTONE_STAIRS_BLOCK_ENTITY::get));
    public static final DeferredItem<TintableBlockItem> TINTED_SANDSTONE_STAIRS_ITEM = ITEMS.register("tinted_sandstone_stairs",
            () -> new TintableBlockItem(TINTED_SANDSTONE_STAIRS.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SimpleTintableBlockEntity>> TINTED_SANDSTONE_STAIRS_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
            "tinted_sandstone_stairs", () -> BlockEntityType.Builder.of((pos, state) -> new SimpleTintableBlockEntity(TintedFullSpectrum.TINTED_SANDSTONE_STAIRS_BLOCK_ENTITY.get(), pos, state), TINTED_SANDSTONE_STAIRS.get()).build(null));

    public static final DeferredBlock<Block> TINTED_SANDSTONE_SLAB = BLOCKS.register("tinted_sandstone_slab",
            () -> new SimpleTintableSlabBlock(Properties.ofFullCopy(Blocks.SANDSTONE_SLAB), TintedFullSpectrum.TINTED_SANDSTONE_SLAB_BLOCK_ENTITY::get));
    public static final DeferredItem<TintableBlockItem> TINTED_SANDSTONE_SLAB_ITEM = ITEMS.register("tinted_sandstone_slab",
            () -> new TintableBlockItem(TINTED_SANDSTONE_SLAB.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SimpleTintableBlockEntity>> TINTED_SANDSTONE_SLAB_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
            "tinted_sandstone_slab", () -> BlockEntityType.Builder.of((pos, state) -> new SimpleTintableBlockEntity(TintedFullSpectrum.TINTED_SANDSTONE_SLAB_BLOCK_ENTITY.get(), pos, state), TINTED_SANDSTONE_SLAB.get()).build(null));

    public static final DeferredBlock<Block> TINTED_RED_SANDSTONE_STAIRS = BLOCKS.register("tinted_red_sandstone_stairs",
            () -> new SimpleTintableStairBlock(TINTED_RED_SANDSTONE.get().defaultBlockState(), Properties.ofFullCopy(Blocks.RED_SANDSTONE_STAIRS),
                    TintedFullSpectrum.TINTED_RED_SANDSTONE_STAIRS_BLOCK_ENTITY::get));
    public static final DeferredItem<TintableBlockItem> TINTED_RED_SANDSTONE_STAIRS_ITEM = ITEMS.register("tinted_red_sandstone_stairs",
            () -> new TintableBlockItem(TINTED_RED_SANDSTONE_STAIRS.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SimpleTintableBlockEntity>> TINTED_RED_SANDSTONE_STAIRS_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
            "tinted_red_sandstone_stairs", () -> BlockEntityType.Builder.of((pos, state) -> new SimpleTintableBlockEntity(TintedFullSpectrum.TINTED_RED_SANDSTONE_STAIRS_BLOCK_ENTITY.get(), pos, state), TINTED_RED_SANDSTONE_STAIRS.get()).build(null));

    public static final DeferredBlock<Block> TINTED_RED_SANDSTONE_SLAB = BLOCKS.register("tinted_red_sandstone_slab",
            () -> new SimpleTintableSlabBlock(Properties.ofFullCopy(Blocks.RED_SANDSTONE_SLAB), TintedFullSpectrum.TINTED_RED_SANDSTONE_SLAB_BLOCK_ENTITY::get));
    public static final DeferredItem<TintableBlockItem> TINTED_RED_SANDSTONE_SLAB_ITEM = ITEMS.register("tinted_red_sandstone_slab",
            () -> new TintableBlockItem(TINTED_RED_SANDSTONE_SLAB.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SimpleTintableBlockEntity>> TINTED_RED_SANDSTONE_SLAB_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
            "tinted_red_sandstone_slab", () -> BlockEntityType.Builder.of((pos, state) -> new SimpleTintableBlockEntity(TintedFullSpectrum.TINTED_RED_SANDSTONE_SLAB_BLOCK_ENTITY.get(), pos, state), TINTED_RED_SANDSTONE_SLAB.get()).build(null));

    // A brick-appropriate WoodType for FenceGateBlock's mandatory constructor param -- FenceGateBlock
    // has no non-wood constructor at all, but WoodType.OAK's creak doesn't fit a masonry material and
    // neither Brick Fence Gate here is made of wood. No real vanilla "stone fence gate" sound exists to
    // mirror, so this picks the closest sensible analog (a heavy door creak) instead of a wooden one --
    // used for both the plain and Tinted Brick Fence Gate.
    public static final WoodType BRICK_WOOD_TYPE = WoodType.register(new WoodType(MODID + ":brick",
            net.minecraft.world.level.block.state.properties.BlockSetType.STONE, SoundType.STONE, SoundType.STONE,
            net.minecraft.sounds.SoundEvents.IRON_DOOR_CLOSE, net.minecraft.sounds.SoundEvents.IRON_DOOR_OPEN));

    // Tinted Bricks family: the mod's first family with a full multi-stage vanilla production chain
    // (Clay Ball -> Brick -> Bricks, both real vanilla mechanics) rather than a single conversion step,
    // and its first Wall-type block. Shapes are crafted directly from the already-dyed Tinted Bricks
    // block (no per-shape dye step -- see tinted_full_spectrum_bricks_handoff.md), so all six blocks
    // share ONE block entity type, same sharing convention as Torch's floor/wall and Planks' five
    // shapes.
    public static final DeferredBlock<Block> TINTED_BRICKS = BLOCKS.register("tinted_bricks",
            () -> new SimpleTintableBlock(Properties.ofFullCopy(Blocks.BRICKS), TintedFullSpectrum.TINTED_BRICKS_BLOCK_ENTITY::get));
    public static final DeferredBlock<Block> TINTED_BRICK_SLAB = BLOCKS.register("tinted_brick_slab",
            () -> new SimpleTintableSlabBlock(Properties.ofFullCopy(Blocks.BRICK_SLAB), TintedFullSpectrum.TINTED_BRICKS_BLOCK_ENTITY::get));
    public static final DeferredBlock<Block> TINTED_BRICK_STAIRS = BLOCKS.register("tinted_brick_stairs",
            () -> new SimpleTintableStairBlock(TINTED_BRICKS.get().defaultBlockState(), Properties.ofFullCopy(Blocks.BRICK_STAIRS),
                    TintedFullSpectrum.TINTED_BRICKS_BLOCK_ENTITY::get));
    public static final DeferredBlock<Block> TINTED_BRICK_WALL = BLOCKS.register("tinted_brick_wall",
            () -> new SimpleTintableWallBlock(Properties.ofFullCopy(Blocks.BRICK_WALL), TintedFullSpectrum.TINTED_BRICKS_BLOCK_ENTITY::get));
    public static final DeferredBlock<Block> TINTED_BRICK_FENCE = BLOCKS.register("tinted_brick_fence",
            () -> new SimpleTintableFenceBlock(Properties.ofFullCopy(Blocks.BRICKS), TintedFullSpectrum.TINTED_BRICKS_BLOCK_ENTITY::get));
    // Properties copied from Blocks.BRICKS (not a vanilla fence gate) so hardness/blast resistance and
    // requiresCorrectToolForDrops match the brick material this is actually made of, not wood. The
    // crafting shape also ended up brick-family-native rather than mirroring the generic wood fence
    // gate: Tinted Bricks (block) in the middle column, Tinted Brick (item) flanking on both sides,
    // 2 rows -- the inverse arrangement from Fence's own W#W/W#W (see tinted_brick_fence_gate.json),
    // so the two recipes can't collide. Matches how Nether Brick Fence's own real properties copy
    // Nether Bricks rather than any wood fence.
    public static final DeferredBlock<Block> TINTED_BRICK_FENCE_GATE = BLOCKS.register("tinted_brick_fence_gate",
            () -> new SimpleTintableFenceGateBlock(BRICK_WOOD_TYPE, Properties.ofFullCopy(Blocks.BRICKS), TintedFullSpectrum.TINTED_BRICKS_BLOCK_ENTITY::get));

    public static final DeferredItem<TintableBlockItem> TINTED_BRICKS_ITEM = ITEMS.register("tinted_bricks",
            () -> new TintableBlockItem(TINTED_BRICKS.get(), new Item.Properties()));
    public static final DeferredItem<TintableBlockItem> TINTED_BRICK_SLAB_ITEM = ITEMS.register("tinted_brick_slab",
            () -> new TintableBlockItem(TINTED_BRICK_SLAB.get(), new Item.Properties()));
    public static final DeferredItem<TintableBlockItem> TINTED_BRICK_STAIRS_ITEM = ITEMS.register("tinted_brick_stairs",
            () -> new TintableBlockItem(TINTED_BRICK_STAIRS.get(), new Item.Properties()));
    public static final DeferredItem<TintableBlockItem> TINTED_BRICK_WALL_ITEM = ITEMS.register("tinted_brick_wall",
            () -> new TintableBlockItem(TINTED_BRICK_WALL.get(), new Item.Properties()));
    public static final DeferredItem<TintableBlockItem> TINTED_BRICK_FENCE_ITEM = ITEMS.register("tinted_brick_fence",
            () -> new TintableBlockItem(TINTED_BRICK_FENCE.get(), new Item.Properties()));
    public static final DeferredItem<TintableBlockItem> TINTED_BRICK_FENCE_GATE_ITEM = ITEMS.register("tinted_brick_fence_gate",
            () -> new TintableBlockItem(TINTED_BRICK_FENCE_GATE.get(), new Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SimpleTintableBlockEntity>> TINTED_BRICKS_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
            "tinted_bricks", () -> BlockEntityType.Builder.of((pos, state) -> new SimpleTintableBlockEntity(TintedFullSpectrum.TINTED_BRICKS_BLOCK_ENTITY.get(), pos, state),
                    TINTED_BRICKS.get(), TINTED_BRICK_SLAB.get(), TINTED_BRICK_STAIRS.get(), TINTED_BRICK_WALL.get(),
                    TINTED_BRICK_FENCE.get(), TINTED_BRICK_FENCE_GATE.get()).build(null));

    // Tinted Clay Ball / Tinted Brick: the raw-material and intermediate items feeding the Bricks
    // chain above -- both standalone registered items (not invisible intermediates), plain (non-block)
    // tintable items via TintableRawItem.
    public static final DeferredItem<TintableRawItem> TINTED_CLAY_BALL_ITEM = ITEMS.register("tinted_clay_ball",
            () -> new TintableRawItem(new Item.Properties()));
    public static final DeferredItem<TintableRawItem> TINTED_BRICK_ITEM = ITEMS.register("tinted_brick",
            () -> new TintableRawItem(new Item.Properties()));

    // Plain, untinted Brick Fence / Brick Fence Gate -- vanilla itself has neither at all (Nether Brick
    // Fence is the only non-wood fence vanilla ships), so these exist purely as genuinely new,
    // ordinary vanilla-parity content: real vanilla Bricks/Brick textures and ingredients, an ordinary
    // static crafting_shaped recipe (no dye, no custom recipe class), so they're genuinely JEI-visible
    // and discoverable even by a player who's never touched the Chroma Alembic. Distinct items from
    // Tinted Brick Fence/Fence Gate, not a substitute for them -- see brick_fence.json/
    // brick_fence_gate.json, which mirror the tinted recipes' own shapes exactly.
    public static final DeferredBlock<Block> BRICK_FENCE = BLOCKS.register("brick_fence",
            () -> new net.minecraft.world.level.block.FenceBlock(Properties.ofFullCopy(Blocks.BRICKS)));
    public static final DeferredBlock<Block> BRICK_FENCE_GATE = BLOCKS.register("brick_fence_gate",
            () -> new net.minecraft.world.level.block.FenceGateBlock(BRICK_WOOD_TYPE, Properties.ofFullCopy(Blocks.BRICKS)));
    public static final DeferredItem<BlockItem> BRICK_FENCE_ITEM = ITEMS.registerSimpleBlockItem("brick_fence", BRICK_FENCE);
    public static final DeferredItem<BlockItem> BRICK_FENCE_GATE_ITEM = ITEMS.registerSimpleBlockItem("brick_fence_gate", BRICK_FENCE_GATE);

    // Tinted Concrete -- FULL vanilla parity per explicit user request ("might as well make them
    // mirror their vanilla counterparts"): a real gravity-affected Tinted Concrete Powder that
    // hardens into this solid block on water contact, same two-block relationship as vanilla's own
    // Concrete Powder / Concrete. See TintedConcretePowderBlock for how the tint color survives every
    // transition (falling, landing, hardening in place) -- none of which vanilla's own colorless
    // powder ever needed to solve. Both blocks share ONE BlockEntityType specifically so the block
    // entity (and its color) can be preserved in place across the powder->solid conversion instead of
    // being destroyed and recreated blank.
    public static final DeferredBlock<Block> TINTED_CONCRETE = BLOCKS.register("tinted_concrete",
            () -> new TintedConcreteBlock(Properties.ofFullCopy(Blocks.WHITE_CONCRETE)));
    public static final DeferredItem<TintableBlockItem> TINTED_CONCRETE_ITEM = ITEMS.register("tinted_concrete",
            () -> new TintableBlockItem(TINTED_CONCRETE.get(), new Item.Properties()));
    public static final DeferredBlock<Block> TINTED_CONCRETE_POWDER = BLOCKS.register("tinted_concrete_powder",
            () -> new TintedConcretePowderBlock(Properties.ofFullCopy(Blocks.WHITE_CONCRETE_POWDER)));
    public static final DeferredItem<TintableBlockItem> TINTED_CONCRETE_POWDER_ITEM = ITEMS.register("tinted_concrete_powder",
            () -> new TintableBlockItem(TINTED_CONCRETE_POWDER.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TintedConcreteBlockEntity>> TINTED_CONCRETE_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
            "tinted_concrete", () -> BlockEntityType.Builder.of(TintedConcreteBlockEntity::new,
                    TINTED_CONCRETE.get(), TINTED_CONCRETE_POWDER.get()).build(null));


    // Chroma Glass Pane: vanilla-parity glass pane. Named to avoid colliding in spirit with vanilla's
    // own real minecraft:tinted_glass (the copper-frosted block).
    public static final DeferredBlock<Block> CHROMA_GLASS_PANE = BLOCKS.register("chroma_glass_pane",
            () -> new ChromaGlassPaneBlock(Properties.ofFullCopy(Blocks.GLASS_PANE)));
    public static final DeferredItem<TintableBlockItem> CHROMA_GLASS_PANE_ITEM = ITEMS.register("chroma_glass_pane",
            () -> new TintableBlockItem(CHROMA_GLASS_PANE.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ChromaGlassPaneBlockEntity>> CHROMA_GLASS_PANE_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
            "chroma_glass_pane", () -> BlockEntityType.Builder.of(ChromaGlassPaneBlockEntity::new, CHROMA_GLASS_PANE.get()).build(null));

    // Chroma Glass: the solid glass block counterpart to Chroma Glass Pane, same naming-collision
    // reasoning (avoids minecraft:tinted_glass in spirit). Extends TransparentBlock directly (plain
    // Blocks.GLASS), same scoped-simplification as the pane skipping StainedGlassBlock.
    public static final DeferredBlock<Block> CHROMA_GLASS = BLOCKS.register("chroma_glass",
            () -> new ChromaGlassBlock(Properties.ofFullCopy(Blocks.GLASS)));
    public static final DeferredItem<TintableBlockItem> CHROMA_GLASS_ITEM = ITEMS.register("chroma_glass",
            () -> new TintableBlockItem(CHROMA_GLASS.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ChromaGlassBlockEntity>> CHROMA_GLASS_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
            "chroma_glass", () -> BlockEntityType.Builder.of(ChromaGlassBlockEntity::new, CHROMA_GLASS.get()).build(null));

    // Chroma Stained Glass: the dyeable counterpart to Chroma Glass -- vanilla only ever dyes INTO
    // stained glass (plain glass is never dyed at all), so this is the block the dye recipes actually
    // target, matching vanilla's real Glass/Stained Glass split. Same TransparentBlock pattern.
    public static final DeferredBlock<Block> CHROMA_STAINED_GLASS = BLOCKS.register("chroma_stained_glass",
            () -> new ChromaStainedGlassBlock(Properties.ofFullCopy(Blocks.WHITE_STAINED_GLASS)));
    public static final DeferredItem<TintableBlockItem> CHROMA_STAINED_GLASS_ITEM = ITEMS.register("chroma_stained_glass",
            () -> new TintableBlockItem(CHROMA_STAINED_GLASS.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ChromaStainedGlassBlockEntity>> CHROMA_STAINED_GLASS_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
            "chroma_stained_glass", () -> BlockEntityType.Builder.of(ChromaStainedGlassBlockEntity::new, CHROMA_STAINED_GLASS.get()).build(null));

    // Chroma Stained Glass Pane: the dyeable counterpart to Chroma Glass Pane, same reasoning as above.
    public static final DeferredBlock<Block> CHROMA_STAINED_GLASS_PANE = BLOCKS.register("chroma_stained_glass_pane",
            () -> new ChromaGlassPaneBlock(Properties.ofFullCopy(Blocks.WHITE_STAINED_GLASS_PANE)));
    public static final DeferredItem<TintableBlockItem> CHROMA_STAINED_GLASS_PANE_ITEM = ITEMS.register("chroma_stained_glass_pane",
            () -> new TintableBlockItem(CHROMA_STAINED_GLASS_PANE.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ChromaGlassPaneBlockEntity>> CHROMA_STAINED_GLASS_PANE_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
            "chroma_stained_glass_pane", () -> BlockEntityType.Builder.of(ChromaGlassPaneBlockEntity::new, CHROMA_STAINED_GLASS_PANE.get()).build(null));

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

    // 8 glass pane + dye -> 8 Chroma Stained Glass Pane -- see ChromaGlassPaneRecipe.
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ChromaGlassPaneRecipe>> CHROMA_GLASS_PANE_SERIALIZER = RECIPE_SERIALIZERS.register(
            "chroma_glass_pane", () -> new SimpleCraftingRecipeSerializer<>(ChromaGlassPaneRecipe::new));

    // 4 sand + 4 gravel + Colored Dye -> 8 Tinted Concrete Powder, mirroring vanilla's own real
    // concrete powder recipe exactly -- see TintedConcretePowderRecipe.
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<TintedConcretePowderRecipe>> TINTED_CONCRETE_POWDER_SERIALIZER = RECIPE_SERIALIZERS.register(
            "tinted_concrete_powder", () -> new SimpleCraftingRecipeSerializer<>(TintedConcretePowderRecipe::new));

    // 1 torch + 1 iron ingot + dye -> 1 Tinted Torch, already colored -- see TintedTorchRecipe.
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<TintedTorchRecipe>> TINTED_TORCH_SERIALIZER = RECIPE_SERIALIZERS.register(
            "tinted_torch", () -> new SimpleCraftingRecipeSerializer<>(TintedTorchRecipe::new));

    // 8 glass + dye -> 8 Chroma Glass -- see ChromaGlassRecipe.
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ChromaGlassRecipe>> CHROMA_GLASS_SERIALIZER = RECIPE_SERIALIZERS.register(
            "chroma_glass", () -> new SimpleCraftingRecipeSerializer<>(ChromaGlassRecipe::new));

    // 6 Chroma Glass (same color) -> 16 Chroma Glass Pane, matching vanilla's own real
    // glass-to-pane ratio -- see ChromaGlassToPaneRecipe.
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ChromaGlassToPaneRecipe>> CHROMA_GLASS_TO_PANE_SERIALIZER = RECIPE_SERIALIZERS.register(
            "chroma_glass_to_pane", () -> new SimpleCraftingRecipeSerializer<>(ChromaGlassToPaneRecipe::new));

    // 6 Chroma Stained Glass (same color) -> 16 Chroma Stained Glass Pane, matching vanilla's own real
    // stained-glass-to-pane ratio -- see ChromaStainedGlassToPaneRecipe.
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ChromaStainedGlassToPaneRecipe>> CHROMA_STAINED_GLASS_TO_PANE_SERIALIZER = RECIPE_SERIALIZERS.register(
            "chroma_stained_glass_to_pane", () -> new SimpleCraftingRecipeSerializer<>(ChromaStainedGlassToPaneRecipe::new));

    // Tint-carrying furnace smelting (registers under vanilla's own RecipeType.SMELTING via
    // AbstractCookingRecipe) -- see TintCarryingSmeltingRecipe. Used for Tinted [Red] Sandstone ->
    // Tinted Smooth [Red] Sandstone, mirroring vanilla's real smelting-only path to Smooth.
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<TintCarryingSmeltingRecipe>> TINT_CARRYING_SMELTING_SERIALIZER = RECIPE_SERIALIZERS.register(
            "tint_carrying_smelting", () -> new net.minecraft.world.item.crafting.SimpleCookingSerializer<>(TintCarryingSmeltingRecipe::new, 200));

    // Tint-carrying stonecutting (registers under vanilla's own RecipeType.STONECUTTING via
    // StonecutterRecipe) -- see TintCarryingStonecuttingRecipe. Used for all six Tinted [Red]
    // Sandstone -> Tinted [Red] Cut/Chiseled conversions.
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<TintCarryingStonecuttingRecipe>> TINT_CARRYING_STONECUTTING_SERIALIZER = RECIPE_SERIALIZERS.register(
            "tint_carrying_stonecutting", TintCarryingStonecuttingRecipe.Serializer::new);

    // 4 Tinted [Red] Sandstone (same color) -> 4 Tinted [Red] Cut Sandstone, mirroring vanilla's own
    // real 2x2-sandstone-to-4-cut-sandstone shaped recipe -- see TintedCutSandstoneRecipe.
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<TintedCutSandstoneRecipe>> TINTED_CUT_SANDSTONE_SERIALIZER = RECIPE_SERIALIZERS.register(
            "tinted_cut_sandstone", () -> new SimpleCraftingRecipeSerializer<>(TintedCutSandstoneRecipe::new));

    // 4 Tinted [Red] Sand (same color) -> 1 Tinted [Red] Sandstone, mirroring vanilla's own real
    // 2x2-sand-to-1-sandstone shaped recipe -- see TintedSandstoneFromSandRecipe.
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<TintedSandstoneFromSandRecipe>> TINTED_SANDSTONE_FROM_SAND_SERIALIZER = RECIPE_SERIALIZERS.register(
            "tinted_sandstone_from_sand", () -> new SimpleCraftingRecipeSerializer<>(TintedSandstoneFromSandRecipe::new));

    // 6 of [Tinted [Red] Sandstone/Cut/Chiseled] (same color) -> 4 Tinted [Red] Sandstone Stairs,
    // mirroring vanilla's own real sandstone_stairs.json -- see TintedSandstoneStairsRecipe.
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<TintedSandstoneStairsRecipe>> TINTED_SANDSTONE_STAIRS_SERIALIZER = RECIPE_SERIALIZERS.register(
            "tinted_sandstone_stairs", () -> new SimpleCraftingRecipeSerializer<>(TintedSandstoneStairsRecipe::new));

    // 3 of [Tinted [Red] Sandstone/Chiseled] (same color) -> 6 Tinted [Red] Sandstone Slab, mirroring
    // vanilla's own real sandstone_slab.json -- see TintedSandstoneSlabRecipe.
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<TintedSandstoneSlabRecipe>> TINTED_SANDSTONE_SLAB_SERIALIZER = RECIPE_SERIALIZERS.register(
            "tinted_sandstone_slab", () -> new SimpleCraftingRecipeSerializer<>(TintedSandstoneSlabRecipe::new));

    // 2 Tinted [Red] Sandstone Slab (same color) -> 1 Tinted [Red] Chiseled Sandstone, mirroring
    // vanilla's own real chiseled_sandstone.json -- see TintedChiseledSandstoneFromSlabRecipe. Closes
    // the dependency gap the Sandstone handoff flagged (needed Slabs to exist first).
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<TintedChiseledSandstoneFromSlabRecipe>> TINTED_CHISELED_SANDSTONE_FROM_SLAB_SERIALIZER = RECIPE_SERIALIZERS.register(
            "tinted_chiseled_sandstone_from_slab", () -> new SimpleCraftingRecipeSerializer<>(TintedChiseledSandstoneFromSlabRecipe::new));

    // Stage 0 -- 1 Clay Ball + Colored Dye (or the Mud + Gravel + Colored Dye fallback) -> 4 Tinted
    // Clay Ball -- see TintedClayBallRecipe.
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<TintedClayBallRecipe>> TINTED_CLAY_BALL_SERIALIZER = RECIPE_SERIALIZERS.register(
            "tinted_clay_ball", () -> new SimpleCraftingRecipeSerializer<>(TintedClayBallRecipe::new));

    // Real shaped-crafting recipes with an exact-color-match requirement -- Stage 2 assembly, Slab/
    // Stairs/Wall crafting, Fence, and Fence Gate all register under this one serializer, with each
    // recipe's actual pattern/key/result living in its own JSON (see TintCarryingShapedRecipe; fixes
    // the wall/stairs collision bug the earlier per-shape CustomRecipe classes had, since those only
    // checked ingredient COUNT, not real position).
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<TintCarryingShapedRecipe>> TINT_CARRYING_SHAPED_SERIALIZER = RECIPE_SERIALIZERS.register(
            "tint_carrying_shaped", TintCarryingShapedRecipe.Serializer::new);


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
                output.accept(BRICK_FENCE_ITEM.get());
                output.accept(BRICK_FENCE_GATE_ITEM.get());
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
        ENTITY_TYPES.register(modEventBus);

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
