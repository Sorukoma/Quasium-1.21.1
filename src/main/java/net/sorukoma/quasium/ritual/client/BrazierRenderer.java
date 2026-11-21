package net.sorukoma.quasium.ritual.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.sorukoma.quasium.ritual.brazier.RitualBrazierBlockEntity;
import net.sorukoma.quasium.ritual.brazier.RitualPreview;
import net.sorukoma.quasium.ritual.brazier.RitualPulse;
import org.joml.Matrix4f;

import java.util.List;

public final class BrazierRenderer implements BlockEntityRenderer<RitualBrazierBlockEntity> {
    private static final float CATALYST_HEIGHT = 1.05f;
    private static final float COALS = 0.7f;
    private static final float RING_FROM = 1.6f;
    private static final float RING_TO = 0.2f;
    private static final float RING_WIDTH = 0.09f;
    private static final float FLARE_RADIUS = 0.42f;
    private static final float FLARE_SHARE = 0.25f;
    private static final int RING_ALPHA = 170;
    private static final int FLARE_ALPHA = 210;
    private static final int SMOKE = 0xD8D0C8;
    private static final int EMBER = 0xFFB040;
    private static final int SEGMENTS = 32;
    private static final int REACH = 4;
    private static final float GHOST_ALPHA = 0.6f;
    private static final float BARS_HEIGHT = 1.8f;
    private static final float BAR_WIDTH = 0.7f;
    private static final float BAR_THICKNESS = 0.07f;
    private static final float BAR_GAP = 0.05f;
    private static final int BAR_TRACK = 0x99101010;
    private static final int BAR_ALPHA = 0xE0;

    public BrazierRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(RitualBrazierBlockEntity brazier, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Level level = brazier.getLevel();
        if (level == null) return;
        FloatingItemRenderer.draw(brazier.catalyst(), level, brazier.getBlockPos(), CATALYST_HEIGHT, partialTick, pose, buffers, light, overlay);
        RitualPulse pulse = brazier.pulse();
        if (pulse != null) beat(pulse.phase(level.getGameTime() + partialTick), pose.last().pose(), buffers.getBuffer(RenderType.debugQuads()));
        RitualPreview preview = brazier.preview();
        for (RitualPreview.Ghost ghost : preview.ghosts()) ghost(ghost, brazier, level, partialTick, pose, buffers, overlay);
        if (!preview.notes().isEmpty()) bars(preview.notes(), pose, buffers.getBuffer(RenderType.debugQuads()));
    }

    private static void ghost(RitualPreview.Ghost ghost, RitualBrazierBlockEntity brazier, Level level, float partialTick, PoseStack pose,
                              MultiBufferSource buffers, int overlay) {
        BlockPos offset = ghost.pedestal().subtract(brazier.getBlockPos());
        ItemStack stack = new ItemStack(ghost.item());
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, false);
        boolean custom = Minecraft.getInstance().getItemRenderer().getModel(stack, level, null, 0).isCustomRenderer();
        pose.pushPose();
        pose.translate(offset.getX(), offset.getY(), offset.getZ());
        FloatingItemRenderer.draw(stack, level, ghost.pedestal(), FloatingItemRenderer.PEDESTAL_HEIGHT, partialTick, pose,
                custom ? buffers : new GhostBuffers(buffers, GHOST_ALPHA), LevelRenderer.getLightColor(level, ghost.pedestal().above()), overlay);
        pose.popPose();
    }

    private static void bars(List<RitualPreview.NoteBar> notes, PoseStack pose, VertexConsumer quads) {
        pose.pushPose();
        pose.translate(0.5f, BARS_HEIGHT, 0.5f);
        pose.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        Matrix4f matrix = pose.last().pose();
        float left = -BAR_WIDTH / 2;
        for (int row = 0; row < notes.size(); row++) {
            RitualPreview.NoteBar bar = notes.get(row);
            float bottom = (notes.size() - 1 - row) * (BAR_THICKNESS + BAR_GAP);
            float split = left + BAR_WIDTH * bar.fill();
            rectangle(quads, matrix, left, split, bottom, BAR_ALPHA << 24 | bar.note().color());
            rectangle(quads, matrix, split, -left, bottom, BAR_TRACK);
        }
        pose.popPose();
    }

    private static void rectangle(VertexConsumer quads, Matrix4f matrix, float from, float to, float bottom, int color) {
        if (to <= from) return;
        quads.addVertex(matrix, from, bottom, 0f).setColor(color);
        quads.addVertex(matrix, from, bottom + BAR_THICKNESS, 0f).setColor(color);
        quads.addVertex(matrix, to, bottom + BAR_THICKNESS, 0f).setColor(color);
        quads.addVertex(matrix, to, bottom, 0f).setColor(color);
    }

    private static void beat(double phase, Matrix4f matrix, VertexConsumer quads) {
        float closing = (float) phase;
        int ringAlpha = Math.round(closing * closing * RING_ALPHA);
        ring(quads, matrix, COALS + 0.04f, Mth.lerp(closing, RING_FROM, RING_TO), RING_WIDTH, ringAlpha << 24 | SMOKE);
        float flare = 1f - closing / FLARE_SHARE;
        if (flare > 0f) ring(quads, matrix, COALS, FLARE_RADIUS / 2, FLARE_RADIUS, Math.round(flare * FLARE_ALPHA) << 24 | EMBER);
    }

    private static void ring(VertexConsumer quads, Matrix4f matrix, float y, float radius, float width, int color) {
        float inner = Math.max(0f, radius - width / 2);
        float outer = radius + width / 2;
        for (int segment = 0; segment < SEGMENTS; segment++) {
            double from = Math.PI * 2 * segment / SEGMENTS;
            double to = Math.PI * 2 * (segment + 1) / SEGMENTS;
            vertex(quads, matrix, from, inner, y, color);
            vertex(quads, matrix, from, outer, y, color);
            vertex(quads, matrix, to, outer, y, color);
            vertex(quads, matrix, to, inner, y, color);
        }
    }

    private static void vertex(VertexConsumer quads, Matrix4f matrix, double angle, float radius, float y, int color) {
        quads.addVertex(matrix, 0.5f + (float) Math.cos(angle) * radius, y, 0.5f + (float) Math.sin(angle) * radius).setColor(color);
    }

    @Override
    public AABB getRenderBoundingBox(RitualBrazierBlockEntity brazier) {
        return new AABB(brazier.getBlockPos()).inflate(REACH, 0, REACH).expandTowards(0, 2, 0);
    }
}
