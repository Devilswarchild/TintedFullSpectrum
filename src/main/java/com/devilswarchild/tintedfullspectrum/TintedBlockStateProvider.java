package com.devilswarchild.tintedfullspectrum;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

// Generates blockstates/models for Tinted Planks' base cube and slab -- fully tinted, since
// vanilla's own "block/leaves" template is a full cube with tintindex 0 on every face (exactly what
// a tinted cube_all needs) and the slab's bottom/top halves are simple enough to hand-build the
// same way.
//
// Stairs, fence, and fence gate are NOT generated here -- they're hand-authored static resources
// instead (models/block/tinted_planks_stairs*.json etc., blockstates/tinted_planks_stairs.json
// etc.), because fully tinting their geometry needs custom per-face tintindex that NeoForge's
// stairsBlock/fenceBlock/fenceGateBlock datagen helpers can't produce (they always reference
// vanilla's own untinted templates). The hand-authored versions reuse vanilla's exact geometry and
// blockstate variant/rotation structure (the shape-independent part), just with tintindex added and
// pointed at custom tinted parent models under models/block/custom/tinted_*.json.
public class TintedBlockStateProvider extends BlockStateProvider {
    public TintedBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, TintedFullSpectrum.MODID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(TintedFullSpectrum.MODID, "block/white_planks");

        Block planks = TintedFullSpectrum.TINTED_PLANKS.get();
        ModelFile planksModel = models().withExistingParent(blockName(planks), mcLoc("block/leaves")).texture("all", texture);
        simpleBlockWithItem(planks, planksModel);

        SlabBlock slab = (SlabBlock) TintedFullSpectrum.TINTED_PLANKS_SLAB.get();
        ModelFile blockBase = models().getExistingFile(mcLoc("block/block"));
        ModelFile slabBottom = models().getBuilder(blockName(slab))
                .parent(blockBase)
                .texture("all", texture).texture("particle", texture)
                .element().from(0, 0, 0).to(16, 8, 16).allFaces((direction, face) -> face.texture("#all").tintindex(0)).end();
        ModelFile slabTop = models().getBuilder(blockName(slab) + "_top")
                .parent(blockBase)
                .texture("all", texture).texture("particle", texture)
                .element().from(0, 8, 0).to(16, 16, 16).allFaces((direction, face) -> face.texture("#all").tintindex(0)).end();
        slabBlock(slab, slabBottom, slabTop, planksModel);
        simpleBlockItem(slab, slabBottom);
    }

    private static String blockName(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).getPath();
    }
}
