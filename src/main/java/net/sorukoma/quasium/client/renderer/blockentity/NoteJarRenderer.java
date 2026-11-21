package net.sorukoma.quasium.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.block.entity.NoteJarBlockEntity;

public class NoteJarRenderer implements BlockEntityRenderer<NoteJarBlockEntity> {
    private static final ResourceLocation SMOKE = ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "textures/block/note_jar_smoke.png");
    private static final float MIN = 4.2f / 16f;
    private static final float MAX = 11.8f / 16f;
    private static final float BOTTOM = 1.05f / 16f;
    private static final float TOP = 12.8f / 16f;

    public NoteJarRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(NoteJarBlockEntity jar, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
                       int light, int overlay) {
        if (jar.note() == null || jar.amount() <= 0) return;
        float height = BOTTOM + (TOP - BOTTOM) * jar.fill();
        int color = jar.note().color();
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(SMOKE));
        PoseStack.Pose pose = poseStack.last();
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        face(consumer, pose, r, g, b, light, new float[][]{{MIN, height, MIN}, {MIN, height, MAX}, {MAX, height, MAX}, {MAX, height, MIN}}, 0, 1, 0);
        face(consumer, pose, r, g, b, light, new float[][]{{MIN, BOTTOM, MIN}, {MAX, BOTTOM, MIN}, {MAX, BOTTOM, MAX}, {MIN, BOTTOM, MAX}}, 0, -1, 0);
        face(consumer, pose, r, g, b, light, new float[][]{{MIN, BOTTOM, MIN}, {MIN, height, MIN}, {MAX, height, MIN}, {MAX, BOTTOM, MIN}}, 0, 0, -1);
        face(consumer, pose, r, g, b, light, new float[][]{{MAX, BOTTOM, MAX}, {MAX, height, MAX}, {MIN, height, MAX}, {MIN, BOTTOM, MAX}}, 0, 0, 1);
        face(consumer, pose, r, g, b, light, new float[][]{{MIN, BOTTOM, MAX}, {MIN, height, MAX}, {MIN, height, MIN}, {MIN, BOTTOM, MIN}}, -1, 0, 0);
        face(consumer, pose, r, g, b, light, new float[][]{{MAX, BOTTOM, MIN}, {MAX, height, MIN}, {MAX, height, MAX}, {MAX, BOTTOM, MAX}}, 1, 0, 0);
    }

    private static void face(VertexConsumer consumer, PoseStack.Pose pose, float r, float g, float b, int light,
                             float[][] corners, float nx, float ny, float nz) {
        float[][] uvs = {{0, 1}, {0, 0}, {1, 0}, {1, 1}};
        for (int i = 0; i < 4; i++) {
            consumer.addVertex(pose, corners[i][0], corners[i][1], corners[i][2])
                    .setColor(r, g, b, 0.78f)
                    .setUv(uvs[i][0], uvs[i][1])
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light)
                    .setNormal(pose, nx, ny, nz);
        }
    }
}
