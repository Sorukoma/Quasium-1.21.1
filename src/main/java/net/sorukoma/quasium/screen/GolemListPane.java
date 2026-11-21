package net.sorukoma.quasium.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

abstract class GolemListPane {
    static final int LEFT = 8;
    static final int TOP = 18;
    static final int WIDTH = 146;
    static final int HEIGHT = 160;
    private static final int TRACK_X = 157;
    private static final int TRACK_W = 12;
    private static final int SCROLLER_H = 15;
    private static final ResourceLocation SCROLLER = ResourceLocation.withDefaultNamespace("container/creative_inventory/scroller");
    private static final ResourceLocation SCROLLER_OFF = ResourceLocation.withDefaultNamespace("container/creative_inventory/scroller_disabled");

    private final int rowHeight;
    protected int left;
    protected int top;
    protected int first;
    private boolean dragging;

    GolemListPane(int rowHeight) {
        this.rowHeight = rowHeight;
    }

    abstract int rowCount();

    abstract void renderRow(GuiGraphics graphics, Font font, int row, int x, int y);

    void place(int screenLeft, int screenTop) {
        left = screenLeft;
        top = screenTop;
        scrollTo(first);
    }

    int visibleRows() {
        return HEIGHT / rowHeight;
    }

    int rowY(int slot) {
        return top + TOP + slot * rowHeight;
    }

    void scrollTo(int row) {
        first = Mth.clamp(row, 0, Math.max(0, rowCount() - visibleRows()));
        scrolled();
    }

    void scrolled() {
    }

    void render(GuiGraphics graphics, Font font) {
        for (int slot = 0; slot < visibleRows() && first + slot < rowCount(); slot++) {
            renderRow(graphics, font, first + slot, left + LEFT, rowY(slot));
        }
        int max = rowCount() - visibleRows();
        int y = top + TOP + (max <= 0 ? 0 : (HEIGHT - SCROLLER_H) * first / max);
        graphics.blitSprite(max <= 0 ? SCROLLER_OFF : SCROLLER, left + TRACK_X, y, TRACK_W, SCROLLER_H);
    }

    boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (amount == 0 || !inside(mouseX, mouseY, left + LEFT, left + TRACK_X + TRACK_W)) return false;
        scrollTo(first - (int) Math.signum(amount));
        return true;
    }

    boolean mouseClicked(double mouseX, double mouseY) {
        dragging = rowCount() > visibleRows() && inside(mouseX, mouseY, left + TRACK_X, left + TRACK_X + TRACK_W);
        if (dragging) drag(mouseY);
        return dragging;
    }

    boolean mouseDragged(double mouseY) {
        if (dragging) drag(mouseY);
        return dragging;
    }

    void mouseReleased() {
        dragging = false;
    }

    private boolean inside(double mouseX, double mouseY, int fromX, int toX) {
        return mouseX >= fromX && mouseX < toX && mouseY >= top + TOP && mouseY < top + TOP + HEIGHT;
    }

    private void drag(double mouseY) {
        float share = (float) (mouseY - top - TOP - SCROLLER_H / 2.0) / (HEIGHT - SCROLLER_H);
        scrollTo(Math.round(Mth.clamp(share, 0f, 1f) * (rowCount() - visibleRows())));
    }
}
