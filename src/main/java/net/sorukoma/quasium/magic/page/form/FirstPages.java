package net.sorukoma.quasium.magic.page.form;

import net.minecraft.resources.ResourceLocation;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.magic.page.SpellPage;

import java.util.List;

public final class FirstPages {
    private static final String ECHO = "echo";

    public static final List<SpellPage> ALL = List.of(
            page("first_embers", "beam", List.of("heat")),
            page("first_frost", "bolt", List.of("frost")),
            page("first_ash", "cone", List.of("choke", "blind")),
            page("first_nectar", "cloud", List.of("healing")));

    private FirstPages() {
    }

    private static SpellPage page(String name, String form, List<String> hearts) {
        String label = ClassicPages.label(ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, name));
        return new SpellPage(label, form, hearts, List.of(ECHO), "", "");
    }
}
