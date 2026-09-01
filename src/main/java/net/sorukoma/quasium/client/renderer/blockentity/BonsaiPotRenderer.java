package net.sorukoma.quasium.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.sorukoma.quasium.block.entity.BonsaiPotBlockEntity;

public class BonsaiPotRenderer implements BlockEntityRenderer<BonsaiPotBlockEntity> {
    public BonsaiPotRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(BonsaiPotBlockEntity entity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int combinedLight, int combinedOverlay) {
        if (entity.getTreeType().isEmpty()) return;

        float growthScale = 0.35F + entity.getGrowth() / 100.0F * 0.65F;
        BlockState leaves = switch (entity.getTreeType()) {
            case "grape" -> Blocks.AZALEA_LEAVES.defaultBlockState();
            case "lavender" -> Blocks.FLOWERING_AZALEA_LEAVES.defaultBlockState();
            case "cocoa", "coconut" -> Blocks.JUNGLE_LEAVES.defaultBlockState();
            default -> Blocks.OAK_LEAVES.defaultBlockState();
        };

        poseStack.pushPose();
        poseStack.translate(0.5, 0.31, 0.5);
        poseStack.scale(growthScale, growthScale, growthScale);
        renderBlock(Blocks.OAK_LOG.defaultBlockState(), poseStack, bufferSource, combinedLight, combinedOverlay,
                -0.08F, 0.0F, -0.08F, 0.16F, 0.65F, 0.16F);
        renderBlock(leaves, poseStack, bufferSource, combinedLight, combinedOverlay,
                -0.42F, 0.48F, -0.30F, 0.58F, 0.42F, 0.58F);
        renderBlock(leaves, poseStack, bufferSource, combinedLight, combinedOverlay,
                -0.12F, 0.62F, -0.38F, 0.58F, 0.42F, 0.58F);
        renderBlock(leaves, poseStack, bufferSource, combinedLight, combinedOverlay,
                -0.20F, 0.73F, -0.08F, 0.58F, 0.42F, 0.58F);
        poseStack.popPose();
    }

    private static void renderBlock(BlockState state, PoseStack poseStack, MultiBufferSource bufferSource,
                                    int combinedLight, int combinedOverlay, float x, float y, float z,
                                    float width, float height, float depth) {
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.scale(width, height, depth);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                state, poseStack, bufferSource, combinedLight, combinedOverlay);
        poseStack.popPose();
    }
}
