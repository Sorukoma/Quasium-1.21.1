package net.sorukoma.quasium.client.renderer.entity;

import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.sorukoma.quasium.golem.CalloudGolemEntity;

public class CalloudGolemRenderer extends WorkerGolemRenderer<CalloudGolemEntity, CalloudGolemModel> {
    private static final ResourceLocation[] GLOWS = faces("calloud_golem_glow");

    public CalloudGolemRenderer(EntityRendererProvider.Context context) {
        super(context, new CalloudGolemModel(context.bakeLayer(CalloudGolemModel.LAYER)), 0.35f, "calloud_golem");
        addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
        addLayer(new RenderLayer<>(this) {
            @Override
            public void render(PoseStack pose, MultiBufferSource buffers, int light, CalloudGolemEntity golem, float limbSwing,
                               float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
                getParentModel().renderToBuffer(pose, buffers.getBuffer(RenderType.eyes(face(GLOWS, golem))),
                        LightTexture.FULL_SKY, OverlayTexture.NO_OVERLAY);
            }
        });
    }
}
