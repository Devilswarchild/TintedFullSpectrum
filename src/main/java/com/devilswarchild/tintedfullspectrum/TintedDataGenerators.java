package com.devilswarchild.tintedfullspectrum;

import net.neoforged.neoforge.data.event.GatherDataEvent;

// Registers all datagen providers for the mod: blockstates/models for Tinted Planks' stairs/slab/
// fence variants (hand-authoring these per-shape is exactly what tintable_system_and_planks.md says
// not to do), loot tables for the whole plank family, and their crafting recipes. Run via
// `gradlew runData`; the "data" run config was already present in the MDK template.
public final class TintedDataGenerators {
    private TintedDataGenerators() {
    }

    public static void gatherData(GatherDataEvent event) {
        if (event.includeClient()) {
            event.createProvider(output -> new TintedBlockStateProvider(output, event.getExistingFileHelper()));
        }
        if (event.includeServer()) {
            event.createProvider(TintedRecipeProvider::new);
            event.createProvider(TintedLootTableProvider::new);
        }
    }
}
