package net.sorukoma.quasium.client.renderer.entity;

import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Pose;
import net.minecraft.client.model.ArmedModel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.golem.ClayGolemEntity;
import net.sorukoma.quasium.golem.GolemPipe;
import net.sorukoma.quasium.golem.SmokeMarks;

public class ClayGolemModel extends HierarchicalModel<ClayGolemEntity> implements ArmedModel {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "clay_golem"), "main");

    private static final float SCALE = 0.75f;
    private static final float LIFT = -1f / 16f;
    private static final float SIT_DROP = 5f;
    private static final float LEG_SPLAY = 0.3f;
    private static final float PIPE_PITCH = 1.9f;
    private static final float PIPE_YAW = 0.6f;
    private static final float FIST_X = 1.5f / 16f;
    private static final float FIST_Y = 7f / 16f;

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public ClayGolemModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition parts = mesh.getRoot();
        parts.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4f, -6f, -3.5f, 8, 6, 7)
                        .texOffs(46, 0).addBox(-1f, -3f, -4.5f, 2, 2, 1),
                PartPose.offset(0f, 11f, 0f));
        PartDefinition body = parts.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(0, 13).addBox(-5.5f, -8f, -3.5f, 11, 8, 7),
                PartPose.offset(0f, 19f, 0f));
        body.addOrReplaceChild("flask", CubeListBuilder.create()
                        .texOffs(32, 28).addBox(-2.5f, -6f, 3.5f, 5, 5, 4)
                        .texOffs(50, 28).addBox(-1f, -9f, 4.5f, 2, 3, 2)
                        .texOffs(30, 0).addBox(-2f, -11f, 3.5f, 4, 2, 4),
                PartPose.ZERO);
        parts.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(36, 13).addBox(-3f, -1f, -1.5f, 3, 8, 3),
                PartPose.offset(-5.5f, 12f, 0f));
        parts.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(48, 13).addBox(0f, -1f, -1.5f, 3, 8, 3),
                PartPose.offset(5.5f, 12f, 0f));
        parts.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 28).addBox(-2f, 0f, -2f, 4, 5, 4),
                PartPose.offset(-2.5f, 19f, 0f));
        parts.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(16, 28).addBox(-2f, 0f, -2f, 4, 5, 4),
                PartPose.offset(2.5f, 19f, 0f));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(ClayGolemEntity golem, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        head.xRot = headPitch * Mth.DEG_TO_RAD;
        float stride = Mth.cos(limbSwing * 0.6662f) * 1.2f * limbSwingAmount;
        rightLeg.xRot = stride;
        leftLeg.xRot = -stride;
        float chop = Mth.sin(attackTime * Mth.PI) * 1.3f;
        rightArm.xRot = -stride * 0.8f - chop;
        leftArm.xRot = stride * 0.8f - chop;
        if (golem.getPose() == Pose.SITTING) {
            root.y = SIT_DROP;
            rightLeg.xRot = leftLeg.xRot = -Mth.HALF_PI;
            rightLeg.yRot = LEG_SPLAY;
            leftLeg.yRot = -LEG_SPLAY;
        }
        if (golem.isVibing() && attackTime == 0f) GolemDance.apply(ageInTicks, head, body, rightArm, leftArm);
        GolemPipe pipe = golem.getExistingDataOrNull(SmokeMarks.PIPE);
        if (pipe != null) raisePipe(pipe.raise(golem.level().getGameTime(), ageInTicks - golem.tickCount), golem.getMainArm());
    }

    private void raisePipe(float raise, HumanoidArm side) {
        if (raise <= 0f) return;
        boolean right = side == HumanoidArm.RIGHT;
        ModelPart arm = right ? rightArm : leftArm;
        arm.xRot = Mth.lerp(raise, arm.xRot, head.xRot - PIPE_PITCH);
        arm.yRot = Mth.lerp(raise, arm.yRot, head.yRot + (right ? -PIPE_YAW : PIPE_YAW));
        arm.zRot = Mth.lerp(raise, arm.zRot, 0f);
    }

    void translateToFist(HumanoidArm side, PoseStack pose) {
        root.translateAndRotate(pose);
        (side == HumanoidArm.RIGHT ? rightArm : leftArm).translateAndRotate(pose);
        pose.translate(side == HumanoidArm.RIGHT ? -FIST_X : FIST_X, FIST_Y, 0f);
    }

    @Override
    public void translateToHand(HumanoidArm arm, PoseStack pose) {
        root.translateAndRotate(pose);
        (arm == HumanoidArm.RIGHT ? rightArm : leftArm).translateAndRotate(pose);
        pose.scale(SCALE, SCALE, SCALE);
        pose.translate(0f, LIFT, 0f);
    }
}
