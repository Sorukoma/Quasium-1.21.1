package net.sorukoma.quasium.client.renderer.entity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.golem.SmokeWardenEntity;

public class SmokeWardenRenderer extends MobRenderer<SmokeWardenEntity, SmokeWardenModel> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "textures/entity/smoke_warden.png");

    public SmokeWardenRenderer(EntityRendererProvider.Context context) {
        super(context, new SmokeWardenModel(context.bakeLayer(SmokeWardenModel.LAYER)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(SmokeWardenEntity entity) {
        return TEXTURE;
    }
}
