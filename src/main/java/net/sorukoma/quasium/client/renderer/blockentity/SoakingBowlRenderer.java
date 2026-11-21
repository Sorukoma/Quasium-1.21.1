package net.sorukoma.quasium.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.sorukoma.quasium.block.entity.SoakingBowlBlockEntity;
import net.sorukoma.quasium.item.ModItems;

public class SoakingBowlRenderer implements BlockEntityRenderer<SoakingBowlBlockEntity> {

    public SoakingBowlRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(SoakingBowlBlockEntity entity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int combinedLight, int combinedOverlay) {
        int grams = entity.getTobaccoGrams();
        if (grams <= 0) return;

        poseStack.pushPose();
        poseStack.translate(0.5, 1 / 16.0 + 0.01 + Math.min(grams, 20) * 0.0075, 0.5);
        poseStack.mulPose(Axis.XP.rotationDegrees(90f));
        float scale = 0.45f;
        poseStack.scale(scale, scale, scale);

        ItemStack item = new ItemStack(ModItems.TOBACCO_LEAVES.get());

        Minecraft.getInstance().getItemRenderer().renderStatic(
                item, ItemDisplayContext.FIXED, combinedLight, combinedOverlay,
                poseStack, bufferSource, entity.getLevel(), 0);

        poseStack.popPose();
    }
}
