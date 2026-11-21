package net.sorukoma.quasium.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.sorukoma.quasium.block.LowTableBlock;
import net.sorukoma.quasium.block.entity.LowTableBlockEntity;

public class LowTableRenderer implements BlockEntityRenderer<LowTableBlockEntity> {
    private static final float TOP = LowTableBlock.TOP_PX / 16f;
    private static final float ITEM_SCALE = 0.375f;
    private static final float FLAT_LIFT = 1 / 64f;
    private static final float BLOCK_LIFT = ITEM_SCALE / 4;
    private static final float[][] SLOT_CENTERS = {{0.28f, 0.28f}, {0.72f, 0.28f}, {0.28f, 0.72f}, {0.72f, 0.72f}};
    private static final float[] SLOT_YAW = {12f, -18f, -6f, 24f};

    private final ItemRenderer itemRenderer;

    public LowTableRenderer(BlockEntityRendererProvider.Context context) {
        itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(LowTableBlockEntity table, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
                       int light, int overlay) {
        for (int slot = 0; slot < LowTableBlockEntity.SLOTS; slot++) {
            ItemStack stack = table.items().get(slot);
            if (stack.isEmpty()) continue;
            boolean block = itemRenderer.getModel(stack, table.getLevel(), null, 0).isGui3d();
            poseStack.pushPose();
            poseStack.translate(SLOT_CENTERS[slot][0], TOP + (block ? BLOCK_LIFT : FLAT_LIFT), SLOT_CENTERS[slot][1]);
            poseStack.mulPose(Axis.YP.rotationDegrees(SLOT_YAW[slot]));
            if (!block) poseStack.mulPose(Axis.XP.rotationDegrees(90f));
            poseStack.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
            itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, light, overlay, poseStack, buffers, table.getLevel(),
                    (int) table.getBlockPos().asLong() + slot);
            poseStack.popPose();
        }
    }
}
