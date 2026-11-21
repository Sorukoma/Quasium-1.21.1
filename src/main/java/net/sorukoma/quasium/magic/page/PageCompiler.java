package net.sorukoma.quasium.magic.page;

import com.google.common.hash.Hashing;
import net.minecraft.resources.ResourceLocation;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.magic.FocusUpgrade;
import net.sorukoma.quasium.magic.NoteType;
import net.sorukoma.quasium.magic.Spell;
import net.sorukoma.quasium.magic.SpellForm;
import net.sorukoma.quasium.magic.Spells;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class PageCompiler {
    public static final String POWER = "page_power";
    public static final String SECONDS = "seconds_scale";
    private static final String POWER_KEY = "power";
    private static final String SECONDS_KEY = "seconds";

    private PageCompiler() {
    }

    public static Optional<CompiledPage> compile(SpellPage page, int overflowStep, @Nullable FocusUpgrade upgrade) {
        if (page.isEmpty()) return Optional.empty();
        ResourceLocation id = idOf(page, overflowStep, page.isSeal() ? null : upgrade);
        CompiledPage cached = PageSpells.get(id);
        if (cached != null) return Optional.of(cached);
        Optional<CompiledPage> compiled = page.isSeal() ? seal(id, page, overflowStep) : composed(id, page, overflowStep, upgrade);
        compiled.ifPresent(PageSpells::put);
        return compiled;
    }

    private static Optional<CompiledPage> composed(ResourceLocation id, SpellPage page, int overflowStep, @Nullable FocusUpgrade upgrade) {
        PageCatalog catalog = PageCatalog.get();
        PageForm form = catalog.form(page.form());
        if (form == null) return Optional.empty();
        List<PageEffect> hearts = new ArrayList<>();
        for (String heart : page.hearts()) {
            PageEffect effect = catalog.effect(heart);
            if (effect == null) return Optional.empty();
            hearts.add(effect);
        }
        if (hearts.isEmpty() && form.shape() != SpellForm.SPIRIT) return Optional.empty();
        Map<String, Integer> paid = countTops(page.tops(), page.form(), catalog);
        if (paid == null) return Optional.empty();
        Map<String, Integer> all = withFreeTop(paid, freeTop(upgrade), page.form(), catalog);

        int volume = form.volume() + hearts.stream().mapToInt(PageEffect::volume).sum();
        int extraNotes = 0;
        for (Map.Entry<String, Integer> top : paid.entrySet()) {
            PageTop definition = catalog.top(top.getKey());
            volume += definition.volume() * top.getValue();
            extraNotes += definition.noteCost() * top.getValue();
        }
        Map<String, Float> params = new HashMap<>(form.params());
        applyTops(params, all, catalog, false);
        applyOverflow(params, page.overflow(), all, overflowStep, catalog, false);

        List<NoteType> heartNotes = hearts.stream().map(PageEffect::note).toList();
        Set<NoteType> notes = heartNotes.isEmpty() ? Set.of() : EnumSet.copyOf(heartNotes);
        float cooldown = (float) hearts.stream().mapToDouble(PageEffect::cooldown).max().orElse(0.0);
        Spell spell = new Spell(form.shape(), List.of(), volume, nominalGrams(volume), cooldown, 0, false, "page",
                Map.copyOf(params), colorOf(heartNotes));
        return Optional.of(new CompiledPage(id, page, spell, page.hearts(), Map.copyOf(all), accordsOf(notes, catalog), notes,
                PageMath.noteCost(heartNotes, extraNotes), volume, null));
    }

    private static Optional<CompiledPage> seal(ResourceLocation id, SpellPage page, int overflowStep) {
        ResourceLocation legacyId = ResourceLocation.tryParse(page.seal());
        Spell legacy = legacyId == null ? null : Spells.registered(legacyId);
        if (legacy == null) return Optional.empty();
        PageCatalog catalog = PageCatalog.get();
        Map<String, Integer> tops = countTops(page.tops(), "seal", catalog);
        if (tops == null) return Optional.empty();
        int volume = legacy.lungs();
        for (Map.Entry<String, Integer> top : tops.entrySet()) volume += catalog.top(top.getKey()).volume() * top.getValue();
        Map<String, Float> params = new HashMap<>(legacy.params());
        applyTops(params, tops, catalog, true);
        applyOverflow(params, page.overflow(), tops, overflowStep, catalog, true);
        Spell spell = new Spell(legacy.form(), legacy.inputs(), volume, legacy.grams(), legacy.cooldown(), legacy.kaif(),
                legacy.hidden(), legacy.action(), Map.copyOf(params), legacy.color());
        return Optional.of(new CompiledPage(id, page, spell, List.of(), Map.copyOf(tops), List.of(), Set.of(), Map.of(), volume, legacyId));
    }

    @Nullable
    static Map<String, Integer> countTops(List<String> tops, String form, PageCatalog catalog) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String top : tops) {
            PageTop definition = catalog.top(top);
            if (definition == null || !definition.fits(form)) return null;
            int count = counts.merge(top, 1, Integer::sum);
            if (count > definition.max()) return null;
        }
        return counts;
    }

    private static Map<String, Integer> withFreeTop(Map<String, Integer> paid, @Nullable String free, String form, PageCatalog catalog) {
        if (free == null) return paid;
        PageTop definition = catalog.top(free);
        if (definition == null || !definition.fits(form) || paid.getOrDefault(free, 0) >= definition.max()) return paid;
        Map<String, Integer> all = new LinkedHashMap<>(paid);
        all.merge(free, 1, Integer::sum);
        return all;
    }

    @Nullable
    private static String freeTop(@Nullable FocusUpgrade upgrade) {
        if (upgrade == null) return null;
        return switch (upgrade) {
            case SPLITTER -> "fan";
            case RESONATOR -> "chain";
            case COLLIMATOR -> "reach";
            default -> null;
        };
    }

    private static void applyTops(Map<String, Float> params, Map<String, Integer> tops, PageCatalog catalog, boolean legacy) {
        for (Map.Entry<String, Integer> entry : tops.entrySet()) {
            PageTop top = catalog.top(entry.getKey());
            int count = entry.getValue();
            top.scale().forEach((key, scale) -> scale(params, key, PageMath.stackedScale(scale, count), legacy));
            top.add().forEach((key, add) -> params.merge(key, add * count, Float::sum));
        }
    }

    private static void applyOverflow(Map<String, Float> params, String target, Map<String, Integer> tops, int step,
                                      PageCatalog catalog, boolean legacy) {
        if (step <= 0) return;
        float multiplier = PageMath.overflowMultiplier(step, PageConfig.OVERFLOW_STEP_PCT.getAsInt());
        PageTop top = tops.containsKey(target) ? catalog.top(target) : null;
        if (top == null || top.scale().isEmpty()) {
            scale(params, POWER_KEY, multiplier, legacy);
            return;
        }
        top.scale().keySet().forEach(key -> scale(params, key, multiplier, legacy));
    }

    private static void scale(Map<String, Float> params, String key, float multiplier, boolean legacy) {
        if (POWER_KEY.equals(key)) {
            params.merge(POWER, multiplier, (a, b) -> a * b);
            return;
        }
        if (SECONDS_KEY.equals(key)) {
            if (!legacy) {
                params.merge(SECONDS, multiplier, (a, b) -> a * b);
                return;
            }
            params.replaceAll((name, value) -> name.endsWith("_seconds") ? value * multiplier : value);
            return;
        }
        params.computeIfPresent(key, (name, value) -> value * multiplier);
    }

    private static List<String> accordsOf(Set<NoteType> notes, PageCatalog catalog) {
        List<NoteType> sorted = new ArrayList<>(notes);
        List<String> accords = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            for (int j = i + 1; j < sorted.size(); j++) {
                String accord = catalog.accordOf(sorted.get(i), sorted.get(j));
                if (accord != null) accords.add(accord);
            }
        }
        return List.copyOf(accords);
    }

    private static float nominalGrams(int volume) {
        return Math.min(64f, (float) (volume * PageConfig.GRAMS_PER_ML.get()));
    }

    private static int colorOf(List<NoteType> notes) {
        if (notes.isEmpty()) return 0xCCCCCC;
        int r = 0;
        int g = 0;
        int b = 0;
        for (NoteType note : notes) {
            r += note.color() >> 16 & 0xFF;
            g += note.color() >> 8 & 0xFF;
            b += note.color() & 0xFF;
        }
        int n = notes.size();
        return (r / n) << 16 | (g / n) << 8 | (b / n);
    }

    private static ResourceLocation idOf(SpellPage page, int overflowStep, @Nullable FocusUpgrade upgrade) {
        String key = page.canonical() + "|" + (upgrade == null ? "" : upgrade.id());
        String hash = Long.toHexString(Hashing.murmur3_128().hashString(key, StandardCharsets.UTF_8).asLong());
        String suffix = overflowStep > 0 ? "_o" + overflowStep : "";
        return ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, PageSpells.PREFIX + hash + suffix);
    }
}
