package net.sorukoma.quasium.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.sorukoma.quasium.block.ModBlocks;
import net.sorukoma.quasium.block.TobaccoCropBlock;
import net.sorukoma.quasium.block.entity.TobaccoPoleBlockEntity;

public class TobaccoPoleRenderer implements BlockEntityRenderer<TobaccoPoleBlockEntity> {

    public TobaccoPoleRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(TobaccoPoleBlockEntity entity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int combinedLight, int combinedOverlay) {
        ItemStack seed = entity.getResult();
        if (seed.isEmpty()) return;

        // Прогресс скрещивания задаёт стадию роста растения (как на обычной грядке).
        float progressRatio = 1.0f;
        if (!entity.isMatured() && entity.getTotalTime() > 0) {
            progressRatio = Math.min(1.0f, (float) entity.getProgress() / entity.getTotalTime());
        }
        int maxAge = ModBlocks.HYBRID_TOBACCO_CROP.get() instanceof TobaccoCropBlock crop ? crop.getMaxAge() : 4;
        int age = Math.round(progressRatio * maxAge);

        BlockState cropState = ModBlocks.HYBRID_TOBACCO_CROP.get().defaultBlockState()
                .setValue(TobaccoCropBlock.AGE, age);

        // Растение "растёт" между столбиками, чуть приподнято над основанием жёрдочки.
        poseStack.pushPose();
        poseStack.translate(0.0, 0.05, 0.0);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                cropState, poseStack, bufferSource, combinedLight, combinedOverlay);
        poseStack.popPose();
    }
}
