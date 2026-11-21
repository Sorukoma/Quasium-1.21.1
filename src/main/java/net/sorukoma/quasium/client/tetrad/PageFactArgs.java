package net.sorukoma.quasium.client.tetrad;

import java.util.List;
import java.util.Map;

public final class PageFactArgs {
    public static final Map<String, List<String>> FORMS = Map.of(
            "bolt", List.of("range"),
            "beam", List.of("range"),
            "cone", List.of("length", "width"),
            "cloud", List.of("radius", "duration"),
            "wave", List.of("radius"),
            "self", List.of(),
            "veil", List.of("width", "height", "duration"),
            "ring", List.of("range"),
            "spirit", List.of("count", "lifetime"));

    public static final Map<String, List<String>> EFFECTS = Map.ofEntries(
            Map.entry("frost", List.of("damage", "slow_level", "slow_seconds")),
            Map.entry("gust", List.of("push")),
            Map.entry("clarity", List.of("glow_seconds")),
            Map.entry("heat", List.of("damage", "burn_seconds")),
            Map.entry("blast", List.of("damage", "radius")),
            Map.entry("whisper", List.of("radius", "glow_seconds")),
            Map.entry("healing", List.of("heal")),
            Map.entry("sticky", List.of("slow_level", "effect_seconds")),
            Map.entry("growth", List.of("crop_chance", "sapling_chance")),
            Map.entry("soak", List.of("wet_seconds")),
            Map.entry("calm", List.of("calm_seconds")),
            Map.entry("dissolve", List.of("armor", "effect_seconds")),
            Map.entry("heaviness", List.of("slow_level", "effect_seconds")),
            Map.entry("shackles", List.of("root_seconds")),
            Map.entry("carapace", List.of("absorption", "effect_seconds")),
            Map.entry("choke", List.of("damage_per_second", "choke_seconds")),
            Map.entry("blind", List.of("blind_seconds")),
            Map.entry("decay", List.of("weakness_seconds", "armor_wear")));

    public static final Map<String, List<String>> TOPS = Map.ofEntries(
            Map.entry("chain", List.of("chain")),
            Map.entry("fan", List.of("copies")),
            Map.entry("homing", List.of()),
            Map.entry("lurk", List.of("lurk")),
            Map.entry("pierce", List.of("pierce")),
            Map.entry("silence", List.of()),
            Map.entry("echo", List.of()),
            Map.entry("loyalty", List.of("loyal")),
            Map.entry("taunt", List.of("taunt")));

    public static final Map<String, List<String>> ACCORDS = Map.ofEntries(
            Map.entry("spark", List.of("jumps")),
            Map.entry("lightness", List.of("effect_seconds")),
            Map.entry("rime", List.of("freeze_seconds")),
            Map.entry("glaze", List.of("radius", "effect_seconds")),
            Map.entry("oblivion", List.of("scatter")),
            Map.entry("caramel", List.of("burn_seconds")),
            Map.entry("steam", List.of("blind_seconds")),
            Map.entry("magma", List.of("fire_seconds")),
            Map.entry("smolder", List.of("radius")),
            Map.entry("balm", List.of("radius")),
            Map.entry("amber", List.of("hold_seconds")),
            Map.entry("ferment", List.of("effect_seconds")),
            Map.entry("quagmire", List.of("slow_level", "effect_seconds")),
            Map.entry("fog", List.of("radius", "fog_seconds")),
            Map.entry("collapse", List.of("fall_multiplier")));

    private PageFactArgs() {
    }

    public static boolean percent(String param) {
        return param.endsWith("_chance");
    }
}
