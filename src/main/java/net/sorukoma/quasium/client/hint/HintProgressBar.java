package net.sorukoma.quasium.client.hint;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.function.IntConsumer;

final class HintProgressBar extends AbstractWidget {
    private static final int FILL = 0x80AAAADD;
    private static final int FILL_SHADE = 0x50AAAADD;
    private static final int MARK = 0x70FFFFFF;
    private static final int MARK_HOVERED = 0xE0FFFFFF;
    private static final float CHASE = 0.5f;

    private final int barX;
    private final int barY;
    private final int barWidth;
    private final int[] starts;
    private final IntConsumer seek;
    private float progress;
    private float progressBefore;
    private int time;

    HintProgressBar(int x, int y, int width, int[] starts, IntConsumer seek) {
        super(x, y - 3, width + 4, 24, Component.empty());
        this.barX = x;
        this.barY = y;
        this.barWidth = width;
        this.starts = starts;
        this.seek = seek;
        this.active = keyframes() > 0;
    }

    void tick(int time) {
        this.time = time;
        progressBefore = progress;
        progress += ((float) time / total() - progress) * CHASE;
    }

    private int total() {
        return starts[starts.length - 1];
    }

    private int keyframes() {
        return starts.length - 2;
    }

    private int keyframeTime(int index) {
        return starts[index + 1];
    }

    private int hoveredKeyframe(double mouseX) {
        int at = (int) ((mouseX - barX) / (barWidth + 4) * total());
        int toEnd = total() - at;
        if (toEnd > 0 && toEnd < (at - keyframeTime(keyframes() - 1)) / 2) return keyframes();
        int hovered = -1;
        for (int index = 0; index < keyframes() && keyframeTime(index) <= at; index++) hovered = index;
        return hovered;
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        seek.accept(hoveredKeyframe(mouseX) + 1);
    }

    @Override
    public void playDownSound(SoundManager sounds) {
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        HintTheme.box(graphics, barX, barY, barWidth, 1, HintTheme.FLAT, HintTheme.IDLE_TOP, HintTheme.IDLE_BOTTOM);
        graphics.pose().pushPose();
        graphics.pose().translate(barX - 2, barY - 2, 0);
        graphics.pose().pushPose();
        graphics.pose().scale((barWidth + 4) * Mth.lerp(partialTick, progressBefore, progress), 1, 1);
        graphics.fill(0, 1, 1, 3, FILL);
        graphics.fill(0, 3, 1, 4, FILL_SHADE);
        graphics.pose().popPose();
        int hovered = active && isHovered ? hoveredKeyframe(mouseX) : -2;
        if (hovered == -1) marker(graphics, true, 0, 0);
        if (hovered == keyframes()) marker(graphics, true, total(), barWidth + 4);
        for (int index = 0; index < keyframes(); index++) {
            int at = keyframeTime(index);
            marker(graphics, index == hovered, at, (int) ((float) at / total() * (barWidth + 2)));
        }
        graphics.pose().popPose();
    }

    private void marker(GuiGraphics graphics, boolean hovered, int at, int x) {
        int color = hovered ? MARK_HOVERED : MARK;
        int length = hovered ? 8 : 4;
        if (hovered) {
            Font font = Minecraft.getInstance().font;
            boolean ahead = time < at;
            String arrow = ahead ? ">" : "<";
            graphics.fill(x, 9, x + 2, 9 + length, color);
            graphics.drawString(font, Component.literal(arrow).withStyle(ChatFormatting.BOLD), ahead ? x - 2 - font.width(arrow) : x + 4,
                    10, color, false);
        }
        graphics.fill(x, 0, x + 2, 1 + length, color);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
