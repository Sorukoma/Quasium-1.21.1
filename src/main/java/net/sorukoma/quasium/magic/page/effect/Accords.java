package net.sorukoma.quasium.magic.page.effect;

import net.minecraft.world.entity.LivingEntity;
import net.sorukoma.quasium.magic.page.Accord;
import net.sorukoma.quasium.magic.page.AccordBehavior;
import net.sorukoma.quasium.magic.page.PageCatalog;

import java.util.Map;
import java.util.function.BiConsumer;

public final class Accords {
    private Accords() {
    }

    public static Map<String, AccordBehavior> all() {
        return Map.ofEntries(
                Map.entry("spark", sounding(ColdAccords::spark)),
                Map.entry("lightness", sounding(ColdAccords::lightness)),
                Map.entry("rime", sounding(ColdAccords::rime)),
                Map.entry("glaze", sounding(ColdAccords::glaze)),
                Map.entry("oblivion", sounding(ColdAccords::oblivion)),
                Map.entry("caramel", sounding(FireAccords::caramel)),
                Map.entry("steam", sounding(FireAccords::steam)),
                Map.entry("magma", sounding(FireAccords::magma)),
                Map.entry("smolder", sounding(FireAccords::smolder)),
                Map.entry("balm", sounding(MistAccords::balm)),
                Map.entry("ferment", sounding(MistAccords::ferment)),
                Map.entry("fog", sounding(MistAccords::fog)),
                Map.entry("amber", sounding(BindingAccords::amber)),
                Map.entry("quagmire", sounding(BindingAccords::quagmire)),
                Map.entry("collapse", sounding(BindingAccords::collapse)));
    }

    private static AccordBehavior sounding(BiConsumer<AccordHit, LivingEntity> effect) {
        return (ctx, page, accord, target, strength) -> effect.accept(new AccordHit(ctx, page, accord, strength), target);
    }

    static float param(String accord, String key, float fallback) {
        Accord definition = PageCatalog.get().accords().get(accord);
        return definition == null ? fallback : definition.param(key, fallback);
    }
}
