package net.sorukoma.quasium.client.tetrad;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Player;
import net.sorukoma.quasium.magic.Spell;
import net.sorukoma.quasium.magic.page.PageLabels;
import net.sorukoma.quasium.magic.page.PageRules;
import net.sorukoma.quasium.magic.page.SpellPage;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

final class PyramidPane {
    record Slot(PartPicker.Kind kind, int index, int x, int y, int width) {
    }

    static final int HEIGHT = 66;
    private static final int LABEL_W = 42;
    private static final int STEP = TetradArt.CELL + 2;
    private static final int WIDE = 4 * STEP - 2;

    private final List<Slot> slots = new ArrayList<>();
    private int left;
    private int top;

    void place(int x, int y) {
        left = x;
        top = y;
        slots.clear();
        int start = x + LABEL_W;
        for (int i = 0; i < SpellPage.MAX_TOPS; i++) slots.add(new Slot(PartPicker.Kind.TOP, i, start + i * STEP, y, TetradArt.CELL));
        int hearts = start + (WIDE - (SpellPage.MAX_HEARTS * STEP - 2)) / 2;
        for (int i = 0; i < SpellPage.MAX_HEARTS; i++) {
            slots.add(new Slot(PartPicker.Kind.HEART, i, hearts + i * STEP, y + STEP, TetradArt.CELL));
        }
        slots.add(new Slot(PartPicker.Kind.FORM, 0, start, y + 2 * STEP, WIDE));
    }

    int right() {
        return left + LABEL_W + WIDE;
    }

    @Nullable
    Slot at(double mouseX, double mouseY) {
        for (Slot slot : slots) {
            if (mouseX >= slot.x() && mouseX < slot.x() + slot.width() && mouseY >= slot.y() && mouseY < slot.y() + TetradArt.CELL) return slot;
        }
        return null;
    }

    @Nullable
    Slot slot(PartPicker.Kind kind, int index) {
        for (Slot slot : slots) {
            if (slot.kind() == kind && slot.index() == index) return slot;
        }
        return null;
    }

    @Nullable
    static String partIn(SpellPage page, Slot slot) {
        List<String> parts = switch (slot.kind()) {
            case FORM -> page.form().isEmpty() ? List.of() : List.of(page.form());
            case HEART -> page.hearts();
            case TOP -> page.tops();
        };
        return slot.index() < parts.size() ? parts.get(slot.index()) : null;
    }

    @Nullable
    static Component layerRefusal(Player player, TetradDraft draft, PartPicker.Kind kind) {
        return switch (kind) {
            case FORM -> draft.layerRefusal(player, PageRules.BASE_NODE);
            case HEART -> draft.layerRefusal(player, PageRules.HEART_NODE);
            case TOP -> null;
        };
    }

    void render(GuiGraphics graphics, Font font, Player player, TetradDraft draft, PartPicker picker, int mouseX, int mouseY) {
        label(graphics, font, PartPicker.Kind.TOP, top);
        label(graphics, font, PartPicker.Kind.HEART, top + STEP);
        label(graphics, font, PartPicker.Kind.FORM, top + 2 * STEP);
        SpellPage page = draft.page();
        for (Slot slot : slots) {
            String part = partIn(page, slot);
            boolean locked = part == null && layerRefusal(player, draft, slot.kind()) != null;
            ResourceLocation frame = picker.picks(slot.kind(), slot.index()) ? TetradArt.SLOT_PICKED
                    : locked ? TetradArt.SLOT_LOCKED : at(mouseX, mouseY) == slot ? TetradArt.SLOT_HOVER : TetradArt.SLOT;
            graphics.blitSprite(frame, slot.x(), slot.y(), slot.width(), TetradArt.CELL);
            if (slot.kind() == PartPicker.Kind.FORM && page.isSeal()) {
                wide(graphics, font, TetradArt.SEAL, sealName(page), slot);
            } else if (part != null && slot.kind() == PartPicker.Kind.FORM) {
                wide(graphics, font, TetradArt.form(part), PageLabels.form(part), slot);
            } else if (part != null) {
                graphics.blitSprite(PartPicker.icon(slot.kind(), part), slot.x() + 2, slot.y() + 2, TetradArt.ICON, TetradArt.ICON);
            }
            if (locked) graphics.blitSprite(TetradArt.LOCK, slot.x() + (slot.width() - 7) / 2, slot.y() + 6, 7, 8);
        }
    }

    @Nullable
    List<FormattedCharSequence> tooltip(Font font, Player player, TetradDraft draft, double mouseX, double mouseY) {
        Slot slot = at(mouseX, mouseY);
        if (slot == null) return null;
        SpellPage page = draft.page();
        if (slot.kind() == PartPicker.Kind.FORM && page.isSeal()) {
            return PartInfo.text(font, Component.translatable("quasium.tetrad.seal"), Component.translatable("quasium.tetrad.seal.hint"));
        }
        String part = partIn(page, slot);
        if (part != null) {
            List<FormattedCharSequence> lines = new ArrayList<>(PartPicker.info(font, slot.kind(), part, null));
            lines.add(Component.translatable("quasium.tetrad.slot.change").withStyle(ChatFormatting.DARK_GRAY).getVisualOrderText());
            return lines;
        }
        Component refusal = layerRefusal(player, draft, slot.kind());
        Component title = layerName(slot.kind());
        return PartInfo.text(font, title, refusal != null ? refusal : Component.translatable("quasium.tetrad.slot.pick"));
    }

    static int freeIndex(SpellPage page, Slot slot) {
        int filled = switch (slot.kind()) {
            case FORM -> 0;
            case HEART -> page.hearts().size();
            case TOP -> page.tops().size();
        };
        return Math.min(slot.index(), filled);
    }

    private static Component layerName(PartPicker.Kind kind) {
        return Component.translatable("quasium.tetrad.layer." + switch (kind) {
            case FORM -> "base";
            case HEART -> "heart";
            case TOP -> "top";
        });
    }

    private static Component sealName(SpellPage page) {
        ResourceLocation seal = ResourceLocation.tryParse(page.seal());
        return seal == null ? Component.literal(page.seal()) : Spell.displayName(seal);
    }

    private void label(GuiGraphics graphics, Font font, PartPicker.Kind kind, int y) {
        graphics.drawString(font, layerName(kind), left, y + 6, TetradArt.INK, false);
    }

    private static void wide(GuiGraphics graphics, Font font, ResourceLocation icon, Component name, Slot slot) {
        graphics.blitSprite(icon, slot.x() + 2, slot.y() + 2, TetradArt.ICON, TetradArt.ICON);
        String text = font.plainSubstrByWidth(name.getString(), slot.width() - TetradArt.ICON - 8);
        graphics.drawString(font, text, slot.x() + TetradArt.ICON + 5, slot.y() + 6, TetradArt.INK, false);
    }
}
