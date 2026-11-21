package net.sorukoma.quasium.ritual;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.sorukoma.quasium.progression.ConfigArgs;

public final class RitualConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    static {
        BUILDER.comment("Ritual brazier: structure, participants, group").push("brazier");
    }

    public static final ModConfigSpec.IntValue JAR_RADIUS = BUILDER
            .comment("Note jars this close to the brazier feed its ritual")
            .defineInRange("jarRadius", 8, 1, 32);
    public static final ModConfigSpec.IntValue PEDESTAL_MIN_DISTANCE = BUILDER
            .comment("Closest a pedestal may stand to the brazier, blocks")
            .defineInRange("pedestalMinDistance", 2, 1, 8);
    public static final ModConfigSpec.IntValue PEDESTAL_MAX_DISTANCE = BUILDER
            .comment("Farthest a pedestal may stand from the brazier, blocks")
            .defineInRange("pedestalMaxDistance", 3, 1, 8);
    public static final ModConfigSpec.IntValue MAX_PEDESTALS = BUILDER
            .comment("Pedestals one brazier counts")
            .defineInRange("maxPedestals", 8, 0, 8);
    public static final ModConfigSpec.IntValue PARTICIPANT_RADIUS = BUILDER
            .comment("Players this close to the brazier take part: their average kaif counts and they see the ritual bar")
            .defineInRange("participantRadius", 8, 2, 32);
    public static final ModConfigSpec.IntValue GROUP_PLAYERS = BUILDER
            .comment("Different players who must hit the beat of the brazier inside the group window for the group bonus")
            .defineInRange("groupPlayers", 3, 2, 16);
    public static final ModConfigSpec.IntValue GROUP_WINDOW_SEC = BUILDER
            .comment("Group bonus window, seconds")
            .defineInRange("groupWindowSec", 30, 5, 300);
    public static final ModConfigSpec.IntValue NIRVANA_TIER = BUILDER
            .comment("Rituals of this tier and higher start only while a participant stands in a Nirvana zone")
            .defineInRange("nirvanaTier", 4, 1, 5);

    static {
        BUILDER.pop();
        BUILDER.comment("Rhythm: a lit brazier beats, a pipe puff started inside the window around a beat is a hit").push("rhythm");
    }

    public static final ModConfigSpec.IntValue PULSE_MS_T1 = BUILDER
            .comment("Time between beats of a tier 1 ritual, ms")
            .defineInRange("pulseMsTier1", 1500, 400, 5000);
    public static final ModConfigSpec.IntValue PULSE_MS_T2 = BUILDER
            .comment("Time between beats of a tier 2 ritual, ms")
            .defineInRange("pulseMsTier2", 1330, 400, 5000);
    public static final ModConfigSpec.IntValue PULSE_MS_T3 = BUILDER
            .comment("Time between beats of a tier 3 ritual, ms")
            .defineInRange("pulseMsTier3", 1150, 400, 5000);
    public static final ModConfigSpec.IntValue PULSE_MS_T4 = BUILDER
            .comment("Time between beats of a tier 4 ritual, ms")
            .defineInRange("pulseMsTier4", 1000, 400, 5000);
    public static final ModConfigSpec.IntValue WINDOW_MS_T1 = BUILDER
            .comment("A tier 1 puff counts as a hit this close to a beat, ms either way")
            .defineInRange("windowMsTier1", 250, 20, 1000);
    public static final ModConfigSpec.IntValue WINDOW_MS_T2 = BUILDER
            .comment("A tier 2 puff counts as a hit this close to a beat, ms either way")
            .defineInRange("windowMsTier2", 215, 20, 1000);
    public static final ModConfigSpec.IntValue WINDOW_MS_T3 = BUILDER
            .comment("A tier 3 puff counts as a hit this close to a beat, ms either way")
            .defineInRange("windowMsTier3", 185, 20, 1000);
    public static final ModConfigSpec.IntValue WINDOW_MS_T4 = BUILDER
            .comment("A tier 4 puff counts as a hit this close to a beat, ms either way")
            .defineInRange("windowMsTier4", 150, 20, 1000);
    public static final ModConfigSpec.IntValue PRESS_LAG_MS = BUILDER
            .comment("Oldest puff moment a client may report, ms behind the server clock")
            .defineInRange("pressLagMs", 500, 0, 2000);
    public static final ModConfigSpec.IntValue STREAK_HITS = BUILDER
            .comment("Hits in a row that bring the streak multiplier to its top")
            .defineInRange("streakHits", 8, 1, 64);
    public static final ModConfigSpec.IntValue STREAK_MAX_PCT = BUILDER
            .comment("Progress of one hit at the top of a streak, percent of a plain hit")
            .defineInRange("streakMaxPct", 200, 100, 1000);
    public static final ModConfigSpec.IntValue HITS_PER_100_PUFFS = BUILDER
            .comment("Plain hits a ritual needs for every 100 puffs of its data file")
            .defineInRange("hitsPer100Puffs", 16, 1, 1000);
    public static final ModConfigSpec.IntValue NETWORK_SPEED_PCT = BUILDER
            .comment("Speed of a ritual on full network draft, percent of a perfect rhythm at the top of its streak")
            .defineInRange("networkSpeedPct", 50, 0, 200);
    public static final ModConfigSpec.IntValue DRAFT_WINDOW_SEC = BUILDER
            .comment("Seconds over which the network draft of the ritual is averaged")
            .defineInRange("draftWindowSec", 5, 1, 60);

    static {
        BUILDER.pop();
        BUILDER.comment("Instability starts at the ritual base. Every beat adds (base - kaif/kaifDivisor - groupBonus - filterBonus)"
                + " / driftDivisor, a hit takes hitRelief off, a miss adds missPenalty, a beat nobody hits adds emptyPulsePenalty"
                + " times the share of draft the network does not cover").push("instability");
    }

    public static final ModConfigSpec.IntValue RITUAL_BREAK_THRESHOLD = BUILDER
            .comment("Instability at which a ritual breaks")
            .defineInRange("ritualBreakThreshold", 100, 1, 1000);
    public static final ModConfigSpec.IntValue HIT_RELIEF = BUILDER
            .comment("Instability one hit takes off")
            .defineInRange("hitRelief", 2, 0, 100);
    public static final ModConfigSpec.IntValue MISS_PENALTY = BUILDER
            .comment("Instability one puff off the beat adds")
            .defineInRange("missPenalty", 3, 0, 100);
    public static final ModConfigSpec.IntValue EMPTY_PULSE_PENALTY = BUILDER
            .comment("Instability a beat nobody hits adds when the network gives no draft")
            .defineInRange("emptyPulsePenalty", 1, 0, 100);
    public static final ModConfigSpec.IntValue DRIFT_DIVISOR = BUILDER
            .comment("The base pressure of a ritual divided by this is added on every beat")
            .defineInRange("driftDivisor", 36, 1, 1000);
    public static final ModConfigSpec.DoubleValue STARVE_PENALTY = BUILDER
            .comment("Instability every beat adds while the jars are dry and the ritual waits")
            .defineInRange("starvePenalty", 0.5, 0.0, 20.0);
    public static final ModConfigSpec.IntValue KAIF_DIVISOR = BUILDER
            .comment("Average kaif of the participants divided by this lowers the base pressure")
            .defineInRange("kaifDivisor", 20, 1, 100);
    public static final ModConfigSpec.IntValue GROUP_BONUS = BUILDER
            .comment("Base pressure removed by the group bonus")
            .defineInRange("groupBonus", 20, 0, 200);
    public static final ModConfigSpec.IntValue FILTER_BONUS = BUILDER
            .comment("Base pressure removed by a Filter upgrade set into the brazier")
            .defineInRange("filterBonus", 10, 0, 200);

    static {
        BUILDER.pop();
        BUILDER.comment("A broken ritual: items drop, the smoke hurts the mood").push("breakdown");
    }

    public static final ModConfigSpec.IntValue FAIL_KAIF_LOSS = BUILDER
            .comment("Kaif every participant loses when a ritual breaks")
            .defineInRange("failKaifLoss", 10, 0, 100);
    public static final ModConfigSpec.IntValue FAIL_DENSITY = BUILDER
            .comment("Smoke density added to the chunk of a broken ritual")
            .defineInRange("failDensity", 10, 0, 100);
    public static final ModConfigSpec.BooleanValue FAIL_SPAWNS_DYMNIK = BUILDER
            .comment("A broken ritual lets out a Dymnik when that mob exists")
            .define("failSpawnsDymnik", true);
    public static final ModConfigSpec.IntValue FAIL_NOTE_RETURN_PCT = BUILDER
            .comment("Share of the notes a broken ritual already drew that flows back into the jars nearby, percent")
            .defineInRange("failNoteReturnPct", 50, 0, 100);

    static {
        BUILDER.pop();
        BUILDER.comment("Artifacts").push("artifacts");
    }

    public static final ModConfigSpec.IntValue ETERNAL_COAL_GRAMS_PCT = BUILDER
            .comment("Grams written to the overdose window while smoking on the Eternal Coal, percent")
            .defineInRange("eternalCoalGramsPct", 115, 100, 300);
    public static final ModConfigSpec.IntValue BOTTOMLESS_FLASK_KAIF_PER_CAST = BUILDER
            .comment("Kaif one cast costs instead of tobacco while the Bottomless Flask is in the inventory")
            .defineInRange("bottomlessFlaskKaifPerCast", 2, 0, 20);
    public static final ModConfigSpec.IntValue VOID_PIPE_LOW_BOWL_GRAMS = BUILDER
            .comment("Below this many grams in the bowl every cast through the Void Pipe costs health")
            .defineInRange("voidPipeLowBowlGrams", 5, 0, 20);
    public static final ModConfigSpec.IntValue VOID_PIPE_HP_PER_CAST = BUILDER
            .comment("Health one cast through the Void Pipe costs on a low bowl")
            .defineInRange("voidPipeHpPerCast", 1, 0, 20);
    public static final ModConfigSpec.IntValue VOID_PIPE_END_CITY_PCT = BUILDER
            .comment("Chance of a Void Pipe in an End City chest, percent")
            .defineInRange("voidPipeEndCityPct", 10, 0, 100);
    public static final ModConfigSpec.IntValue SIGNAL_PIPE_HEIGHT = BUILDER
            .comment("Height of the Signal Pipe smoke column, blocks")
            .defineInRange("signalPipeHeight", 64, 8, 256);
    public static final ModConfigSpec.IntValue SIGNAL_PIPE_MAX_CHARS = BUILDER
            .comment("Longest Signal Pipe message")
            .defineInRange("signalPipeMaxChars", 32, 1, 256);
    public static final ModConfigSpec.IntValue SIGNAL_PIPE_COOLDOWN_SEC = BUILDER
            .comment("Signal Pipe cooldown, seconds")
            .defineInRange("signalPipeCooldownSec", 300, 0, 3600);
    public static final ModConfigSpec.IntValue SIGNAL_PIPE_COLUMN_SEC = BUILDER
            .comment("How long the Signal Pipe smoke column stands, seconds")
            .defineInRange("signalPipeColumnSec", 10, 1, 120);
    public static final ModConfigSpec.IntValue DREAM_CATCHER_ML_PER_NIGHT = BUILDER
            .comment("Dream essence every sleeper nearby gives the Dream Catcher for one night")
            .defineInRange("dreamCatcherMlPerNight", 1, 0, 20);
    public static final ModConfigSpec.IntValue DREAM_CATCHER_RADIUS = BUILDER
            .comment("Sleepers this close to the Dream Catcher are caught")
            .defineInRange("dreamCatcherRadius", 8, 1, 32);
    public static final ModConfigSpec.IntValue DREAM_CATCHER_CAP_ML = BUILDER
            .comment("Dream essence the Dream Catcher holds")
            .defineInRange("dreamCatcherCapMl", 20, 1, 1000);

    static {
        BUILDER.pop();
        BUILDER.comment("Quasium Harness on the backpack hookah").push("harness");
    }

    public static final ModConfigSpec.IntValue HARNESS_LUNG_PCT = BUILDER
            .comment("Lung capacity while smoking the harnessed backpack hookah, percent of normal")
            .defineInRange("harnessLungPct", 200, 100, 400);
    public static final ModConfigSpec.IntValue HARNESS_RING_COOLDOWN_PCT = BUILDER
            .comment("Cooldown of ring spells cast from the harnessed backpack hookah, percent of normal")
            .defineInRange("harnessRingCooldownPct", 50, 0, 100);
    public static final ModConfigSpec.IntValue HARNESS_LINK_RADIUS = BUILDER
            .comment("A running brazier this close counts the puffs of a harnessed backpack hookah")
            .defineInRange("harnessLinkRadius", 8, 1, 32);

    static {
        BUILDER.pop();
        BUILDER.comment("Smoke armor").push("armor");
    }

    public static final ModConfigSpec.IntValue CARAPACE_CHARGE_CAP = BUILDER
            .comment("Damage the Smoke Carapace soaks before it lets out the nova")
            .defineInRange("carapaceChargeCap", 20, 1, 200);
    public static final ModConfigSpec.IntValue CARAPACE_NOVA_PCT = BUILDER
            .comment("Nova damage as percent of the soaked charge")
            .defineInRange("carapaceNovaPct", 50, 0, 200);
    public static final ModConfigSpec.IntValue CARAPACE_NOVA_RADIUS = BUILDER
            .comment("Nova radius, blocks")
            .defineInRange("carapaceNovaRadius", 3, 1, 16);
    public static final ModConfigSpec.IntValue SMOKESTEP_DASH_COOLDOWN_SEC = BUILDER
            .comment("Smokestep Boots air dash cooldown, seconds")
            .defineInRange("smokestepDashCooldownSec", 20, 1, 600);
    public static final ModConfigSpec.DoubleValue SMOKESTEP_DASH_SPEED = BUILDER
            .comment("Forward push of the air dash, blocks per tick")
            .defineInRange("smokestepDashSpeed", 1.2, 0.0, 4.0);
    public static final ModConfigSpec.DoubleValue SMOKESTEP_DASH_LIFT = BUILDER
            .comment("Upward push of the air dash, blocks per tick")
            .defineInRange("smokestepDashLift", 0.45, 0.0, 2.0);
    public static final ModConfigSpec.IntValue SMOKESTEP_FALL_DAMAGE_PCT = BUILDER
            .comment("Fall damage taken in Smokestep Boots, percent")
            .defineInRange("smokestepFallDamagePct", 50, 0, 100);
    public static final ModConfigSpec.IntValue SMOKE_THREAD_DODGE_PCT = BUILDER
            .comment("Dodge chance every smoke-threaded piece adds to the full quasium set, percent")
            .defineInRange("smokeThreadDodgePct", 5, 0, 50);
    public static final ModConfigSpec.IntValue SMOKE_THREAD_DODGE_CAP_PCT = BUILDER
            .comment("Highest dodge chance of the quasium set, percent")
            .defineInRange("smokeThreadDodgeMaxPct", 45, 0, 100);
    public static final ModConfigSpec.IntValue SMOKE_TEMPER_DURABILITY_PCT = BUILDER
            .comment("Max durability a smoke tempered item gains, percent of its base durability")
            .defineInRange("smokeTemperDurabilityPct", 50, 0, 1000);

    static {
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    private RitualConfig() {
    }

    public static int pulseMs(int tier) {
        return ConfigArgs.synced(byTier(tier, PULSE_MS_T1, PULSE_MS_T2, PULSE_MS_T3, PULSE_MS_T4));
    }

    public static int windowMs(int tier) {
        return ConfigArgs.synced(byTier(tier, WINDOW_MS_T1, WINDOW_MS_T2, WINDOW_MS_T3, WINDOW_MS_T4));
    }

    private static ModConfigSpec.IntValue byTier(int tier, ModConfigSpec.IntValue... values) {
        return values[Math.clamp(tier, 1, values.length) - 1];
    }
}
