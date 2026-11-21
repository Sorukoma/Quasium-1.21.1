package net.sorukoma.quasium.client.hint;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

final class HintTheme {
    static final int FLAT = 0xFF000000;
    static final int TRANSPARENT = 0xDD000000;
    static final int IDLE_TOP = 0x40FFEEDD;
    static final int IDLE_BOTTOM = 0x20FFEEDD;
    static final int CAPTION_TOP = 0x607A6000;
    static final int CAPTION_BOTTOM = 0x207A6000;
    static final int CAPTION_TEXT = 0xFFF6F6E5;
    static final int CAPTION_SHADOW_TOP = 0xFF494949;
    static final int CAPTION_SHADOW_BOTTOM = 0xFF393939;
    static final int TEXT = 0xFFEEEEEE;
    static final int TEXT_DARKER = 0xFFA3A3A3;
    private static final int STREAK = 0xFF101010;
    private static final int NAV_ARROW = 0xAA9999;

    private HintTheme() {
    }

    static void box(GuiGraphics graphics, int x, int y, int width, int height, int background, int top, int bottom) {
        int left = x - 3;
        int right = x + width + 3;
        int upper = y - 3;
        int lower = y + height + 3;
        graphics.fill(left, upper - 1, right, upper, background);
        graphics.fill(left - 1, upper, left, lower, background);
        graphics.fill(left, lower, right, lower + 1, background);
        graphics.fill(right, upper, right + 1, lower, background);
        graphics.fill(left, upper, right, lower, background);
        graphics.fill(left, upper, right, upper + 1, top);
        graphics.fillGradient(left, upper + 1, left + 1, lower - 1, top, bottom);
        graphics.fillGradient(right - 1, upper + 1, right, lower - 1, top, bottom);
        graphics.fill(left, lower - 1, right, lower, bottom);
    }

    static void streak(GuiGraphics graphics, float angle, int x, int y, int thickness, int length) {
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(x, y, 0);
        pose.mulPose(Axis.ZP.rotationDegrees(angle - 90));
        int half = thickness / 2;
        int strong = alpha(STREAK, 0.625f);
        int middle = alpha(STREAK, 0.5f);
        int faint = alpha(STREAK, 0.0625f);
        graphics.fillGradient(-half, 0, half, length / 2, strong, middle);
        graphics.fillGradient(-half, length / 2, half, length * 3 / 4, middle, faint);
        graphics.fillGradient(-half, length * 3 / 4, half, length, faint, alpha(STREAK, 0));
        pose.popPose();
    }

    static void navigationArrows(GuiGraphics graphics, int centerX, int y) {
        int tip = 64 << 24 | NAV_ARROW;
        int near = 32 << 24 | NAV_ARROW;
        int far = 16 << 24 | NAV_ARROW;
        arrow(graphics, centerX - 20, y, 20, 20, 5, tip, near);
        arrow(graphics, centerX + 20, y, -20, 20, -5, tip, near);
        arrow(graphics, centerX - 90, y, 70, 20, 5, tip, far);
        arrow(graphics, centerX + 90, y, -70, 20, -5, tip, far);
    }

    private static void arrow(GuiGraphics graphics, int x, int y, int width, int height, int indent, int start, int end) {
        float total = Math.abs(width) + 2f * Math.abs(indent);
        int nearTip = mix(start, end, Math.abs(indent) / total);
        int nearTail = mix(start, end, (Math.abs(indent) + Math.abs(width)) / total);
        float mid = height / 2f;
        float tail = width + indent;
        graphics.pose().pushPose();
        graphics.pose().translate(x - indent, y, 0);
        Matrix4f matrix = graphics.pose().last().pose();
        VertexConsumer quads = graphics.bufferSource().getBuffer(RenderType.debugQuads());
        quad(quads, matrix, 0, mid, start, indent, 0, nearTip, indent, height, nearTip, indent, height, nearTip);
        quad(quads, matrix, indent, 0, nearTip, width, 0, nearTail, width, height, nearTail, indent, height, nearTip);
        quad(quads, matrix, width, 0, nearTail, width, mid, nearTail, tail, 0, end, tail, 0, end);
        quad(quads, matrix, width, height, nearTail, width, mid, nearTail, tail, height, end, tail, height, end);
        graphics.pose().popPose();
    }

    private static void quad(VertexConsumer quads, Matrix4f matrix, float x1, float y1, int c1, float x2, float y2, int c2,
                             float x3, float y3, int c3, float x4, float y4, int c4) {
        quads.addVertex(matrix, x1, y1, 0).setColor(c1);
        quads.addVertex(matrix, x2, y2, 0).setColor(c2);
        quads.addVertex(matrix, x3, y3, 0).setColor(c3);
        quads.addVertex(matrix, x4, y4, 0).setColor(c4);
    }

    static int alpha(int color, float scale) {
        return (int) ((color >>> 24) * Mth.clamp(scale, 0f, 1f)) << 24 | color & 0xFFFFFF;
    }

    static int mix(int from, int to, float amount) {
        int mixed = 0;
        for (int shift = 0; shift < 32; shift += 8) {
            int a = from >>> shift & 0xFF;
            int b = to >>> shift & 0xFF;
            mixed |= (int) (a + (b - a) * amount) << shift;
        }
        return mixed;
    }
}
