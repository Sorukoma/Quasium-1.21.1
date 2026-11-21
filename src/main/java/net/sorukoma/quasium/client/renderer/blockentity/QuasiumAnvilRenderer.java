package net.sorukoma.quasium.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.sorukoma.quasium.block.QuasiumAnvilBlock;
import net.sorukoma.quasium.block.entity.QuasiumAnvilBlockEntity;
import org.joml.Quaternionf;

public class QuasiumAnvilRenderer implements BlockEntityRenderer<QuasiumAnvilBlockEntity> {

    private static final float ANVIL_TOP_Y = 1.02F;
    private static final float ITEM_SPACING = 0.08F;
    private static final float ITEM_SCALE = 0.5F;
    private static final float TILT_X = -90.0F;
    private static final float STACK_SHIFT = 0.12F;
    private static final float STACK_LIFT = 0.06F;

    public QuasiumAnvilRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(QuasiumAnvilBlockEntity entity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int combinedLight, int combinedOverlay) {
        Direction facing = entity.getBlockState().getValue(QuasiumAnvilBlock.FACING);

        float facingDegrees = switch (facing) {
            case NORTH ->   0.0F;
            case SOUTH -> 180.0F;
            case WEST  ->  90.0F;
            case EAST  -> -90.0F;
            default -> 0.0F;
        };

        for (int i = 0; i < 3; i++) {
            ItemStack item = entity.getItemHandler().getStackInSlot(i);
            if (item.isEmpty()) continue;

            poseStack.pushPose();

            poseStack.translate(0.5F, 0.5F, 0.5F);
            poseStack.mulPose(new Quaternionf().rotateY((float) Math.toRadians(facingDegrees)));

            float yOffset = (ANVIL_TOP_Y - 0.5F) + i * ITEM_SPACING;
            poseStack.translate(0.0F, yOffset, 0.0F);

            poseStack.mulPose(new Quaternionf().rotateX((float) Math.toRadians(TILT_X)));

            poseStack.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);

            Minecraft.getInstance().getItemRenderer().renderStatic(
                    item, ItemDisplayContext.FIXED, combinedLight, combinedOverlay,
                    poseStack, bufferSource, entity.getLevel(), 0);
            if (item.getCount() > 1) {
                poseStack.translate(STACK_SHIFT, STACK_SHIFT, STACK_LIFT);
                Minecraft.getInstance().getItemRenderer().renderStatic(
                        item, ItemDisplayContext.FIXED, combinedLight, combinedOverlay,
                        poseStack, bufferSource, entity.getLevel(), 1);
            }

            poseStack.popPose();
        }
    }
}
