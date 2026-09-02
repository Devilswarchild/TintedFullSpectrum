package com.devilswarchild.tintedfullspectrum;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

// Loot tables for Tinted Planks and its stairs/slab/fence variants. Every drop needs to carry the
// TIN_COLOR component from the block entity onto the dropped item (copy_components) -- otherwise
// breaking a dyed block would just drop a fresh, undyed one, silently losing the color.
public class TintedLootTableProvider extends LootTableProvider {
    public TintedLootTableProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, Set.of(), List.of(new SubProviderEntry(BlockLoot::new, LootContextParamSets.BLOCK)), registries);
    }

    public static class BlockLoot extends BlockLootSubProvider {
        protected BlockLoot(HolderLookup.Provider registries) {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS, registries);
        }

        private LootTable.Builder tintableDrop(Block block) {
            return LootTable.lootTable().withPool(
                    applyExplosionCondition(block, LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
                            .add(LootItem.lootTableItem(block)
                                    .apply(CopyComponentsFunction.copyComponents(CopyComponentsFunction.Source.BLOCK_ENTITY)
                                            .include(TintedFullSpectrum.TINT_COLOR.get())))));
        }

        private LootTable.Builder tintableSlabDrop(Block block) {
            return LootTable.lootTable().withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
                    .add((LootPoolEntryContainer.Builder<?>) applyExplosionDecay(block, LootItem.lootTableItem(block)
                            .apply(CopyComponentsFunction.copyComponents(CopyComponentsFunction.Source.BLOCK_ENTITY)
                                    .include(TintedFullSpectrum.TINT_COLOR.get()))
                            .apply(SetItemCountFunction.setCount(ConstantValue.exactly(2.0F))
                                    .when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(block)
                                            .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(SlabBlock.TYPE, SlabType.DOUBLE)))))));
        }

        @Override
        protected void generate() {
            add(TintedFullSpectrum.TINTED_PLANKS.get(), tintableDrop(TintedFullSpectrum.TINTED_PLANKS.get()));
            add(TintedFullSpectrum.TINTED_PLANKS_STAIRS.get(), tintableDrop(TintedFullSpectrum.TINTED_PLANKS_STAIRS.get()));
            add(TintedFullSpectrum.TINTED_PLANKS_SLAB.get(), tintableSlabDrop(TintedFullSpectrum.TINTED_PLANKS_SLAB.get()));
            add(TintedFullSpectrum.TINTED_PLANKS_FENCE.get(), tintableDrop(TintedFullSpectrum.TINTED_PLANKS_FENCE.get()));
            add(TintedFullSpectrum.TINTED_PLANKS_FENCE_GATE.get(), tintableDrop(TintedFullSpectrum.TINTED_PLANKS_FENCE_GATE.get()));
        }

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return List.of(TintedFullSpectrum.TINTED_PLANKS.get(), TintedFullSpectrum.TINTED_PLANKS_STAIRS.get(),
                    TintedFullSpectrum.TINTED_PLANKS_SLAB.get(), TintedFullSpectrum.TINTED_PLANKS_FENCE.get(),
                    TintedFullSpectrum.TINTED_PLANKS_FENCE_GATE.get());
        }
    }
}
