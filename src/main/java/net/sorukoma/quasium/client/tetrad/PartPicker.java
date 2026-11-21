package net.sorukoma.quasium.client.tetrad;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Player;
import net.sorukoma.quasium.magic.NoteType;
import net.sorukoma.quasium.magic.page.PageCatalog;
import net.sorukoma.quasium.magic.page.PageEffect;
import net.sorukoma.quasium.magic.page.PageForm;
import net.sorukoma.quasium.magic.page.PageTop;
import net.sorukoma.quasium.magic.page.PageUnlocks;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

public final class PartPicker {
    public enum Kind { FORM, HEART, TOP }

    record Cell(@Nullable String id, int x, int y) {
    }

    private static final int STEP = TetradArt.CELL + 2;
    private static final int PAD = 5;
    private static final int HEADER = 26;
    private static final int NOTE_ROW = 6;

    private final List<Cell> cells = new ArrayList<>();
    @Nullable
    private Kind kind;
    private int slot;
    private int left;
    private int top;
    private int width;
    private int height;

    boolean isOpen() {
        return kind != null;
    }

    boolean picks(Kind candidate, int candidateSlot) {
        return kind == candidate && slot == candidateSlot;
    }

    @Nullable
    Kind kind() {
        return kind;
    }

    int slot() {
        return slot;
    }

    void close() {
        kind = null;
        cells.clear();
    }

    void open(Kind newKind, int newSlot, boolean removable, int x, int y, int w, int h) {
        close();
        kind = newKind;
        slot = newSlot;
        left = x;
        top = y;
        width = w;
        height = h;
        PageCatalog catalog = PageCatalog.get();
        int gridX = x + PAD;
        int gridY = y + HEADER;
        switch (newKind) {
            case FORM -> lay(sorted(catalog.forms(), PageForm::order), 7, gridX, gridY);
            case TOP -> lay(sorted(catalog.tops(), PageTop::order), 7, gridX, gridY);
            case HEART -> hearts(catalog, gridX, gridY + NOTE_ROW);
        }
        if (removable) cells.add(new Cell(null, x + w - PAD - TetradArt.CELL, y + 3));
    }

    boolean covers(double mouseX, double mouseY) {
        return isOpen() && mouseX >= left && mouseX < left + width && mouseY >= top && mouseY < top + height;
    }

    @Nullable
    Cell at(double mouseX, double mouseY) {
        for (Cell cell : cells) {
            if (mouseX >= cell.x() && mouseX < cell.x() + TetradArt.CELL && mouseY >= cell.y() && mouseY < cell.y() + TetradArt.CELL) {
                return cell;
            }
        }
        return null;
    }

    @Nullable
    Component refusal(Player player, TetradDraft draft, String id) {
        if (kind == null) return null;
        PageCatalog catalog = PageCatalog.get();
        return switch (kind) {
            case FORM -> draft.formRefusal(player, id, catalog.form(id));
            case HEART -> draft.effectRefusal(player, slot, id, catalog.effect(id));
            case TOP -> draft.topRefusal(player, slot, id, catalog.top(id));
        };
    }

    void render(GuiGraphics graphics, Font font, Player player, TetradDraft draft, int mouseX, int mouseY) {
        if (kind == null) return;
        graphics.blitSprite(TetradArt.INSET, left, top, width, height);
        graphics.drawString(font, Component.translatable("quasium.tetrad.pick." + kind.name().toLowerCase(Locale.ROOT)),
                left + PAD, top + 8, TetradArt.INK, false);
        if (kind == Kind.HEART) noteHeads(graphics);
        for (Cell cell : cells) {
            boolean allowed = cell.id() == null || refusal(player, draft, cell.id()) == null;
            boolean hovered = at(mouseX, mouseY) == cell;
            ResourceLocation frame = !allowed ? TetradArt.SLOT_LOCKED : hovered ? TetradArt.SLOT_HOVER : TetradArt.SLOT;
            graphics.blitSprite(frame, cell.x(), cell.y(), TetradArt.CELL, TetradArt.CELL);
            if (!allowed) graphics.setColor(0.5f, 0.5f, 0.5f, 1f);
            graphics.blitSprite(cell.id() == null ? TetradArt.REMOVE : icon(kind, cell.id()), cell.x() + 2, cell.y() + 2, TetradArt.ICON, TetradArt.ICON);
            graphics.setColor(1f, 1f, 1f, 1f);
            if (!allowed && closed(player, cell.id())) graphics.blitSprite(TetradArt.LOCK, cell.x() + TetradArt.CELL - 8, cell.y() + TetradArt.CELL - 9, 7, 8);
        }
    }

    @Nullable
    List<FormattedCharSequence> tooltip(Font font, Player player, TetradDraft draft, double mouseX, double mouseY) {
        Cell cell = at(mouseX, mouseY);
        if (cell == null || kind == null) return null;
        if (cell.id() == null) return List.of(Component.translatable("quasium.tetrad.remove").getVisualOrderText());
        return info(font, kind, cell.id(), refusal(player, draft, cell.id()));
    }

    private boolean closed(Player player, String id) {
        if (kind == null) return false;
        PageCatalog catalog = PageCatalog.get();
        String unlock = switch (kind) {
            case FORM -> catalog.form(id).unlock();
            case HEART -> catalog.effect(id).unlock();
            case TOP -> catalog.top(id).unlock();
        };
        return !PageUnlocks.open(player, unlock);
    }

    static List<FormattedCharSequence> info(Font font, Kind kind, String id, @Nullable Component refusal) {
        PageCatalog catalog = PageCatalog.get();
        PageForm form = catalog.form(id);
        PageEffect effect = catalog.effect(id);
        PageTop top = catalog.top(id);
        if (kind == Kind.FORM && form != null) return PartInfo.form(font, id, form, refusal);
        if (kind == Kind.HEART && effect != null) return PartInfo.effect(font, id, effect, refusal);
        if (kind == Kind.TOP && top != null) return PartInfo.top(font, id, top, refusal);
        return List.of(Component.translatable("quasium.tetrad.unknown_part", id).getVisualOrderText());
    }

    static ResourceLocation icon(Kind kind, String id) {
        return switch (kind) {
            case FORM -> TetradArt.form(id);
            case HEART -> TetradArt.effect(id);
            case TOP -> TetradArt.top(id);
        };
    }

    private void noteHeads(GuiGraphics graphics) {
        NoteType[] notes = NoteType.values();
        for (int column = 0; column < notes.length; column++) {
            TetradArt.dots(graphics, List.of(notes[column]), left + PAD + column * STEP + TetradArt.CELL / 2 - 2, top + HEADER);
        }
    }

    private void hearts(PageCatalog catalog, int x, int y) {
        NoteType[] notes = NoteType.values();
        for (int column = 0; column < notes.length; column++) {
            NoteType note = notes[column];
            Map<String, PageEffect> effects = catalog.effects().entrySet().stream()
                    .filter(entry -> entry.getValue().note() == note)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            List<String> ofNote = sorted(effects, PageEffect::order);
            for (int row = 0; row < ofNote.size(); row++) cells.add(new Cell(ofNote.get(row), x + column * STEP, y + row * STEP));
        }
    }

    private void lay(List<String> ids, int columns, int x, int y) {
        for (int i = 0; i < ids.size(); i++) cells.add(new Cell(ids.get(i), x + i % columns * STEP, y + i / columns * STEP));
    }

    private static <T> List<String> sorted(Map<String, T> parts, ToIntFunction<T> order) {
        return parts.entrySet().stream()
                .sorted(Comparator.comparingInt((Map.Entry<String, T> entry) -> order.applyAsInt(entry.getValue())).thenComparing(Map.Entry::getKey))
                .map(Map.Entry::getKey).toList();
    }
}
