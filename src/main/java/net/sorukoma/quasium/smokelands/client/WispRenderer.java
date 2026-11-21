package net.sorukoma.quasium.smokelands.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.sorukoma.quasium.smokelands.SmokelandsDimension;
import net.sorukoma.quasium.smokelands.mob.DraftWisp;

final class WispRenderer extends EntityRenderer<DraftWisp> {
    private static final ResourceLocation TEXTURE = SmokelandsDimension.id("textures/entity/draft_wisp.png");
    private static final RenderType RENDER_TYPE = RenderType.entityTranslucentEmissive(TEXTURE);
    private static final float SIZE = 0.7f;

    WispRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected int getBlockLightLevel(DraftWisp wisp, BlockPos pos) {
        return 15;
    }

    @Override
    public void render(DraftWisp wisp, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int light) {
        float time = wisp.tickCount + partialTick;
        float size = SIZE * (1f + 0.12f * Mth.sin(time * 0.25f));
        poseStack.pushPose();
        poseStack.translate(0, wisp.getBbHeight() * 0.5 + 0.06 * Mth.sin(time * 0.1f), 0);
        poseStack.mulPose(entityRenderDispatcher.cameraOrientation());
        poseStack.scale(size, size, size);
        PoseStack.Pose pose = poseStack.last();
        VertexConsumer consumer = buffer.getBuffer(RENDER_TYPE);
        vertex(consumer, pose, light, -0.5f, -0.5f, 0, 1);
        vertex(consumer, pose, light, 0.5f, -0.5f, 1, 1);
        vertex(consumer, pose, light, 0.5f, 0.5f, 1, 0);
        vertex(consumer, pose, light, -0.5f, 0.5f, 0, 0);
        poseStack.popPose();
        super.render(wisp, yaw, partialTick, poseStack, buffer, light);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, int light, float x, float y, int u, int v) {
        consumer.addVertex(pose, x, y, 0f).setColor(-1).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                .setNormal(pose, 0f, 1f, 0f);
    }

    @Override
    public ResourceLocation getTextureLocation(DraftWisp wisp) {
        return TEXTURE;
    }
}
