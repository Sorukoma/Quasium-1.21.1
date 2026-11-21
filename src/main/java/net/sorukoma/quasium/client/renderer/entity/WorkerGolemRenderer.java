package net.sorukoma.quasium.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.golem.WorkerGolem;

abstract class WorkerGolemRenderer<T extends WorkerGolem, M extends EntityModel<T>> extends MobRenderer<T, M> {
    private static final String[] SUFFIXES = {"", "_2", "_3", "_4", "_lying"};

    private final ResourceLocation[] faces;

    WorkerGolemRenderer(EntityRendererProvider.Context context, M model, float shadow, String texture) {
        super(context, model, shadow);
        faces = faces(texture);
    }

    static ResourceLocation[] faces(String texture) {
        ResourceLocation[] faces = new ResourceLocation[SUFFIXES.length];
        for (int i = 0; i < faces.length; i++) {
            faces[i] = ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "textures/entity/" + texture + SUFFIXES[i] + ".png");
        }
        return faces;
    }

    static ResourceLocation face(ResourceLocation[] faces, WorkerGolem golem) {
        return faces[golem.isLying() ? faces.length - 1 : golem.moodState() - 1];
    }

    @Override
    public ResourceLocation getTextureLocation(T golem) {
        return face(faces, golem);
    }

    @Override
    protected void setupRotations(T golem, PoseStack pose, float bob, float yBodyRot, float partialTick, float scale) {
        super.setupRotations(golem, pose, bob, yBodyRot, partialTick, scale);
        float down = golem.down(partialTick);
        if (down <= 0f || golem.isDeadOrDying()) return;
        float middle = golem.getBbHeight() / scale / 2;
        pose.translate(0f, Mth.lerp(down, middle, golem.getBbWidth() / scale / 2), 0f);
        pose.mulPose(Axis.ZP.rotationDegrees(90f * down));
        pose.translate(0f, -middle, 0f);
    }
}
