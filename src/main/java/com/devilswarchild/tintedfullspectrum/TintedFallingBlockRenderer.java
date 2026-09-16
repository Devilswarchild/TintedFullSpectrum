package com.devilswarchild.tintedfullspectrum;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.RenderTypeHelper;
import net.neoforged.neoforge.client.model.data.ModelData;

// Copy of vanilla's own FallingBlockRenderer, modified in exactly one place: every VertexConsumer the
// block mesh is written into is wrapped in a TintingVertexConsumer carrying this entity's own synced
// color, so the falling block renders in the correct tint instead of the neutral-gray fallback
// BlockColors produces when there's no real block entity at the falling entity's position to read
// from (see TintedFallingBlockEntity/TintingVertexConsumer for the rest of the mechanism).
public class TintedFallingBlockRenderer extends EntityRenderer<TintedFallingBlockEntity> {
    private final BlockRenderDispatcher dispatcher;

    public TintedFallingBlockRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.5F;
        this.dispatcher = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(TintedFallingBlockEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
            MultiBufferSource buffer, int packedLight) {
        BlockState blockstate = entity.getBlockState();
        if (blockstate.getRenderShape() == RenderShape.MODEL) {
            Level level = entity.level();
            if (blockstate != level.getBlockState(entity.blockPosition()) && blockstate.getRenderShape() != RenderShape.INVISIBLE) {
                poseStack.pushPose();
                BlockPos blockpos = BlockPos.containing(entity.getX(), entity.getBoundingBox().maxY, entity.getZ());
                poseStack.translate(-0.5, 0.0, -0.5);
                BakedModel model = this.dispatcher.getBlockModel(blockstate);
                int color = entity.getColor();
                long seed = blockstate.getSeed(entity.getStartPos());
                for (RenderType renderType : model.getRenderTypes(blockstate, RandomSource.create(seed), ModelData.EMPTY)) {
                    VertexConsumer rawConsumer = buffer.getBuffer(RenderTypeHelper.getMovingBlockRenderType(renderType));
                    VertexConsumer tintedConsumer = new TintingVertexConsumer(rawConsumer, color);
                    this.dispatcher.getModelRenderer().tesselateBlock(
                            level, model, blockstate, blockpos, poseStack, tintedConsumer, false, RandomSource.create(),
                            seed, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, renderType);
                }
                poseStack.popPose();
                super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
            }
        }
    }

    @Override
    public ResourceLocation getTextureLocation(TintedFallingBlockEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
