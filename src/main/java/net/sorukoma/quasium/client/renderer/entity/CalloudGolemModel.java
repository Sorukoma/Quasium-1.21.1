package net.sorukoma.quasium.client.renderer.entity;

import net.minecraft.world.entity.HumanoidArm;
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
import net.sorukoma.quasium.golem.CalloudGolemEntity;

public class CalloudGolemModel extends HierarchicalModel<CalloudGolemEntity> implements ArmedModel {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "calloud_golem"), "main");

    private static final float SCALE = 0.5f;
    private static final float LIFT = -2f / 16f;

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public CalloudGolemModel(ModelPart root) {
        this.root = root;
        head = root.getChild("head");
        body = root.getChild("body");
        rightArm = root.getChild("right_arm");
        leftArm = root.getChild("left_arm");
        rightLeg = root.getChild("right_leg");
        leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3.5f, -6f, -3.5f, 7f, 5f, 7f)
                .texOffs(0, 12).addBox(-4.5f, -2f, -4.5f, 9f, 2f, 9f), PartPose.offset(0f, 17f, 0f));
        root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 24).addBox(-3.5f, 0f, -2.5f, 7f, 4f, 5f), PartPose.offset(0f, 17f, 0f));
        root.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(24, 24).addBox(-1f, 0f, -1f, 2f, 4f, 2f), PartPose.offset(-4.5f, 17f, 0f));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(24, 24).mirror().addBox(-1f, 0f, -1f, 2f, 4f, 2f), PartPose.offset(4.5f, 17f, 0f));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(32, 24).addBox(-1.5f, 0f, -1.5f, 3f, 3f, 3f), PartPose.offset(-1.75f, 21f, 0f));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(32, 24).mirror().addBox(-1.5f, 0f, -1.5f, 3f, 3f, 3f), PartPose.offset(1.75f, 21f, 0f));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(CalloudGolemEntity golem, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        head.xRot = headPitch * Mth.DEG_TO_RAD * 0.5f;
        float step = Mth.cos(limbSwing * 0.6662f) * 1.4f * limbSwingAmount;
        float punch = Mth.sin(attackTime * Mth.PI) * 1.4f;
        rightLeg.xRot = step;
        leftLeg.xRot = -step;
        rightArm.xRot = -step - punch;
        leftArm.xRot = step - punch;
        if (golem.isVibing() && attackTime == 0f) GolemDance.apply(ageInTicks, head, body, rightArm, leftArm);
    }

    @Override
    public void translateToHand(HumanoidArm arm, PoseStack pose) {
        root.translateAndRotate(pose);
        (arm == HumanoidArm.RIGHT ? rightArm : leftArm).translateAndRotate(pose);
        pose.scale(SCALE, SCALE, SCALE);
        pose.translate(0f, LIFT, 0f);
    }
}
