package net.sorukoma.quasium.magic.page.form;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class FormConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.DoubleValue VEIL_DEPTH = BUILDER
            .comment("Thickness of a smoke veil in blocks")
            .defineInRange("veilDepth", 1.5, 0.5, 8.0);

    public static final ModConfigSpec.IntValue VEIL_PROJECTILE_KEEP_PCT = BUILDER
            .comment("Percent of its speed a projectile keeps every tick it spends in a smoke veil")
            .defineInRange("veilProjectileKeepPct", 25, 0, 100);

    public static final ModConfigSpec.DoubleValue VEIL_SIGHT_REACH = BUILDER
            .comment("Blocks around a smoke veil in which mobs lose a target hidden in or behind it")
            .defineInRange("veilSightReach", 24.0, 1.0, 64.0);

    public static final ModConfigSpec.DoubleValue LURK_REACH = BUILDER
            .comment("Blocks from a lurking bolt or veil at which a foe wakes it; a lurking cloud wakes on a foe inside its radius")
            .defineInRange("lurkReach", 3.0, 0.5, 16.0);

    public static final ModConfigSpec.IntValue ECHO_DELAY_TICKS = BUILDER
            .comment("Ticks between an exhale and its echo")
            .defineInRange("echoDelayTicks", 20, 1, 200);

    public static final ModConfigSpec.IntValue ECHO_POWER_PCT = BUILDER
            .comment("Power of an echo, percent of the exhale it repeats")
            .defineInRange("echoPowerPct", 50, 1, 100);

    public static final ModConfigSpec.DoubleValue HOMING_REACH = BUILDER
            .comment("Blocks in which a homing ring looks for a foe")
            .defineInRange("homingReach", 10.0, 1.0, 32.0);

    public static final ModConfigSpec.IntValue HOMING_TURN_PCT = BUILDER
            .comment("Percent of the way to its foe a homing ring turns every tick")
            .defineInRange("homingTurnPct", 12, 1, 100);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private FormConfig() {
    }
}
