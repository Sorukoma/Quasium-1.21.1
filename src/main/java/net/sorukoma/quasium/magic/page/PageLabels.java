package net.sorukoma.quasium.magic.page;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.sorukoma.quasium.magic.Spell;

public final class PageLabels {
    private PageLabels() {
    }

    public static Component of(String label) {
        if (label.startsWith("#")) return form(label.substring(1));
        if (label.startsWith("@")) {
            ResourceLocation seal = ResourceLocation.tryParse(label.substring(1));
            if (seal != null) return Spell.displayName(seal);
        }
        return Component.literal(label);
    }

    public static Component form(String form) {
        return Component.translatable("quasium.page.form." + form);
    }

    public static Component effect(String effect) {
        return Component.translatable("quasium.page.effect." + effect);
    }

    public static Component top(String top) {
        return Component.translatable("quasium.page.top." + top);
    }

    public static Component accord(String accord) {
        return Component.translatable("quasium.page.accord." + accord);
    }
}
