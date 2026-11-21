package net.sorukoma.quasium.smokelands.client;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import net.sorukoma.quasium.smokelands.SmokelandsDimension;
import net.sorukoma.quasium.smokelands.sultan.SultanOfSmoke;

public class SultanModel extends HierarchicalModel<SultanOfSmoke> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(SmokelandsDimension.id("sultan_of_smoke"), "main");

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart bowl;
    private final ModelPart crown;
    private final ModelPart rightHose;
    private final ModelPart leftHose;

    public SultanModel(ModelPart root) {
        this.root = root;
        body = root.getChild("body");
        bowl = body.getChild("bowl");
        crown = bowl.getChild("crown");
        rightHose = body.getChild("right_hose");
        leftHose = body.getChild("left_hose");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition body = mesh.getRoot().addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-8, -10, -8, 16, 10, 16)
                .texOffs(64, 0).addBox(-4, -14, -4, 8, 4, 8)
                .texOffs(80, 26).addBox(-3, -30, -3, 6, 16, 6)
                .texOffs(0, 26).addBox(-10, -32, -10, 20, 2, 20), PartPose.offset(0, 24, 0));
        PartDefinition bowl = body.addOrReplaceChild("bowl", CubeListBuilder.create()
                .texOffs(0, 48).addBox(-6, -9, -6, 12, 9, 12), PartPose.offset(0, -32, 0));
        bowl.addOrReplaceChild("crown", CubeListBuilder.create()
                .texOffs(48, 48).addBox(-7, -4, -7, 14, 4, 14)
                .texOffs(104, 48).addBox(-5, -6, -4, 4, 2, 4)
                .texOffs(104, 48).addBox(1, -6, -2, 4, 2, 4)
                .texOffs(104, 48).addBox(-2, -6, 2, 4, 2, 4), PartPose.offset(0, -9, 0));
        body.addOrReplaceChild("right_hose", CubeListBuilder.create()
                .texOffs(96, 0).addBox(-2, 0, -2, 4, 18, 4)
                .texOffs(104, 26).addBox(-1.5f, 18, -1.5f, 3, 3, 3), PartPose.offset(-10, -29, 0));
        body.addOrReplaceChild("left_hose", CubeListBuilder.create()
                .texOffs(112, 0).addBox(-2, 0, -2, 4, 18, 4)
                .texOffs(104, 32).addBox(-1.5f, 18, -1.5f, 3, 3, 3), PartPose.offset(10, -29, 0));
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(SultanOfSmoke sultan, float limbSwing, float limbSwingAmount, float age, float headYaw, float headPitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        bowl.yRot = headYaw * Mth.DEG_TO_RAD * 0.6f;
        crown.yRot = sultan.flag(SultanOfSmoke.BURNT_OUT) ? 0f : age * 0.04f;
        float sway = Mth.sin(age * 0.07f);
        rightHose.xRot = sway * 0.3f;
        rightHose.zRot = 0.2f + Mth.sin(age * 0.05f) * 0.12f;
        leftHose.xRot = -sway * 0.3f;
        leftHose.zRot = -0.2f - Mth.sin(age * 0.05f + 1f) * 0.12f;
        if (sultan.flag(SultanOfSmoke.INHALING)) {
            float swell = 1f + 0.07f + 0.05f * Mth.sin(age * 0.8f);
            body.xScale = swell;
            body.zScale = swell;
            rightHose.zRot += 0.5f;
            leftHose.zRot -= 0.5f;
        }
        if (sultan.flag(SultanOfSmoke.STUNNED)) {
            body.zRot = Mth.sin(age * 0.35f) * 0.18f;
            body.xRot = Mth.cos(age * 0.27f) * 0.1f;
        }
    }
}
