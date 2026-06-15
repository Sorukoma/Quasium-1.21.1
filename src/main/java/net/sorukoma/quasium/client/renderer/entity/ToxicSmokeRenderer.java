package net.sorukoma.quasium.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.entity.ToxicSmokeEntity;

public class ToxicSmokeRenderer extends EntityRenderer<ToxicSmokeEntity> {

    private static final ResourceLocation DUMMY_TEXTURE = ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "textures/entity/toxic_smoke.png");

    public ToxicSmokeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(ToxicSmokeEntity entity) {
        return DUMMY_TEXTURE;
    }

    @Override
    public void render(ToxicSmokeEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        // Не рендерим модель — только частицы спавнятся в ToxicSmokeEntity.tick()
    }
}