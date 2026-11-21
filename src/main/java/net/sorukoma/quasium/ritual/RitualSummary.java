package net.sorukoma.quasium.ritual;

import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class RitualSummary {
    private RitualSummary() {
    }

    public static List<Component> lines(RitualRecipe recipe) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("quasium.jei.ritual.rhythm", RitualMath.seconds(recipe.pulseMs()), recipe.cleanHits()));
        lines.add(Component.translatable("quasium.jei.ritual.network", recipe.rate()));
        lines.add(kaif(recipe.kaif()));
        lines.add(Component.translatable("quasium.jei.ritual.tier", recipe.tier(), recipe.base()));
        if (recipe.needsNirvana()) lines.add(Component.translatable("quasium.jei.ritual.nirvana"));
        return lines;
    }

    private static Component kaif(RitualRecipe.KaifGate gate) {
        if (gate.min().isEmpty() && gate.orBelow().isEmpty()) return Component.translatable("quasium.jei.ritual.kaif_any");
        if (gate.orBelow().isPresent()) return Component.translatable("quasium.jei.ritual.kaif_or_clear", gate.min().orElse(0), gate.orBelow().get());
        return Component.translatable("quasium.jei.ritual.kaif", gate.min().get());
    }
}
