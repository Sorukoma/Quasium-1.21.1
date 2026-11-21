package net.sorukoma.quasium.magic.page.spirit.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
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
import net.sorukoma.quasium.magic.page.spirit.HookahSpirit;

final class SpiritFaces {
    static final ModelLayerLocation LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "hookah_spirit"), "faces");
    private static final String[] NOTES = {"plain", "menthol", "ember", "nectar", "dew", "resin", "ash"};
    private static final String[] LOOKS = {"open", "blink", "squint", "struck"};
    private static final String SLEEP = "menthol_sleep";
    private static final int CELL_WIDTH = 16;
    private static final int CELL_HEIGHT = 6;

    private final ModelPart head;

    SpiritFaces(ModelPart root) {
        head = root.getChild("head");
    }

    static LayerDefinition createFaceLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition head = mesh.getRoot().addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        for (int row = 0; row < NOTES.length; row++) {
            for (int column = 0; column < LOOKS.length; column++) face(head, NOTES[row] + "_" + LOOKS[column], column, row);
        }
        face(head, SLEEP, 0, NOTES.length);
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static void face(PartDefinition head, String name, int column, int row) {
        head.addOrReplaceChild(name, CubeListBuilder.create()
                .texOffs(column * CELL_WIDTH, row * CELL_HEIGHT).addBox(-4f, -3f, 0f, 8f, 6f, 0f), PartPose.offset(0f, -0.5f, -4.05f));
    }

    static String name(int face, byte mood, boolean blinking) {
        String note = NOTES[Mth.clamp(face, 0, NOTES.length - 1)];
        return switch (mood) {
            case HookahSpirit.HUNTING -> note + "_squint";
            case HookahSpirit.STRUCK -> note + "_struck";
            case HookahSpirit.SLEEPING -> SLEEP;
            default -> note + (blinking ? "_blink" : "_open");
        };
    }

    void render(ModelPart bodyHead, String face, PoseStack poseStack, VertexConsumer buffer, int light, int overlay) {
        head.copyFrom(bodyHead);
        poseStack.pushPose();
        head.translateAndRotate(poseStack);
        head.getChild(face).render(poseStack, buffer, light, overlay);
        poseStack.popPose();
    }
}
