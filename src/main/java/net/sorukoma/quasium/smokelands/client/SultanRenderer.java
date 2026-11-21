package net.sorukoma.quasium.smokelands.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.sorukoma.quasium.smokelands.SmokelandsDimension;
import net.sorukoma.quasium.smokelands.sultan.SultanOfSmoke;

final class SultanRenderer extends MobRenderer<SultanOfSmoke, SultanModel> {
    private static final ResourceLocation TEXTURE = SmokelandsDimension.id("textures/entity/sultan_of_smoke.png");
    private static final RenderType GLOW = RenderType.eyes(SmokelandsDimension.id("textures/entity/sultan_of_smoke_glow.png"));
    private static final float SCALE = 1.4f;

    SultanRenderer(EntityRendererProvider.Context context) {
        super(context, new SultanModel(context.bakeLayer(SultanModel.LAYER)), 1.4f);
        addLayer(new Glow(this));
    }

    @Override
    protected void scale(SultanOfSmoke sultan, PoseStack poseStack, float partialTick) {
        poseStack.scale(SCALE, SCALE, SCALE);
    }

    @Override
    public ResourceLocation getTextureLocation(SultanOfSmoke sultan) {
        return TEXTURE;
    }

    private static final class Glow extends RenderLayer<SultanOfSmoke, SultanModel> {
        private Glow(RenderLayerParent<SultanOfSmoke, SultanModel> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffer, int light, SultanOfSmoke sultan, float limbSwing,
                           float limbSwingAmount, float partialTick, float age, float headYaw, float headPitch) {
            if (sultan.flag(SultanOfSmoke.BURNT_OUT)) return;
            getParentModel().renderToBuffer(poseStack, buffer.getBuffer(GLOW), 15728640, OverlayTexture.NO_OVERLAY);
        }
    }
}
