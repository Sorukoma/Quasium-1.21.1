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
import net.sorukoma.quasium.ecology.DymnikEntity;

public class DymnikModel extends HierarchicalModel<DymnikEntity> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "dymnik"), "main");

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart tail;
    private final ModelPart leftPuff;
    private final ModelPart rightPuff;

    public DymnikModel(ModelPart root) {
        this.root = root;
        body = root.getChild("body");
        head = body.getChild("head");
        tail = body.getChild("wisp").getChild("tail");
        leftPuff = body.getChild("left_puff");
        rightPuff = body.getChild("right_puff");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition body = mesh.getRoot().addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0f, 16f, 0f));
        body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3f, -6f, -3f, 6f, 6f, 6f), PartPose.ZERO);
        PartDefinition wisp = body.addOrReplaceChild("wisp", CubeListBuilder.create()
                .texOffs(0, 12).addBox(-2f, 0f, -2f, 4f, 4f, 4f), PartPose.ZERO);
        wisp.addOrReplaceChild("tail", CubeListBuilder.create()
                .texOffs(16, 12).addBox(-1f, 0f, -1f, 2f, 3f, 2f), PartPose.offset(0f, 4f, 0f));
        body.addOrReplaceChild("left_puff", CubeListBuilder.create()
                .texOffs(0, 20).addBox(-1f, -1f, -1f, 2f, 2f, 2f), PartPose.offset(4.5f, 1f, 0f));
        body.addOrReplaceChild("right_puff", CubeListBuilder.create()
                .texOffs(0, 20).mirror().addBox(-1f, -1f, -1f, 2f, 2f, 2f), PartPose.offset(-4.5f, 1f, 0f));
        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(DymnikEntity dymnik, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        body.y += Mth.sin(ageInTicks * 0.12f) * 1.2f;
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        head.xRot = headPitch * Mth.DEG_TO_RAD * 0.5f;
        tail.zRot = Mth.sin(ageInTicks * 0.15f) * 0.35f;
        tail.xRot = Mth.cos(ageInTicks * 0.11f) * 0.12f;
        float orbit = ageInTicks * 0.1f;
        leftPuff.y += Mth.sin(orbit) * 1.5f;
        rightPuff.y += Mth.sin(orbit + Mth.PI) * 1.5f;
        leftPuff.z += Mth.cos(orbit) * 1.2f;
        rightPuff.z += Mth.cos(orbit + Mth.PI) * 1.2f;
    }
}
