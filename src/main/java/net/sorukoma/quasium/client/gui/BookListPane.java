package net.sorukoma.quasium.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.sorukoma.quasium.client.gui.book.BookArt;
import net.sorukoma.quasium.client.gui.book.BookHover;
import net.sorukoma.quasium.client.gui.book.BookResearch;
import net.sorukoma.quasium.progression.ProgressionArticle;

import java.util.List;

final class BookListPane {
    private static final int ROW = 18;
    private static final int SCROLLBAR = 6;
    private static final int ICON_X = 4;
    private static final int TEXT_X = 23;
    private static final String ELLIPSIS = "...";

    private final ProgressionBookScreen screen;
    private int x;
    private int y;
    private int width;
    private int height;
    private int scroll;
    private boolean dragging;

    BookListPane(ProgressionBookScreen screen) {
        this.screen = screen;
    }

    void place(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    void reveal(int index) {
        if (index >= 0 && index < scroll) scroll = index;
        if (index >= scroll + rows()) scroll = index - rows() + 1;
        scroll = Mth.clamp(scroll, 0, maxScroll());
    }

    void step(int direction) {
        List<ProgressionArticle> listed = screen.listed();
        if (listed.isEmpty()) return;
        int index = Mth.clamp(listed.indexOf(screen.selected()) + direction, 0, listed.size() - 1);
        choose(listed.get(index));
        reveal(index);
    }

    void render(GuiGraphics graphics, Font font, BookHover hover) {
        List<ProgressionArticle> listed = screen.listed();
        int right = textRight();
        for (int row = 0; row < rows() && scroll + row < listed.size(); row++) {
            ProgressionArticle article = listed.get(scroll + row);
            int rowY = y + row * ROW;
            boolean open = screen.isUnlocked(article);
            if (article == screen.selected()) {
                graphics.fill(x, rowY, right, rowY + ROW, BookArt.palette().rowSelected());
                graphics.fill(x, rowY, x + 2, rowY + ROW, BookArt.palette().marker());
            } else if (hover.over(x, rowY, right - x, ROW)) {
                graphics.fill(x, rowY, right, rowY + ROW, BookArt.palette().rowHover());
            }
            ResourceLocation icon = article.getIcon() == null ? null : ResourceLocation.tryParse(article.getIcon());
            if (icon != null) graphics.renderFakeItem(new ItemStack(BuiltInRegistries.ITEM.get(icon)), x + ICON_X, rowY + 1);
            int textEnd = right - 2;
            if (!open) {
                BookArt.shadeIcon(graphics, x + ICON_X, rowY + 1);
                textEnd -= BookArt.LOCK_W + 3;
                graphics.blitSprite(BookArt.LOCK, textEnd + 3, rowY + 2, BookArt.LOCK_W, BookArt.LOCK_H);
            }
            Component title = BookResearch.listTitle(article);
            String full = title.getString();
            int room = textEnd - (x + TEXT_X);
            String shown = font.width(full) <= room ? full : font.plainSubstrByWidth(full, room - font.width(ELLIPSIS)).stripTrailing() + ELLIPSIS;
            graphics.drawString(font, shown, x + TEXT_X, rowY + 5, open ? BookArt.palette().title() : BookArt.palette().muted(), false);
            if (!shown.equals(full)) hover.text(x, rowY, right - x, ROW, title);
        }
        if (maxScroll() > 0) renderScrollbar(graphics);
    }

    private void renderScrollbar(GuiGraphics graphics) {
        int track = rows() * ROW;
        int handle = Math.max(16, track * rows() / screen.listed().size());
        int barX = x + width - SCROLLBAR;
        graphics.fill(barX, y, barX + SCROLLBAR, y + track, BookArt.palette().scrollTrack());
        graphics.blitSprite(BookArt.SCROLLER, barX, y + (track - handle) * scroll / maxScroll(), SCROLLBAR, handle);
    }

    boolean mouseClicked(double mouseX, double mouseY) {
        if (mouseX < x || mouseX >= x + width || mouseY < y || mouseY >= y + rows() * ROW) return false;
        if (maxScroll() > 0 && mouseX >= x + width - SCROLLBAR) {
            dragging = true;
            scrollTo(mouseY);
            return true;
        }
        int index = scroll + (int) ((mouseY - y) / ROW);
        if (index < screen.listed().size()) choose(screen.listed().get(index));
        return true;
    }

    boolean mouseDragged(double mouseY) {
        if (!dragging) return false;
        scrollTo(mouseY);
        return true;
    }

    void mouseReleased() {
        dragging = false;
    }

    boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (mouseX < x || mouseX >= x + width || mouseY < y || mouseY >= y + height || amount == 0) return false;
        scroll = Mth.clamp(scroll - (int) Math.signum(amount), 0, maxScroll());
        return true;
    }

    private void choose(ProgressionArticle article) {
        if (article == screen.selected()) return;
        screen.select(article);
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1f));
    }

    private void scrollTo(double mouseY) {
        double fraction = (mouseY - y) / (rows() * ROW);
        scroll = Mth.clamp((int) Math.round(fraction * maxScroll()), 0, maxScroll());
    }

    private int rows() {
        return Math.max(1, height / ROW);
    }

    private int maxScroll() {
        return Math.max(0, screen.listed().size() - rows());
    }

    private int textRight() {
        return x + width - (maxScroll() > 0 ? SCROLLBAR + 3 : 0);
    }
}
