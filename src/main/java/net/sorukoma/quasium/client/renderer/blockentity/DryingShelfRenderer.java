package net.sorukoma.quasium.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.sorukoma.quasium.block.DryingShelfBlock;
import net.sorukoma.quasium.block.entity.DryingShelfBlockEntity;
import org.joml.Quaternionf;

public class DryingShelfRenderer implements BlockEntityRenderer<DryingShelfBlockEntity> {

    public DryingShelfRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(DryingShelfBlockEntity entity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int combinedLight, int combinedOverlay) {
        ItemStack item = entity.getItemHandler().getStackInSlot(0);
        if (item.isEmpty()) return;

        Direction facing = entity.getBlockState().getValue(DryingShelfBlock.FACING);

        poseStack.pushPose();

        // Move to center of the block, then rotate, then offset.
        poseStack.translate(0.5, 0.5, 0.5);

        // FIXED display context faces -Z (North) by default.
        // Rotate so the item faces the same direction the shelf faces (= toward the player).
        float rotationDegrees = switch (facing) {
            case NORTH ->   0.0F;
            case SOUTH -> 180.0F;
            case WEST  ->  90.0F;
            case EAST  -> -90.0F;
            default -> 0.0F;
        };
        poseStack.mulPose(new Quaternionf().rotateY((float) Math.toRadians(rotationDegrees)));

        // The board is at Y=14..16 (0.875..1.0) and depth=12..16 (0.75..1.0) in local model space.
        // Place the item just in front of the board's center.
        poseStack.translate(0.0, 0.20, 0.30);

        poseStack.scale(0.4f, 0.4f, 0.4f);

        Minecraft.getInstance().getItemRenderer().renderStatic(
                item, ItemDisplayContext.FIXED, combinedLight, combinedOverlay,
                poseStack, bufferSource, entity.getLevel(), 0);

        poseStack.popPose();
    }
}