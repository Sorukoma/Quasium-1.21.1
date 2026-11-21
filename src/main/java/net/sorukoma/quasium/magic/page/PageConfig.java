package net.sorukoma.quasium.magic.page;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class PageConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.DoubleValue GRAMS_PER_ML = BUILDER
            .comment("Grams of tobacco a tetrad page burns per ml of smoke actually inhaled")
            .defineInRange("gramsPerMl", 0.008, 0.0, 1.0);

    public static final ModConfigSpec.IntValue OVERFLOW_STEP_ML = BUILDER
            .comment("Ml inhaled over the page volume that make one overflow step")
            .defineInRange("overflowStepMl", 10, 1, 100);

    public static final ModConfigSpec.IntValue OVERFLOW_STEP_PCT = BUILDER
            .comment("Percent added to the overflow target per overflow step")
            .defineInRange("overflowStepPct", 10, 0, 100);

    public static final ModConfigSpec.IntValue OVERFLOW_MAX_STEPS = BUILDER
            .comment("Most overflow steps one exhale can take")
            .defineInRange("overflowMaxSteps", 5, 0, 20);

    public static final ModConfigSpec.IntValue POWER_CAP_PCT = BUILDER
            .comment("Cap of the total power of a page exhale from hold, overflow, perfect ring and zones, percent")
            .defineInRange("powerCapPct", 200, 100, 1000);

    public static final ModConfigSpec.IntValue FAN_SPREAD_DEGREES = BUILDER
            .comment("Degrees between the copies of a fanned page")
            .defineInRange("fanSpreadDegrees", 15, 1, 90);

    public static final ModConfigSpec.IntValue FAN_COPY_POWER_PCT = BUILDER
            .comment("Power of every copy of a fanned page, percent")
            .defineInRange("fanCopyPowerPct", 70, 1, 100);

    public static final ModConfigSpec.DoubleValue CHAIN_REACH = BUILDER
            .comment("Blocks a chained hit can jump to the next target")
            .defineInRange("chainReach", 6.0, 1.0, 32.0);

    public static final ModConfigSpec.IntValue CHAIN_POWER_PCT = BUILDER
            .comment("Power of every chained hit, percent of the previous one")
            .defineInRange("chainPowerPct", 60, 1, 100);

    public static final ModConfigSpec.IntValue SPIRIT_FACE_KAIF = BUILDER
            .comment("Kaif the viewer needs to see the faces of hookah spirits, a sober eye sees plain smoke")
            .defineInRange("spiritFaceKaif", 10, -100, 100);

    public static final ModConfigSpec.IntValue SPIRIT_MAX_PER_PLAYER = BUILDER
            .comment("Most hookah spirits one player keeps at once, the oldest pops when a new one goes over")
            .defineInRange("spiritMaxPerPlayer", 6, 1, 32);

    public static final ModConfigSpec.IntValue SPIRIT_LAUNCH_TICKS = BUILDER
            .comment("Ticks a fresh spirit flies straight out of the mouth before it looks for a target")
            .defineInRange("spiritLaunchTicks", 10, 0, 100);

    public static final ModConfigSpec.IntValue SPIRIT_HOMING_TURN_PCT = BUILDER
            .comment("Percent the Homing top adds to how fast a spirit turns")
            .defineInRange("spiritHomingTurnPct", 50, 0, 500);

    public static final ModConfigSpec.IntValue SPIRIT_STAR_TURN_PCT = BUILDER
            .comment("Percent every star of the spirit mix leaf adds to how fast a spirit turns")
            .defineInRange("spiritStarTurnPct", 10, 0, 100);

    public static final ModConfigSpec.IntValue SPIRIT_STAR_LIFE_SECONDS = BUILDER
            .comment("Seconds every star of the spirit mix leaf adds to the life of a spirit")
            .defineInRange("spiritStarLifeSeconds", 1, 0, 30);

    public static final ModConfigSpec.IntValue SPIRIT_RAIN_LIFE_PCT = BUILDER
            .comment("Life of a spirit out in the rain, percent")
            .defineInRange("spiritRainLifePct", 50, 1, 100);

    public static final ModConfigSpec.DoubleValue SPIRIT_ORBIT_RADIUS = BUILDER
            .comment("Blocks a waiting or loyal spirit circles around its player")
            .defineInRange("spiritOrbitRadius", 1.5, 0.5, 8.0);

    public static final ModConfigSpec.DoubleValue SPIRIT_GUARD_REACH = BUILDER
            .comment("Blocks around the player where a loyal spirit catches a flying projectile")
            .defineInRange("spiritGuardReach", 3.0, 0.5, 16.0);

    public static final ModConfigSpec.IntValue NOTE_MARK_SECONDS = BUILDER
            .comment("Seconds a hit leaves the mark of its notes on the target, for an accord with the next hit of another note")
            .defineInRange("noteMarkSeconds", 3, 1, 60);

    public static final ModConfigSpec.IntValue TOLERANCE_WINDOW_SECONDS = BUILDER
            .comment("Seconds a target remembers hits of one note for tolerance")
            .defineInRange("toleranceWindowSeconds", 10, 1, 120);

    public static final ModConfigSpec.IntValue TOLERANCE_FREE_HITS = BUILDER
            .comment("Hits of one note on one target inside the window that keep full strength")
            .defineInRange("toleranceFreeHits", 2, 0, 20);

    public static final ModConfigSpec.IntValue TOLERANCE_STEP_PCT = BUILDER
            .comment("Percent every further hit of the same note loses")
            .defineInRange("toleranceStepPct", 15, 0, 100);

    public static final ModConfigSpec.IntValue TOLERANCE_MAX_PCT = BUILDER
            .comment("Most percent a hit can lose to tolerance")
            .defineInRange("toleranceMaxPct", 50, 0, 100);

    public static final ModConfigSpec.IntValue PLAYER_CONTROL_PCT = BUILDER
            .comment("Length of shackles, amber, rime and calm on players, percent of the length on mobs")
            .defineInRange("playerControlPct", 50, 0, 100);

    public static final ModConfigSpec.IntValue BOSS_CONTROL_TICKS = BUILDER
            .comment("Longest shackles, amber, rime and calm on bosses, ticks")
            .defineInRange("bossControlTicks", 20, 0, 200);

    public static final ModConfigSpec.IntValue ACCORD_INSIGHT = BUILDER
            .comment("Insight a player gets for the first sounding of every accord")
            .defineInRange("accordInsight", 1, 0, 100);

    public static final ModConfigSpec.DoubleValue FOG_SIGHT = BUILDER
            .comment("Blocks a mob in the Fog accord can still see a target")
            .defineInRange("fogSight", 4.0, 0.0, 64.0);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private PageConfig() {
    }
}
