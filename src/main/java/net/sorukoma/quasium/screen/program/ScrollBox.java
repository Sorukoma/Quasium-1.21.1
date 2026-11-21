package net.sorukoma.quasium.screen.program;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

final class ScrollBox {
    static final int INSET = 4;
    private static final int BAR = 3;
    private static final int MIN_THUMB = 8;
    private static final int TRACK = 0xFF6E6E6E;
    private static final int THUMB = 0xFFD6D6D6;
    private static final int THUMB_SHADE = 0xFF9A9A9A;

    private final int left;
    private final int top;
    private final int width;
    private final int height;
    private int screenLeft;
    private int screenTop;
    private int content;
    private int scroll;

    ScrollBox(int left, int top, int width, int height) {
        this.left = left;
        this.top = top;
        this.width = width;
        this.height = height;
    }

    void place(int guiLeft, int guiTop) {
        screenLeft = guiLeft + left;
        screenTop = guiTop + top;
    }

    void content(int height) {
        content = height;
        scrollBy(0);
    }

    void scrollBy(double amount) {
        scroll = Mth.clamp(scroll + (int) Math.round(amount), 0, maxScroll());
    }

    private int maxScroll() {
        return Math.max(0, content + 2 * INSET - height);
    }

    boolean contains(double mouseX, double mouseY) {
        return mouseX >= screenLeft && mouseX < screenLeft + width && mouseY >= screenTop && mouseY < screenTop + height;
    }

    boolean inColumn(double mouseX) {
        return mouseX >= screenLeft && mouseX < screenLeft + width;
    }

    int screenTop() {
        return screenTop;
    }

    int screenBottom() {
        return screenTop + height;
    }

    int originX() {
        return screenLeft + INSET;
    }

    int originY() {
        return screenTop + INSET - scroll;
    }

    int innerWidth() {
        return width - 2 * INSET - BAR - 1;
    }

    void clip(GuiGraphics graphics) {
        graphics.enableScissor(screenLeft + 1, screenTop + 1, screenLeft + width - 1, screenTop + height - 1);
    }

    void bar(GuiGraphics graphics) {
        if (maxScroll() == 0) return;
        int x = screenLeft + width - BAR - 2;
        int trackTop = screenTop + 2;
        int track = height - 4;
        int thumb = Math.max(MIN_THUMB, track * height / (content + 2 * INSET));
        int thumbTop = trackTop + (track - thumb) * scroll / maxScroll();
        graphics.fill(x, trackTop, x + BAR, trackTop + track, TRACK);
        graphics.fill(x, thumbTop, x + BAR, thumbTop + thumb, THUMB);
        graphics.fill(x + BAR - 1, thumbTop + 1, x + BAR, thumbTop + thumb, THUMB_SHADE);
        graphics.fill(x, thumbTop + thumb - 1, x + BAR, thumbTop + thumb, THUMB_SHADE);
    }
}
