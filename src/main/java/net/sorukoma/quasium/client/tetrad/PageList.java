package net.sorukoma.quasium.client.tetrad;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.sorukoma.quasium.magic.NoteType;
import net.sorukoma.quasium.magic.page.CompiledPage;
import net.sorukoma.quasium.magic.page.PageCasting;
import net.sorukoma.quasium.magic.page.PageCompiler;
import net.sorukoma.quasium.magic.page.PageLabels;
import net.sorukoma.quasium.magic.page.SpellPage;
import net.sorukoma.quasium.magic.page.TetradContents;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

final class PageList {
    private static final int MAX_ROW = 14;
    private static final int RIBBON_W = 5;
    private static final int RIBBON_H = 9;
    private static final int HOVER = 0x30FFFFFF;

    private int left;
    private int top;
    private int width;
    private int height;
    private int rowHeight;
    private int rows;

    void place(int x, int y, int w, int h, int pages) {
        left = x;
        top = y;
        width = w;
        height = h;
        rows = pages;
        rowHeight = Math.min(MAX_ROW, (h - 2) / Math.max(1, pages));
    }

    int rowAt(double mouseX, double mouseY) {
        if (mouseX < left + 1 || mouseX >= left + width - 1 || mouseY < top + 1) return -1;
        int row = (int) ((mouseY - top - 1) / rowHeight);
        return row < rows ? row : -1;
    }

    void render(GuiGraphics graphics, Font font, TetradContents contents, int selected, SpellPage draft, int mouseX, int mouseY) {
        graphics.blitSprite(TetradArt.INSET, left, top, width, height);
        int hovered = rowAt(mouseX, mouseY);
        int numberWidth = font.width(String.valueOf(rows)) + 4;
        for (int row = 0; row < Math.min(rows, contents.size()); row++) {
            int x = left + 1;
            int y = top + 1 + row * rowHeight;
            int w = width - 2;
            if (row == selected) {
                graphics.blitSprite(TetradArt.ROW, x, y, w, rowHeight);
            } else if (row == hovered) {
                graphics.fill(x, y, x + w, y + rowHeight, HOVER);
            }
            int textY = y + (rowHeight - 8) / 2 + 1;
            graphics.drawString(font, String.valueOf(row + 1), x + 2, textY, TetradArt.DIM, false);
            SpellPage page = row == selected ? draft : contents.page(row);
            if (row == contents.active() && !contents.page(row).isEmpty()) {
                graphics.blitSprite(TetradArt.RIBBON, x + w - RIBBON_W - 1, y + (rowHeight - RIBBON_H) / 2, RIBBON_W, RIBBON_H);
            }
            if (!named(page)) {
                graphics.drawString(font, "-", x + numberWidth, textY, TetradArt.DIM, false);
                continue;
            }
            Set<NoteType> notes = compiled(page).map(CompiledPage::notes).orElse(Set.of());
            int dotsWidth = TetradArt.dotsWidth(notes);
            int room = w - numberWidth - dotsWidth - RIBBON_W - 6;
            graphics.drawString(font, font.plainSubstrByWidth(label(page).getString(), room), x + numberWidth, textY, TetradArt.INK, false);
            TetradArt.dots(graphics, notes, x + w - RIBBON_W - 3 - dotsWidth, y + (rowHeight - 4) / 2);
        }
    }

    @Nullable
    List<FormattedCharSequence> tooltip(Font font, TetradContents contents, int selected, double mouseX, double mouseY) {
        int row = rowAt(mouseX, mouseY);
        if (row < 0 || row == selected) return null;
        SpellPage page = contents.page(row);
        if (page.isEmpty()) return null;
        List<FormattedCharSequence> lines = new ArrayList<>();
        lines.add(label(page).getVisualOrderText());
        compiled(page).ifPresent(compiled -> lines.addAll(font.split(PageFacts.describe(compiled), 200)));
        return lines;
    }

    static Component label(SpellPage page) {
        return PageLabels.of(PageCasting.label(page));
    }

    static Component composition(SpellPage page) {
        if (page.isSeal()) return Component.translatable("quasium.tetrad.seal");
        MutableComponent parts = PageLabels.form(page.form()).copy();
        page.hearts().forEach(heart -> parts.append(" + ").append(PageLabels.effect(heart)));
        page.tops().forEach(top -> parts.append(" + ").append(PageLabels.top(top)));
        return parts;
    }

    private static boolean named(SpellPage page) {
        return !page.isEmpty() || !page.name().isBlank();
    }

    private static Optional<CompiledPage> compiled(SpellPage page) {
        return PageCompiler.compile(page, 0, null);
    }
}
