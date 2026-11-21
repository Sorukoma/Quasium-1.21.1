package net.sorukoma.quasium.client.tetrad;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.BossEvent.BossBarColor;
import net.minecraft.world.entity.player.Player;
import net.sorukoma.quasium.client.GuiBars;
import net.sorukoma.quasium.magic.NoteType;
import net.sorukoma.quasium.magic.page.Accord;
import net.sorukoma.quasium.magic.page.CompiledPage;
import net.sorukoma.quasium.magic.page.PageCasting;
import net.sorukoma.quasium.magic.page.PageCatalog;
import net.sorukoma.quasium.magic.page.PageLabels;
import net.sorukoma.quasium.magic.page.PageRules;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class PageSheet {
    static final int LUNGS_Y = 0;
    static final int ACCORDS_Y = 30;
    static final int TEXT_Y = 42;
    private static final int LABEL_W = 42;
    private static final int BAR = 100;
    private static final int LINE = 10;
    private static final int PAD = 4;
    private static final int MARK = 0xFFFFD27A;

    private int left;
    private int top;
    private int width;
    private int height;
    private int statsX;
    private int statsY;
    private List<FormattedCharSequence> text = List.of();
    private boolean clipped;
    private Component fullText = Component.empty();

    void place(int x, int y, int w, int h, int statX, int statY) {
        left = x;
        top = y;
        width = w;
        height = h;
        statsX = statX;
        statsY = statY;
    }

    void render(GuiGraphics graphics, Font font, Player player, TetradDraft draft, Optional<Component> problem, boolean unsaved) {
        Optional<CompiledPage> compiled = draft.page().isEmpty() ? Optional.empty() : draft.compiled();
        compiled.ifPresent(page -> stats(graphics, font, page));
        lungs(graphics, font, player, compiled.orElse(null));
        compiled.ifPresent(page -> accords(graphics, font, page));
        int boxY = top + TEXT_Y;
        int boxH = height - TEXT_Y;
        graphics.blitSprite(TetradArt.INSET, left, boxY, width, boxH);
        fullText = compiled.map(PageFacts::describe).orElse(Component.translatable(draft.page().isEmpty()
                ? "quasium.tetrad.empty_page" : "quasium.tetrad.unfinished_page"));
        int rows = (boxH - 2 * PAD) / LINE - (problem.isPresent() || unsaved ? 1 : 0);
        List<FormattedCharSequence> lines = font.split(fullText, width - 2 * PAD);
        clipped = lines.size() > rows;
        text = clipped ? new ArrayList<>(lines.subList(0, Math.max(0, rows))) : lines;
        int y = boxY + PAD;
        for (FormattedCharSequence line : text) {
            graphics.drawString(font, line, left + PAD, y, TetradArt.INK, false);
            y += LINE;
        }
        if (clipped) graphics.drawString(font, "...", left + width - PAD - font.width("..."), boxY + boxH - PAD - 8, TetradArt.DIM, false);
        Component status = problem.orElse(unsaved ? Component.translatable("quasium.tetrad.unsaved") : null);
        if (status != null) {
            String shown = font.plainSubstrByWidth(status.getString(), width - 2 * PAD - 12);
            graphics.drawString(font, shown, left + PAD, boxY + boxH - PAD - 8, TetradArt.WARN, false);
        }
    }

    @Nullable
    List<FormattedCharSequence> tooltip(Font font, TetradDraft draft, double mouseX, double mouseY) {
        int boxY = top + TEXT_Y;
        if (clipped && mouseX >= left && mouseX < left + width && mouseY >= boxY && mouseY < top + height) {
            return font.split(fullText, 220);
        }
        Optional<CompiledPage> compiled = draft.page().isEmpty() ? Optional.empty() : draft.compiled();
        if (compiled.isEmpty()) return null;
        if (mouseY >= top + ACCORDS_Y && mouseY < top + ACCORDS_Y + LINE && mouseX >= left && mouseX < left + width) {
            return accordInfo(font, compiled.get());
        }
        if (mouseX >= statsX && mouseX < left + width && mouseY >= statsY && mouseY < statsY + 6 * LINE) {
            int row = (int) ((mouseY - statsY) / LINE);
            String key = row == 0 ? "volume" : row == 1 ? "grams" : row == 2 ? "cooldown" : "notes";
            return font.split(Component.translatable("quasium.tetrad.hint." + key), 180);
        }
        return null;
    }

    private void stats(GuiGraphics graphics, Font font, CompiledPage page) {
        int y = statsY;
        graphics.drawString(font, Component.translatable("quasium.tetrad.stat.ml", page.volume()), statsX, y, TetradArt.INK, false);
        String grams = PageFacts.number(PageCasting.grams(page, page.volume()));
        graphics.drawString(font, Component.translatable("quasium.tetrad.stat.grams", grams), statsX, y + LINE, TetradArt.INK, false);
        String cooldown = PageFacts.number(page.spell().cooldown());
        graphics.drawString(font, Component.translatable("quasium.tetrad.stat.seconds", cooldown), statsX, y + 2 * LINE, TetradArt.INK, false);
        int row = 3;
        for (Map.Entry<NoteType, Integer> cost : page.noteCost().entrySet()) {
            int lineY = y + row * LINE;
            TetradArt.dots(graphics, List.of(cost.getKey()), statsX, lineY + 2);
            graphics.drawString(font, Component.translatable("quasium.tetrad.stat.ml", cost.getValue()), statsX + 7, lineY, TetradArt.INK, false);
            row++;
        }
    }

    private void lungs(GuiGraphics graphics, Font font, Player player, @Nullable CompiledPage page) {
        int y = top + LUNGS_Y;
        graphics.drawString(font, Component.translatable("quasium.tetrad.lungs"), left, y, TetradArt.INK, false);
        int capacity = Math.max(1, PageRules.maxVolume(player));
        int volume = page == null ? 0 : page.volume();
        int barX = left + LABEL_W;
        int barY = y + 2;
        GuiBars.bar(graphics, volume > capacity ? BossBarColor.RED : BossBarColor.WHITE, barX, barY, BAR, volume / (float) capacity);
        if (page != null && volume < capacity) {
            PageHud.overflowZone(graphics, page, barX, barY, BAR, capacity, volume);
            int mark = barX + Math.round(BAR * volume / (float) capacity);
            graphics.fill(mark, barY - 1, mark + 1, barY + GuiBars.THICKNESS + 1, MARK);
        }
        String amount = volume + "/" + capacity;
        graphics.drawString(font, amount, left + width - font.width(amount), y, volume > capacity ? TetradArt.WARN : TetradArt.INK, false);
    }

    private void accords(GuiGraphics graphics, Font font, CompiledPage page) {
        if (page.accords().isEmpty()) return;
        MutableComponent line = Component.translatable("quasium.tetrad.accords");
        Map<String, Accord> known = PageCatalog.get().accords();
        for (int i = 0; i < page.accords().size(); i++) {
            String id = page.accords().get(i);
            line.append(i == 0 ? " " : ", ");
            Accord accord = known.get(id);
            line.append(accord == null ? PageLabels.accord(id) : PageLabels.accord(id).copy().withColor(darker(accord.color())));
        }
        graphics.drawString(font, line, left, top + ACCORDS_Y, TetradArt.INK, false);
    }

    @Nullable
    private static List<FormattedCharSequence> accordInfo(Font font, CompiledPage page) {
        List<FormattedCharSequence> lines = new ArrayList<>();
        Map<String, Accord> known = PageCatalog.get().accords();
        for (String id : page.accords()) {
            Accord accord = known.get(id);
            if (accord != null) lines.addAll(PartInfo.accord(font, id, accord));
        }
        return lines.isEmpty() ? null : lines;
    }

    private static int darker(int color) {
        int r = (color >> 16 & 0xFF) * 3 / 5;
        int g = (color >> 8 & 0xFF) * 3 / 5;
        int b = (color & 0xFF) * 3 / 5;
        return r << 16 | g << 8 | b;
    }
}
