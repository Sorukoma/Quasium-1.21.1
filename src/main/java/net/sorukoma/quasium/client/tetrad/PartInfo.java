package net.sorukoma.quasium.client.tetrad;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.sorukoma.quasium.magic.NoteType;
import net.sorukoma.quasium.magic.page.Accord;
import net.sorukoma.quasium.magic.page.PageEffect;
import net.sorukoma.quasium.magic.page.PageForm;
import net.sorukoma.quasium.magic.page.PageLabels;
import net.sorukoma.quasium.magic.page.PageTop;
import net.sorukoma.quasium.magic.page.PageUnlocks;
import net.sorukoma.quasium.mind.client.ResearchLocks;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

final class PartInfo {
    private static final int WIDTH = 190;
    private static final String RESEARCH = "research:";
    private static final String NOTE = "note:";

    private PartInfo() {
    }

    static List<FormattedCharSequence> form(Font font, String id, PageForm form, @Nullable Component refusal) {
        return lines(font, PageLabels.form(id), List.of(volume(form.volume())), "quasium.page.form." + id + ".desc", refusal);
    }

    static List<FormattedCharSequence> effect(Font font, String id, PageEffect effect, @Nullable Component refusal) {
        List<Component> stats = List.of(volume(effect.volume()),
                Component.translatable("quasium.tetrad.stat.note", effect.note().displayName()),
                Component.translatable("quasium.tetrad.stat.cooldown", PageFacts.number(effect.cooldown())));
        return lines(font, PageLabels.effect(id).copy().withColor(effect.note().color()), stats, "quasium.page.effect." + id + ".desc", refusal);
    }

    static List<FormattedCharSequence> top(Font font, String id, PageTop top, @Nullable Component refusal) {
        List<Component> stats = new ArrayList<>();
        stats.add(top.noteCost() > 0 ? Component.translatable("quasium.tetrad.stat.volume_notes", top.volume(), top.noteCost())
                : volume(top.volume()));
        if (top.max() > 1) stats.add(Component.translatable("quasium.tetrad.stat.max", top.max()));
        if (!top.forms().isEmpty()) {
            MutableComponent forms = Component.empty();
            for (int i = 0; i < top.forms().size(); i++) {
                if (i > 0) forms.append(", ");
                forms.append(PageLabels.form(top.forms().get(i)));
            }
            stats.add(Component.translatable("quasium.tetrad.stat.forms", forms));
        }
        return lines(font, PageLabels.top(id), stats, "quasium.page.top." + id + ".desc", refusal);
    }

    static List<FormattedCharSequence> accord(Font font, String id, Accord accord) {
        Component pair = Component.translatable("quasium.tetrad.stat.pair", accord.first().displayName(), accord.second().displayName());
        return lines(font, PageLabels.accord(id).copy().withColor(accord.color()), List.of(pair), "quasium.page.accord." + id + ".desc", null);
    }

    static List<FormattedCharSequence> text(Font font, Component title, Component body) {
        List<FormattedCharSequence> lines = new ArrayList<>();
        lines.add(title.getVisualOrderText());
        lines.addAll(font.split(body.copy().withStyle(ChatFormatting.GRAY), WIDTH));
        return lines;
    }

    @Nullable
    static Component unlock(String unlock) {
        if (PageUnlocks.START.equals(unlock)) return null;
        if (PageUnlocks.FOUND.equals(unlock)) return Component.translatable("quasium.tetrad.unlock.found");
        if (unlock.startsWith(RESEARCH)) {
            ResourceLocation node = ResourceLocation.tryParse(unlock.substring(RESEARCH.length()));
            if (node != null) return ResearchLocks.needs(node);
        }
        if (unlock.startsWith(NOTE)) {
            NoteType note = NoteType.byId(unlock.substring(NOTE.length()));
            if (note != null) return Component.translatable("quasium.tetrad.unlock.note", note.displayName());
        }
        return Component.translatable("quasium.tetrad.unlock.unknown");
    }

    private static Component volume(int ml) {
        return Component.translatable("quasium.tetrad.stat.volume", ml);
    }

    private static List<FormattedCharSequence> lines(Font font, Component name, List<Component> stats, String descKey,
                                                     @Nullable Component refusal) {
        List<FormattedCharSequence> lines = new ArrayList<>();
        lines.add(name.getVisualOrderText());
        stats.forEach(stat -> lines.add(stat.copy().withStyle(ChatFormatting.GRAY).getVisualOrderText()));
        lines.addAll(font.split(Component.translatable(descKey), WIDTH));
        if (refusal != null) lines.addAll(font.split(refusal.copy().withStyle(ChatFormatting.RED), WIDTH));
        return lines;
    }
}
