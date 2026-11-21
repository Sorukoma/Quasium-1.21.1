package net.sorukoma.quasium.ecology.client;

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
import net.sorukoma.quasium.ecology.GarevshchikEntity;

public class GarevshchikModel extends HierarchicalModel<GarevshchikEntity> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "garevshchik"), "main");

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public GarevshchikModel(ModelPart root) {
        this.root = root;
        head = root.getChild("head");
        rightArm = root.getChild("right_arm");
        leftArm = root.getChild("left_arm");
        rightLeg = root.getChild("right_leg");
        leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4f, -8f, -4f, 8f, 8f, 8f), PartPose.offset(0f, -1f, 0f));
        root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 16).addBox(-4f, 0f, -2f, 8f, 10f, 4f), PartPose.offset(0f, -1f, 0f));
        root.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(24, 16).addBox(-2f, -1f, -1.5f, 3f, 11f, 3f), PartPose.offset(-5f, 0f, 0f));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(24, 16).mirror().addBox(-1f, -1f, -1.5f, 3f, 11f, 3f), PartPose.offset(5f, 0f, 0f));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(36, 16).addBox(-1.5f, 0f, -1.5f, 3f, 14f, 3f), PartPose.offset(-2f, 10f, 0f));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(36, 16).mirror().addBox(-1.5f, 0f, -1.5f, 3f, 14f, 3f), PartPose.offset(2f, 10f, 0f));
        return LayerDefinition.create(mesh, 48, 48);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(GarevshchikEntity garevshchik, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        head.xRot = headPitch * Mth.DEG_TO_RAD;
        float step = Mth.cos(limbSwing * 0.6662f) * 1.2f * limbSwingAmount;
        rightLeg.xRot = step;
        leftLeg.xRot = -step;
        float smolder = Mth.sin(ageInTicks * 0.09f) * 0.06f;
        rightArm.xRot = -step * 0.6f + smolder;
        leftArm.xRot = step * 0.6f - smolder;
        rightArm.zRot = 0.08f;
        leftArm.zRot = -0.08f;
        if (attackTime > 0f) {
            float swing = Mth.sin(attackTime * Mth.PI);
            rightArm.xRot -= swing * 1.6f;
            leftArm.xRot -= swing * 1.6f;
        }
    }
}
