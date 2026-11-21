package net.sorukoma.quasium.ritual.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.sorukoma.quasium.ritual.brazier.RitualPedestalBlockEntity;

public final class FloatingItemRenderer implements BlockEntityRenderer<RitualPedestalBlockEntity> {
    static final float PEDESTAL_HEIGHT = 1.25f;
    private static final float SCALE = 0.45f;
    private static final float BOB = 0.05f;

    public FloatingItemRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(RitualPedestalBlockEntity pedestal, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (pedestal.getLevel() == null) return;
        draw(pedestal.item(), pedestal.getLevel(), pedestal.getBlockPos(), PEDESTAL_HEIGHT, partialTick, pose, buffers, light, overlay);
    }

    static void draw(ItemStack stack, Level level, BlockPos seed, float height, float partialTick, PoseStack pose, MultiBufferSource buffers,
                     int light, int overlay) {
        if (stack.isEmpty()) return;
        float time = level.getGameTime() + partialTick + seed.asLong() % 40;
        pose.pushPose();
        pose.translate(0.5f, height + Math.sin(time / 12f) * BOB, 0.5f);
        pose.mulPose(Axis.YP.rotationDegrees(time * 2f));
        pose.scale(SCALE, SCALE, SCALE);
        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, light, overlay, pose, buffers, level,
                (int) seed.asLong());
        pose.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(RitualPedestalBlockEntity pedestal) {
        return new AABB(pedestal.getBlockPos()).expandTowards(0, 1, 0);
    }
}
