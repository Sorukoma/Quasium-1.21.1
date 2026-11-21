package net.sorukoma.quasium.magic.page.spirit.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.client.ClientKaifData;
import net.sorukoma.quasium.magic.page.PageConfig;
import net.sorukoma.quasium.magic.page.spirit.HookahSpirit;
import org.jetbrains.annotations.Nullable;

public class HookahSpiritRenderer extends MobRenderer<HookahSpirit, HookahSpiritModel> {
    private static final ResourceLocation BODY = texture("hookah_spirit");
    private static final ResourceLocation FACES = texture("hookah_spirit_faces");
    private static final int BLINK_EVERY_TICKS = 64;
    private static final int BLINK_TICKS = 3;

    public HookahSpiritRenderer(EntityRendererProvider.Context context) {
        super(context, new HookahSpiritModel(context.bakeLayer(HookahSpiritModel.LAYER)), 0.15f);
        addLayer(new FaceLayer(this, new SpiritFaces(context.bakeLayer(SpiritFaces.LAYER))));
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "textures/entity/" + name + ".png");
    }

    @Override
    public ResourceLocation getTextureLocation(HookahSpirit spirit) {
        return BODY;
    }

    @Nullable
    @Override
    protected RenderType getRenderType(HookahSpirit spirit, boolean bodyVisible, boolean translucent, boolean glowing) {
        return bodyVisible || translucent ? RenderType.entityTranslucent(BODY) : null;
    }

    private static final class FaceLayer extends RenderLayer<HookahSpirit, HookahSpiritModel> {
        private final SpiritFaces faces;

        private FaceLayer(RenderLayerParent<HookahSpirit, HookahSpiritModel> parent, SpiritFaces faces) {
            super(parent);
            this.faces = faces;
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffers, int light, HookahSpirit spirit, float limbSwing,
                           float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            if (spirit.isInvisible() || ClientKaifData.getKaifValue() < PageConfig.SPIRIT_FACE_KAIF.getAsInt()) return;
            boolean blinking = (spirit.tickCount + spirit.getId() * 13) % BLINK_EVERY_TICKS < BLINK_TICKS;
            String face = SpiritFaces.name(spirit.face(), spirit.mood(), blinking);
            faces.render(getParentModel().head(), face, poseStack, buffers.getBuffer(RenderType.entityTranslucent(FACES)), light,
                    OverlayTexture.NO_OVERLAY);
        }
    }
}
