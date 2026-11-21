package net.sorukoma.quasium.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.sorukoma.quasium.golem.ClayGolemEntity;
import net.sorukoma.quasium.golem.GolemPipe;
import net.sorukoma.quasium.golem.SmokeMarks;
import org.joml.Vector3f;

public class ClayGolemRenderer extends WorkerGolemRenderer<ClayGolemEntity, ClayGolemModel> {
    private static final float MODEL_DROP = 1.501f;

    public ClayGolemRenderer(EntityRendererProvider.Context context) {
        super(context, new ClayGolemModel(context.bakeLayer(ClayGolemModel.LAYER)), 0.45f, "clay_golem");
        addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public void render(ClayGolemEntity golem, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffer, int light) {
        super.render(golem, yaw, partialTick, pose, buffer, light);
        GolemPipe pipe = golem.getExistingDataOrNull(SmokeMarks.PIPE);
        if (pipe == null) return;
        Vec3 at = golem.getPosition(partialTick);
        Vec3 hookah = Vec3.atCenterOf(pipe.hookah());
        pose.pushPose();
        pose.translate(hookah.x - at.x, hookah.y - at.y, hookah.z - at.z);
        HookahConnectionRenderer.renderHose(pose, buffer, at.add(fist(golem, partialTick)), hookah,
                LevelRenderer.getLightColor(golem.level(), pipe.hookah()), light);
        pose.popPose();
    }

    private Vec3 fist(ClayGolemEntity golem, float partialTick) {
        PoseStack hand = new PoseStack();
        float scale = golem.getScale();
        hand.mulPose(Axis.YP.rotationDegrees(180f - Mth.rotLerp(partialTick, golem.yBodyRotO, golem.yBodyRot)));
        hand.scale(-scale, -scale, scale);
        hand.translate(0f, -MODEL_DROP, 0f);
        getModel().translateToFist(golem.getMainArm(), hand);
        Vector3f fist = hand.last().pose().transformPosition(0f, 0f, 0f, new Vector3f());
        return new Vec3(fist.x, fist.y, fist.z);
    }
}
