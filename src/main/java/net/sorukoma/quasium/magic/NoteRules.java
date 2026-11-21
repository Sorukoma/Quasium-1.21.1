package net.sorukoma.quasium.magic;

import net.minecraft.resources.ResourceLocation;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class NoteRules {
    private static volatile List<NoteRule> rules = List.of();

    private NoteRules() {
    }

    public static void set(Map<ResourceLocation, List<NoteRule>> loaded) {
        rules = new TreeMap<>(loaded).values().stream().flatMap(List::stream)
                .sorted(Comparator.comparingInt(NoteRule::priority).reversed()).toList();
    }

    public static List<NoteRule> all() {
        return rules;
    }
}
