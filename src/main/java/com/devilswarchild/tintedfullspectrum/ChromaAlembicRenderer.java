package com.devilswarchild.tintedfullspectrum;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

// Draws the Chroma Alembic's static half unrotated and the platform half spinning around its own
// pivot, driven by ChromaAlembicBlockEntity's crafting state. See chroma_alembic_full_build.md.
public class ChromaAlembicRenderer implements BlockEntityRenderer<ChromaAlembicBlockEntity> {
    public static final ModelResourceLocation STATIC_MODEL = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath(TintedFullSpectrum.MODID, "block/custom/chroma_alembic_static"));
    public static final ModelResourceLocation PLATFORM_MODEL = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath(TintedFullSpectrum.MODID, "block/custom/chroma_alembic_platform"));

    // Platform group's Blockbench pivot (8, 17, 8) out of 16 units per block.
    private static final double PLATFORM_PIVOT_Y = 17.0 / 16.0;

    public ChromaAlembicRenderer(BlockEntityRendererProvider.Context context) {
    }

    // The lens/pole rise to y=30/16 (~1.875 blocks), taller than the default 1x1x1 culling box the
    // dispatcher uses to decide whether to call render() at all -- without this, standing close and
    // looking steeply up puts that undersized box outside the frustum and the whole model vanishes.
    @Override
    public AABB getRenderBoundingBox(ChromaAlembicBlockEntity blockEntity) {
        BlockPos pos = blockEntity.getBlockPos();
        return new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1.0, pos.getY() + 2.0, pos.getZ() + 1.0);
    }

    @Override
    public void render(ChromaAlembicBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource,
            int packedLight, int packedOverlay) {
        Level level = blockEntity.getLevel();
        if (level == null) {
            return;
        }

        BlockState state = blockEntity.getBlockState();
        Minecraft mc = Minecraft.getInstance();
        ModelManager modelManager = mc.getModelManager();
        BakedModel staticModel = modelManager.getModel(STATIC_MODEL);
        BakedModel platformModel = modelManager.getModel(PLATFORM_MODEL);

        int tint = mc.getBlockColors().getColor(state, level, blockEntity.getBlockPos(), 0);
        float r = ((tint >> 16) & 0xFF) / 255f;
        float g = ((tint >> 8) & 0xFF) / 255f;
        float b = (tint & 0xFF) / 255f;

        float angle = blockEntity.getBaseAngle();
        if (blockEntity.isProcessing()) {
            float elapsed = (level.getGameTime() - blockEntity.getCraftStartGameTime()) + partialTick;
            angle += ChromaAlembicBlockEntity.computeAngle(elapsed);
        }

        VertexConsumer buffer = bufferSource.getBuffer(RenderType.translucent());
        var modelRenderer = mc.getBlockRenderer().getModelRenderer();

        poseStack.pushPose();
        Direction facing = state.getValue(ChromaAlembicBlock.FACING);
        poseStack.translate(0.5, 0, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(yRotationForFacing(facing)));
        poseStack.translate(-0.5, 0, -0.5);

        modelRenderer.renderModel(poseStack.last(), buffer, state, staticModel, r, g, b, packedLight, packedOverlay);

        poseStack.pushPose();
        poseStack.translate(0.5, PLATFORM_PIVOT_Y, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(angle));
        poseStack.translate(-0.5, -PLATFORM_PIVOT_Y, -0.5);
        modelRenderer.renderModel(poseStack.last(), buffer, state, platformModel, 1f, 1f, 1f, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.popPose();
    }

    // Mirrors the facing->y-rotation mapping used by tinted_wall_torch.json.
    private static float yRotationForFacing(Direction facing) {
        return switch (facing) {
            case SOUTH -> 180f;
            case WEST -> 270f;
            case EAST -> 90f;
            default -> 0f;
        };
    }
}
