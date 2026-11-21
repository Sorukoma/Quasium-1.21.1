package net.sorukoma.quasium.client.renderer.entity;

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
import net.sorukoma.quasium.golem.SmokeWardenEntity;

public class SmokeWardenModel extends HierarchicalModel<SmokeWardenEntity> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "smoke_warden"), "main");

    private final ModelPart root;
    private final ModelPart head;

    public SmokeWardenModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition parts = mesh.getRoot();
        parts.addOrReplaceChild("base", CubeListBuilder.create().texOffs(0, 0).addBox(-7, -4, -7, 14, 4, 14),
                PartPose.offset(0, 24, 0));
        parts.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 18).addBox(-5, -6, -5, 10, 6, 10),
                PartPose.offset(0, 20, 0));
        parts.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(0, 34).addBox(-4, -3, -4, 8, 3, 8)
                        .texOffs(40, 18).addBox(-1.5f, -9, -1.5f, 3, 6, 3)
                        .texOffs(0, 45).addBox(-3, -6, -3, 6, 1, 6)
                        .texOffs(32, 34).addBox(-2, -11, -6, 4, 3, 6),
                PartPose.offset(0, 14, 0));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(SmokeWardenEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        head.xRot = headPitch * Mth.DEG_TO_RAD;
    }
}
