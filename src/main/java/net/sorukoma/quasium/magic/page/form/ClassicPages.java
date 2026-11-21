package net.sorukoma.quasium.magic.page.form;

import net.minecraft.resources.ResourceLocation;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.magic.page.SpellPage;
import net.sorukoma.quasium.magic.page.TetradContents;

import java.util.List;
import java.util.stream.Stream;

public final class ClassicPages {
    public static final SpellPage FROST_BREATH = composed("frost_breath", "cone", List.of("frost", "soak"), List.of());
    public static final SpellPage EMBER_BREATH = composed("ember_breath", "beam", List.of("heat"), List.of());

    public static final List<SpellPage> COMPOSED = List.of(
            FROST_BREATH,
            EMBER_BREATH,
            composed("healing_steam", "cloud", List.of("healing", "soak"), List.of()),
            composed("smoke_screen", "cloud", List.of("blind", "soak"), List.of("length")),
            composed("heavy_smoke", "cloud", List.of("heaviness", "blind"), List.of()),
            composed("whisper_of_coals", "wave", List.of("whisper"), List.of()),
            composed("ring_trap", "ring", List.of("shackles"), List.of()),
            composed("sprout", "cone", List.of("growth", "soak"), List.of()));

    public static final List<SpellPage> SEALS = Stream.of("ring_shield", "ring_portal", "smoke_step", "waking_dream", "fume",
                    "delirium_breath", "delocalization")
            .map(spell -> seal(ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, spell)))
            .toList();

    private ClassicPages() {
    }

    public static SpellPage seal(ResourceLocation spell) {
        return new SpellPage(label(spell), "", List.of(), List.of(), "", spell.toString());
    }

    public static TetradContents starter(int size) {
        return TetradContents.empty(size).with(0, FROST_BREATH).with(1, EMBER_BREATH);
    }

    static String label(ResourceLocation spell) {
        return "@" + spell;
    }

    private static SpellPage composed(String spell, String form, List<String> hearts, List<String> tops) {
        return new SpellPage(label(ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, spell)), form, hearts, tops, "", "");
    }
}
