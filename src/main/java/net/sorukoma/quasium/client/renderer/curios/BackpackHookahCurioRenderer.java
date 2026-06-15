package net.sorukoma.quasium.client.renderer.curios;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.block.ModBlocks;
import net.sorukoma.quasium.client.ClientHookahInputHandler;
import net.sorukoma.quasium.item.ModItems;
import org.joml.Matrix4f;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

public class BackpackHookahCurioRenderer implements ICurioRenderer {

    @Override
    public <T extends LivingEntity, M extends EntityModel<T>> void render(
            ItemStack stack, SlotContext slotContext, PoseStack poseStack,
            RenderLayerParent<T, M> renderLayerParent, MultiBufferSource bufferSource,
            int light, float limbSwing, float limbSwingAmount, float partialTicks,
            float ageInTicks, float netHeadYaw, float headPitch) {

        LivingEntity entity = slotContext.entity();
        if (!(entity instanceof Player player)) return;

        poseStack.pushPose();

        // Minecraft's entity rendering applies scale(-1,-1,1) (Y-flip) before render layers
        // Y+ = downward in world space, so smaller Y values = higher position on the body
        // XP 180° rotation compensates the Y-flip to keep the block model upright
        poseStack.translate(0.55F, 0.75F, 0.35F);
        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(180));
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180 + 45));
        poseStack.scale(0.85F, 0.85F, 0.85F);

        // Render the hookah block model on the back
        Level level = player.level();
        BlockState hookahState = ModBlocks.HOOKAH_BLOCK.get().defaultBlockState();
        BlockRenderDispatcher blockRenderer = Minecraft.getInstance().getBlockRenderer();
        blockRenderer.renderSingleBlock(hookahState, poseStack, bufferSource, light, 0);

        poseStack.popPose();

        // Render the pipe line from back to hand if connected
        if (ClientHookahInputHandler.isConnectedToBackpack()) {
            renderPipeLine(player, poseStack, bufferSource, light, partialTicks);
        }
    }

    private void renderPipeLine(Player player, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, float partialTicks) {
        poseStack.pushPose();

        Vec3 playerHandPos = getPlayerHandPos(player, partialTicks);
        Vec3 playerBackPos = getPlayerBackPos(player, partialTicks);
        Vec3 relativeHandPos = playerHandPos.subtract(player.position());
        Vec3 relativeBackPos = playerBackPos.subtract(player.position());

        VertexConsumer vertexconsumer = bufferSource.getBuffer(RenderType.leash());
        Matrix4f matrix4f = poseStack.last().pose();

        int r = 0, g = 0, b = 0, a = 255;

        float diffX = (float)(relativeHandPos.x - relativeBackPos.x);
        float diffY = (float)(relativeHandPos.y - relativeBackPos.y);
        float diffZ = (float)(relativeHandPos.z - relativeBackPos.z);

        float f3 = 0.025F;
        float f4 = Mth.invSqrt(diffX * diffX + diffZ * diffZ) * f3 / 2.0F;
        float f5 = diffZ * f4;
        float f6 = diffX * f4;

        float startX = (float)relativeBackPos.x;
        float startY = (float)relativeBackPos.y;
        float startZ = (float)relativeBackPos.z;

        for (int i1 = 0; i1 <= 24; i1++) {
            float f = (float)i1 / 24.0F;
            float segmentColorFactor = i1 % 2 == 0 ? 0.7F : 1.0F;
            float currentR = r * segmentColorFactor;
            float currentG = g * segmentColorFactor;
            float currentB = b * segmentColorFactor;

            float currentX = startX + diffX * f;
            float currentY = startY + (diffY > 0.0F ? diffY * f * f : diffY - diffY * (1.0F - f) * (1.0F - f));
            float currentZ = startZ + diffZ * f;

            vertexconsumer.addVertex(matrix4f, currentX - f5, currentY + f3, currentZ + f6)
                          .setColor(currentR, currentG, currentB, a)
                          .setLight(packedLight);
            vertexconsumer.addVertex(matrix4f, currentX + f5, currentY + f3 - f3, currentZ - f6)
                          .setColor(currentR, currentG, currentB, a)
                          .setLight(packedLight);
        }

        poseStack.popPose();
    }

    private Vec3 getPlayerHandPos(Player player, float partialTicks) {
        Vec3 eyePos = player.getEyePosition(partialTicks);
        float yaw = player.getViewYRot(partialTicks) * ((float)Math.PI / 180F);
        float pitch = player.getViewXRot(partialTicks) * ((float)Math.PI / 180F);
        float cosYaw = Mth.cos(yaw);
        float sinYaw = Mth.sin(yaw);
        float sinPitch = Mth.sin(pitch);
        float cosPitch = Mth.cos(pitch);

        double offsetX = -0.3 * sinYaw * cosPitch;
        double offsetY = -0.5 + (0.3 * sinPitch);
        double offsetZ = 0.3 * cosYaw * cosPitch;
        return eyePos.add(offsetX, offsetY, offsetZ);
    }

    private Vec3 getPlayerBackPos(Player player, float partialTicks) {
        float yaw = player.getViewYRot(partialTicks) * ((float)Math.PI / 180F);
        float cosYaw = Mth.cos(yaw);
        float sinYaw = Mth.sin(yaw);

        Vec3 bodyCenter = player.position().add(0, player.getEyeHeight() * 0.75, 0);
        double offsetX = -0.35 * sinYaw;
        double offsetZ = -0.35 * cosYaw;
        return bodyCenter.add(offsetX, 0, offsetZ);
    }
}