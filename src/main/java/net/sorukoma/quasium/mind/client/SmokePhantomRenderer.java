package net.sorukoma.quasium.mind.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.mind.overdose.SmokePhantom;

@OnlyIn(Dist.CLIENT)
public class SmokePhantomRenderer extends MobRenderer<SmokePhantom, SmokePhantomRenderer.Model> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "smoke_phantom"), "main");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "textures/entity/smoke_phantom.png");
    private static final float DEGREES = Mth.DEG_TO_RAD;

    public SmokePhantomRenderer(EntityRendererProvider.Context context) {
        super(context, new Model(context.bakeLayer(LAYER)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(SmokePhantom phantom) {
        return TEXTURE;
    }

    @Override
    protected RenderType getRenderType(SmokePhantom phantom, boolean bodyVisible, boolean translucent, boolean glowing) {
        return RenderType.entityTranslucent(TEXTURE);
    }

    @Override
    protected void scale(SmokePhantom phantom, PoseStack poseStack, float partialTick) {
        poseStack.translate(0f, 1.3125f, 0.1875f);
    }

    @Override
    protected void setupRotations(SmokePhantom phantom, PoseStack poseStack, float bob, float yaw, float partialTick, float scale) {
        super.setupRotations(phantom, poseStack, bob, yaw, partialTick, scale);
        poseStack.mulPose(Axis.XP.rotationDegrees(phantom.getXRot()));
    }

    @OnlyIn(Dist.CLIENT)
    public static class Model extends HierarchicalModel<SmokePhantom> {
        private final ModelPart root;
        private final ModelPart leftWingBase;
        private final ModelPart leftWingTip;
        private final ModelPart rightWingBase;
        private final ModelPart rightWingTip;
        private final ModelPart tailBase;
        private final ModelPart tailTip;

        public Model(ModelPart root) {
            this.root = root;
            ModelPart body = root.getChild("body");
            tailBase = body.getChild("tail_base");
            tailTip = tailBase.getChild("tail_tip");
            leftWingBase = body.getChild("left_wing_base");
            leftWingTip = leftWingBase.getChild("left_wing_tip");
            rightWingBase = body.getChild("right_wing_base");
            rightWingTip = rightWingBase.getChild("right_wing_tip");
        }

        @Override
        public ModelPart root() {
            return root;
        }

        @Override
        public void setupAnim(SmokePhantom phantom, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
            float flap = (phantom.getId() * 3 + ageInTicks) * 7.448451f * DEGREES;
            leftWingBase.zRot = Mth.cos(flap) * 16f * DEGREES;
            leftWingTip.zRot = Mth.cos(flap) * 16f * DEGREES;
            rightWingBase.zRot = -leftWingBase.zRot;
            rightWingTip.zRot = -leftWingTip.zRot;
            tailBase.xRot = -(5f + Mth.cos(flap * 2f) * 5f) * DEGREES;
            tailTip.xRot = tailBase.xRot;
        }
    }
}
