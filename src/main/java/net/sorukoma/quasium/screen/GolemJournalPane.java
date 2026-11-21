package net.sorukoma.quasium.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.sorukoma.quasium.golem.GolemJournal;

import java.util.ArrayList;
import java.util.List;

final class GolemJournalPane extends GolemListPane {
    private static final int LINE = 10;
    private static final int TEXT = 0x404040;
    private static final int MUTED = 0x808080;
    private static final Component EMPTY = Component.translatable("quasium.golem.journal.empty");

    private sealed interface Row permits Stamp, Text {
    }

    private record Stamp(GolemJournal.Entry entry) implements Row {
    }

    private record Text(FormattedCharSequence line, int color) implements Row {
    }

    private final List<Row> rows = new ArrayList<>();

    GolemJournalPane() {
        super(LINE);
    }

    void rebuild(List<GolemJournal.Entry> entries, Font font) {
        rows.clear();
        if (entries.isEmpty()) font.split(EMPTY, WIDTH).forEach(line -> rows.add(new Text(line, MUTED)));
        for (GolemJournal.Entry entry : entries) {
            rows.add(new Stamp(entry));
            font.split(message(entry), WIDTH).forEach(line -> rows.add(new Text(line, TEXT)));
        }
        scrollTo(first);
    }

    private static Component message(GolemJournal.Entry entry) {
        Object[] args = entry.args().stream()
                .map(arg -> Language.getInstance().has(arg) ? Component.translatable(arg) : Component.literal(arg)).toArray();
        return Component.translatable(entry.key(), args);
    }

    @Override
    int rowCount() {
        return rows.size();
    }

    @Override
    void renderRow(GuiGraphics graphics, Font font, int row, int x, int y) {
        switch (rows.get(row)) {
            case Stamp stamp -> graphics.drawString(font, stamp(stamp.entry()), x, y + 1, MUTED, false);
            case Text text -> graphics.drawString(font, text.line(), x, y + 1, text.color(), false);
        }
    }

    private static Component stamp(GolemJournal.Entry entry) {
        long seconds = Math.max(0, Minecraft.getInstance().level.getGameTime() - entry.time()) / 20;
        MutableComponent ago = seconds < 60 ? Component.translatable("quasium.golem.ago.now")
                : seconds < 3600 ? Component.translatable("quasium.golem.ago.minutes", seconds / 60)
                : seconds < 86400 ? Component.translatable("quasium.golem.ago.hours", seconds / 3600)
                : Component.translatable("quasium.golem.ago.days", seconds / 86400);
        return entry.repeats() > 1 ? ago.append(" x" + entry.repeats()) : ago;
    }
}
