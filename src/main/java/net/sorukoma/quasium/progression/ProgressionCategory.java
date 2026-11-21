package net.sorukoma.quasium.progression;

import net.minecraft.resources.ResourceLocation;
import net.sorukoma.quasium.QuasiumMod;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public enum ProgressionCategory {
    START("dikar_seeds"),
    HOOKAH("hookah_pipe"),
    KAIF("kaifometer"),
    QUASIUM("quasiumingot"),
    SMOKECRAFT("note_jar"),
    DRAFT("bellows"),
    GOLEMS("clay_golem"),
    PATCH_NOTES("minecraft:writable_book"),
    RESEARCH_SMOKE("focus_upgrade_resonator", true),
    RESEARCH_NOTES("distillation_cube", true),
    RESEARCH_DRAFT("quasium_pipe", true),
    RESEARCH_SMOG("ventilation", true),
    RESEARCH_MIND("treatise", true),
    RESEARCH_DREAMS("minecraft:red_bed", true),
    RESEARCH_GOLEMS("program_card", true),
    RESEARCH_MIRROR("smokeland_hook", true);

    private final ResourceLocation icon;
    private final boolean research;

    ProgressionCategory(String icon) {
        this(icon, false);
    }

    ProgressionCategory(String icon, boolean research) {
        this.icon = icon.contains(":") ? ResourceLocation.parse(icon) : ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, icon);
        this.research = research;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String translationKey() {
        return "quasium.progression.category." + id();
    }

    public ResourceLocation icon() {
        return icon;
    }

    public boolean research() {
        return research;
    }

    public static List<ProgressionCategory> side(boolean research) {
        return Arrays.stream(values()).filter(category -> category.research == research).toList();
    }

    public static Optional<ProgressionCategory> byId(String id) {
        for (ProgressionCategory category : values()) {
            if (category.id().equals(id)) return Optional.of(category);
        }
        return Optional.empty();
    }
}
