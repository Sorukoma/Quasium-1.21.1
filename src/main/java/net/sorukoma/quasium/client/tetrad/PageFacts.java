package net.sorukoma.quasium.client.tetrad;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.sorukoma.quasium.magic.NoteType;
import net.sorukoma.quasium.magic.Spell;
import net.sorukoma.quasium.magic.page.Accord;
import net.sorukoma.quasium.magic.page.CompiledPage;
import net.sorukoma.quasium.magic.page.PageCatalog;
import net.sorukoma.quasium.magic.page.PageCompiler;
import net.sorukoma.quasium.magic.page.PageEffect;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.ToDoubleFunction;

final class PageFacts {
    private static final String SECONDS = "_seconds";

    private PageFacts() {
    }

    static Component describe(CompiledPage page) {
        if (page.isSeal()) return seal(page);
        PageCatalog catalog = PageCatalog.get();
        String form = page.page().form();
        List<Component> facts = new ArrayList<>();
        float secondsScale = page.param(PageCompiler.SECONDS, 1f);
        for (String heart : page.hearts()) {
            PageEffect effect = catalog.effect(heart);
            if (effect == null) continue;
            facts.add(fact("effect", heart, PageFactArgs.EFFECTS, key -> scaled(key, effect.param(key, 0f), secondsScale)));
        }
        for (String top : page.tops().keySet()) {
            if (PageFactArgs.TOPS.containsKey(top)) facts.add(fact("top", top, PageFactArgs.TOPS, key -> page.param(key, 0f)));
        }
        float power = page.param(PageCompiler.POWER, 1f);
        if (Math.abs(power - 1f) > 0.01f) facts.add(Component.translatable("quasium.tetrad.fact.power", number(power)));
        for (String accordId : page.accords()) {
            Accord accord = catalog.accords().get(accordId);
            if (accord != null) facts.add(fact("accord", accordId, PageFactArgs.ACCORDS, key -> scaled(key, accord.param(key, 0f), 1f)));
        }
        MutableComponent text = fact("form", form, PageFactArgs.FORMS, key -> page.param(key, 0f));
        if (facts.isEmpty()) return text;
        text.append(": ");
        for (int i = 0; i < facts.size(); i++) {
            if (i > 0) text.append(", ");
            text.append(facts.get(i));
        }
        return text;
    }

    static String number(float value) {
        float rounded = Math.round(value * 10f) / 10f;
        if (rounded == Math.round(rounded)) return Integer.toString(Math.round(rounded));
        return String.format(Locale.ROOT, "%.1f", rounded);
    }

    static Component essence(String essence) {
        NoteType note = NoteType.byId(essence);
        return note != null ? note.displayName() : Component.translatable("item.quasium.essence_bottle." + essence);
    }

    private static Component seal(CompiledPage page) {
        Spell spell = page.spell();
        MutableComponent bottles = Component.empty();
        if (!spell.inputs().isEmpty()) {
            List<Map.Entry<String, Integer>> needed = new ArrayList<>(spell.inputs().getFirst().entrySet());
            needed.sort(Map.Entry.comparingByKey());
            for (int i = 0; i < needed.size(); i++) {
                if (i > 0) bottles.append(", ");
                bottles.append(essence(needed.get(i).getKey()));
            }
        }
        return Component.translatable("quasium.tetrad.fact.seal", Spell.displayName(page.legacyId()), bottles);
    }

    private static MutableComponent fact(String kind, String id, Map<String, List<String>> table, ToDoubleFunction<String> source) {
        List<String> keys = table.getOrDefault(id, List.of());
        Object[] args = new Object[keys.size()];
        for (int i = 0; i < keys.size(); i++) args[i] = number((float) source.applyAsDouble(keys.get(i)));
        return Component.translatable("quasium.tetrad.fact." + kind + "." + id, args);
    }

    private static float scaled(String key, float value, float secondsScale) {
        if (PageFactArgs.percent(key)) return value * 100f;
        return key.endsWith(SECONDS) ? value * secondsScale : value;
    }
}
