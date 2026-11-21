package net.sorukoma.quasium.magic.page.spirit.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.magic.page.spirit.HookahSpirit;

public class HookahSpiritModel extends HierarchicalModel<HookahSpirit> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "hookah_spirit"), "main");
    private static final float HEAD_Y = 20f;
    private static final float STRUCK_SWELL = 1.35f;

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart leftPuff;
    private final ModelPart rightPuff;
    private final ModelPart tail;
    private int tint = -1;

    public HookahSpiritModel(ModelPart root) {
        this.root = root;
        head = root.getChild("head");
        leftPuff = head.getChild("left_puff");
        rightPuff = head.getChild("right_puff");
        tail = head.getChild("tail");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition head = mesh.getRoot().addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4f, -3.5f, -4f, 8f, 7f, 8f), PartPose.offset(0f, HEAD_Y, 0f));
        head.addOrReplaceChild("top", CubeListBuilder.create()
                .texOffs(0, 15).addBox(-2.5f, -3f, -2.5f, 5f, 3f, 5f), PartPose.offset(0.5f, -3.5f, 0.5f));
        head.addOrReplaceChild("left_puff", CubeListBuilder.create()
                .texOffs(20, 15).addBox(-1.5f, -1.5f, -1.5f, 3f, 3f, 3f), PartPose.offset(4.5f, 1f, 0.5f));
        head.addOrReplaceChild("right_puff", CubeListBuilder.create()
                .texOffs(20, 15).mirror().addBox(-1.5f, -1.5f, -1.5f, 3f, 3f, 3f), PartPose.offset(-4.5f, 1f, 0.5f));
        PartDefinition tail = head.addOrReplaceChild("tail", CubeListBuilder.create()
                .texOffs(32, 0).addBox(-2f, -2f, 0f, 4f, 4f, 4f), PartPose.offset(0f, 1f, 4f));
        tail.addOrReplaceChild("tip", CubeListBuilder.create()
                .texOffs(32, 8).addBox(-1f, -1f, 0f, 2f, 2f, 3f), PartPose.offset(0f, 0.5f, 4f));
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    ModelPart head() {
        return head;
    }

    @Override
    public void setupAnim(HookahSpirit spirit, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        tint = 0xFF000000 | spirit.color();
        head.y += Mth.sin(ageInTicks * 0.15f) * 1.0f;
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        head.xRot = headPitch * Mth.DEG_TO_RAD;
        float breath = 1f + Mth.sin(ageInTicks * 0.3f) * 0.05f;
        float swell = spirit.mood() == HookahSpirit.STRUCK ? STRUCK_SWELL : 1f;
        head.xScale = breath * swell;
        head.yScale = (2f - breath) * swell;
        head.zScale = breath * swell;
        float orbit = ageInTicks * 0.2f;
        leftPuff.y += Mth.sin(orbit) * 1.2f;
        rightPuff.y += Mth.sin(orbit + Mth.PI) * 1.2f;
        tail.yRot = Mth.sin(ageInTicks * 0.25f) * 0.4f;
        tail.xRot = Mth.cos(ageInTicks * 0.18f) * 0.15f;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        super.renderToBuffer(poseStack, buffer, packedLight, packedOverlay, FastColor.ARGB32.multiply(color, tint));
    }
}
