package net.sorukoma.quasium.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MinecartRenderer;
import net.minecraft.world.entity.player.Player;
import net.sorukoma.quasium.cart.HookahMinecart;

public class HookahMinecartRenderer extends MinecartRenderer<HookahMinecart> {
    private static final double HOSE_HEIGHT = 0.7;

    public HookahMinecartRenderer(EntityRendererProvider.Context context) {
        super(context, ModelLayers.CHEST_MINECART);
    }

    @Override
    public boolean shouldRender(HookahMinecart cart, Frustum frustum, double camX, double camY, double camZ) {
        if (super.shouldRender(cart, frustum, camX, camY, camZ)) return true;
        Player holder = cart.hoseHolder();
        return holder != null && frustum.isVisible(holder.getBoundingBoxForCulling());
    }

    @Override
    public void render(HookahMinecart cart, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.render(cart, yaw, partialTick, poseStack, buffer, packedLight);
        Player holder = cart.hoseHolder();
        if (holder == null) return;
        int holderLight = entityRenderDispatcher.getRenderer(holder).getPackedLightCoords(holder, partialTick);
        poseStack.pushPose();
        poseStack.translate(0, HOSE_HEIGHT, 0);
        HookahConnectionRenderer.renderHose(poseStack, buffer, holder, cart.getPosition(partialTick).add(0, HOSE_HEIGHT, 0),
                partialTick, packedLight, holderLight);
        poseStack.popPose();
    }
}
