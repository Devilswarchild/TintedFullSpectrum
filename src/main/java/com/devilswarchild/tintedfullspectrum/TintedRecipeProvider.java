package com.devilswarchild.tintedfullspectrum;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.world.item.crafting.Ingredient;

// Generates the standard "planks -> stairs/slab/fence" shaped recipes for Tinted Planks, mirroring
// vanilla's own recipes for every wood type. The torch recipe and the generic recolor recipe are
// simple enough to be hand-placed data/tinted_full_spectrum/recipe/*.json files instead (see
// tintable_system_and_planks.md), not generated here.
public class TintedRecipeProvider extends RecipeProvider {
    public TintedRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput recipeOutput) {
        Ingredient planks = Ingredient.of(TintedFullSpectrum.TINTED_PLANKS_ITEM.get());
        stairBuilder(TintedFullSpectrum.TINTED_PLANKS_STAIRS_ITEM.get(), planks)
                .unlockedBy(getHasName(TintedFullSpectrum.TINTED_PLANKS_ITEM.get()), has(TintedFullSpectrum.TINTED_PLANKS_ITEM.get()))
                .save(recipeOutput);
        slab(recipeOutput, RecipeCategory.BUILDING_BLOCKS, TintedFullSpectrum.TINTED_PLANKS_SLAB_ITEM.get(), TintedFullSpectrum.TINTED_PLANKS_ITEM.get());
        fenceBuilder(TintedFullSpectrum.TINTED_PLANKS_FENCE_ITEM.get(), planks)
                .unlockedBy(getHasName(TintedFullSpectrum.TINTED_PLANKS_ITEM.get()), has(TintedFullSpectrum.TINTED_PLANKS_ITEM.get()))
                .save(recipeOutput);
        fenceGateBuilder(TintedFullSpectrum.TINTED_PLANKS_FENCE_GATE_ITEM.get(), planks)
                .unlockedBy(getHasName(TintedFullSpectrum.TINTED_PLANKS_ITEM.get()), has(TintedFullSpectrum.TINTED_PLANKS_ITEM.get()))
                .save(recipeOutput);
    }
}
