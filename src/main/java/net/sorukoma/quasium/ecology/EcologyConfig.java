package net.sorukoma.quasium.ecology;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class EcologyConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue DENSITY_DECAY_PER_MINUTE = BUILDER
            .comment("Smoke density every chunk loses per minute")
            .defineInRange("densityDecayPerMinute", 1, 0, 100);

    public static final ModConfigSpec.IntValue ZONE_DENSITY_PER_MINUTE = BUILDER
            .comment("Smoke density an active kaif zone adds to its chunk per minute")
            .defineInRange("zoneDensityPerMinute", 2, 0, 100);

    public static final ModConfigSpec.IntValue RITUAL_DENSITY = BUILDER
            .comment("Smoke density a finished brazier ritual adds to its chunk. A broken one adds failDensity from the ritual config")
            .defineInRange("ritualDensity", 10, 0, 100);

    public static final ModConfigSpec.DoubleValue TREE_DENSITY_PER_MINUTE = BUILDER
            .comment("Smoke density one grown tree removes from its chunk per minute")
            .defineInRange("treeDensityPerMinute", 0.25, 0.0, 10.0);

    public static final ModConfigSpec.IntValue TREE_DENSITY_CAP_PER_MINUTE = BUILDER
            .comment("Most smoke density the trees of one chunk remove per minute")
            .defineInRange("treeDensityCapPerMinute", 2, 0, 100);

    public static final ModConfigSpec.IntValue BONUS_THRESHOLD = BUILDER
            .comment("Chunk smoke density from which the aura bonuses work")
            .defineInRange("densityBonusThreshold", 40, 10, 90);

    public static final ModConfigSpec.IntValue BONUS_KAIF_PCT = BUILDER
            .comment("Extra kaif gain in a chunk over the bonus threshold, percent")
            .defineInRange("densityBonusKaifPct", 15, 0, 100);

    public static final ModConfigSpec.IntValue BONUS_HOLD_PCT = BUILDER
            .comment("Faster hold charge in a chunk over the bonus threshold, percent")
            .defineInRange("densityBonusHoldPct", 10, 0, 100);

    public static final ModConfigSpec.IntValue BONUS_CUBE_PCT = BUILDER
            .comment("Extra distillation cube output in a chunk over the bonus threshold, percent")
            .defineInRange("densityBonusCubePct", 10, 0, 100);

    public static final ModConfigSpec.IntValue HAZE_THRESHOLD = BUILDER
            .comment("Chunk smoke density from which the air turns hazy")
            .defineInRange("densityHazeThreshold", 80, 10, 100);

    public static final ModConfigSpec.IntValue SMOG_TINT_PCT = BUILDER
            .comment("Strength of the haze and smog tint on screen, grass and leaves, percent. 0 turns it off")
            .defineInRange("smogTintPct", 100, 0, 100);

    static {
        BUILDER.comment("Smog: onset, fading, spreading, effects, cleansing").push("smog");
    }

    public static final ModConfigSpec.IntValue SMOG_ONSET_MINUTES = BUILDER
            .comment("Minutes a chunk has to stay at full density before its smog grows by one")
            .defineInRange("smogOnsetMinutes", 5, 1, 30);

    public static final ModConfigSpec.IntValue SMOG_RESET_DENSITY = BUILDER
            .comment("Smoke density a chunk drops to when its smog grows")
            .defineInRange("smogResetDensity", 60, 0, 100);

    public static final ModConfigSpec.IntValue SMOG_FADE_MINUTES = BUILDER
            .comment("Minutes of clean air (density 0) after which smog drops by one")
            .defineInRange("smogFadeMinutes", 10, 1, 120);

    public static final ModConfigSpec.IntValue SMOG_SPREAD_PCT_PER_LEVEL = BUILDER
            .comment("Chance per minute and per smog level that smog drifts one level into the downwind chunk, percent")
            .defineInRange("smogSpreadPctPerLevel", 5, 0, 20);

    public static final ModConfigSpec.IntValue SMOG_SPREAD_NEIGHBOURS_AT_MAX = BUILDER
            .comment("Neighbour chunks the heaviest smog drifts into at once")
            .defineInRange("smogSpreadNeighboursAtMax", 2, 1, 4);

    public static final ModConfigSpec.IntValue SMOG_NAUSEA_SECONDS = BUILDER
            .comment("Seconds of nausea a player gets every minute in smog of level 2 and more")
            .defineInRange("smogNauseaSeconds", 3, 0, 60);

    public static final ModConfigSpec.IntValue VILLAGER_COUGH_SMOG = BUILDER
            .comment("Smog level from which villagers cough and refuse to trade. The hookah master is used to smoke and keeps trading")
            .defineInRange("villagerCoughSmog", 3, 1, 5);

    public static final ModConfigSpec.IntValue VENT_SMOG_MINUTES = BUILDER
            .comment("Minutes of full draft after which a ventilation removes one smog level in its 3x3 chunks")
            .defineInRange("ventSmogMinutes", 5, 1, 60);

    public static final ModConfigSpec.IntValue LAVENDER_DENSITY_PER_MINUTE = BUILDER
            .comment("Smoke density a lamp burning lavender removes per minute in each chunk of a 3x3 area")
            .defineInRange("lavenderDensityPerMinute", 2, 0, 100);

    public static final ModConfigSpec.IntValue LAVENDER_SMOG_MINUTES = BUILDER
            .comment("Minutes a lamp has to burn lavender to remove one smog level in its 3x3 chunks")
            .defineInRange("lavenderSmogMinutes", 3, 1, 60);

    static {
        BUILDER.pop();
        BUILDER.comment("Smog creatures: Smokeling and Cinderman").push("mobs");
    }

    public static final ModConfigSpec.IntValue SPAWN_INTERVAL_SECONDS = BUILDER
            .comment("Seconds between spawn attempts in every smog chunk near a player")
            .defineInRange("smogSpawnIntervalSeconds", 30, 5, 600);

    public static final ModConfigSpec.IntValue DYMNIK_HEALTH = BUILDER
            .comment("Health of a Smokeling")
            .defineInRange("dymnikHealth", 8, 1, 1000);

    public static final ModConfigSpec.IntValue DYMNIK_STEAL_RADIUS = BUILDER
            .comment("Blocks from a Smokeling within which it steals kaif at night")
            .defineInRange("dymnikStealRadius", 3, 1, 16);

    public static final ModConfigSpec.IntValue DYMNIK_STEAL_KAIF_PER_SECOND = BUILDER
            .comment("Kaif a Smokeling steals per second")
            .defineInRange("dymnikStealKaifPerSecond", 1, 0, 100);

    public static final ModConfigSpec.IntValue DYMNIK_SUNBURN_SECONDS = BUILDER
            .comment("Seconds a Smokeling lasts in sunlight")
            .defineInRange("dymnikSunburnSeconds", 10, 1, 600);

    public static final ModConfigSpec.IntValue DYMNIK_ASH_ML = BUILDER
            .comment("Ash essence a Smokeling drops, ml")
            .defineInRange("dymnikAshMl", 1, 0, 20);

    public static final ModConfigSpec.IntValue DYMNIK_MIN_SMOG = BUILDER
            .comment("Smog level from which Smokelings appear at night")
            .defineInRange("dymnikMinSmog", 2, 1, 5);

    public static final ModConfigSpec.IntValue DYMNIK_CAP_PER_LEVEL = BUILDER
            .comment("Smokelings a chunk can hold for every smog level from the one they start at")
            .defineInRange("dymnikCapPerLevel", 2, 0, 16);

    public static final ModConfigSpec.IntValue GAREVSHCHIK_HEALTH = BUILDER
            .comment("Health of a Cinderman")
            .defineInRange("garevshchikHealth", 20, 1, 1000);

    public static final ModConfigSpec.IntValue GAREVSHCHIK_DAMAGE = BUILDER
            .comment("Damage of a Cinderman hit")
            .defineInRange("garevshchikDamage", 3, 0, 100);

    public static final ModConfigSpec.IntValue GAREVSHCHIK_BLINDNESS_SECONDS = BUILDER
            .comment("Seconds of blindness a Cinderman hit gives")
            .defineInRange("garevshchikBlindnessSeconds", 2, 0, 60);

    public static final ModConfigSpec.IntValue GAREVSHCHIK_EMBER_ML = BUILDER
            .comment("Ember essence a Cinderman drops, ml")
            .defineInRange("garevshchikEmberMl", 2, 0, 20);

    public static final ModConfigSpec.IntValue GAREVSHCHIK_MIN_SMOG = BUILDER
            .comment("Smog level from which a Cinderman appears")
            .defineInRange("garevshchikMinSmog", 4, 1, 5);

    public static final ModConfigSpec.IntValue GAREVSHCHIK_CAP = BUILDER
            .comment("Cindermen a smog chunk can hold")
            .defineInRange("garevshchikCap", 1, 0, 16);

    static {
        BUILDER.pop();
        BUILDER.comment("Tea: bush, samovar, cup, tea party").push("tea");
    }

    public static final ModConfigSpec.IntValue TEA_LEAVES_PER_HARVEST = BUILDER
            .comment("Tea leaves a ripe tea bush gives when picked")
            .defineInRange("teaLeavesPerHarvest", 2, 1, 16);

    public static final ModConfigSpec.IntValue TEA_DRYING_SECONDS = BUILDER
            .comment("Seconds a tea leaf dries on a drying shelf")
            .defineInRange("teaDryingSeconds", 60, 1, 3600);

    public static final ModConfigSpec.IntValue TEA_KAIF = BUILDER
            .comment("Kaif a cup of tea gives over its Vigor effect")
            .defineInRange("teaKaif", 12, 0, 100);

    public static final ModConfigSpec.IntValue VIGOR_SECONDS = BUILDER
            .comment("Seconds of Vigor after a cup of tea")
            .defineInRange("vigorSeconds", 60, 1, 3600);

    public static final ModConfigSpec.IntValue VIGOR_WITHDRAWAL_MULTIPLIER = BUILDER
            .comment("How many times faster withdrawal fades under Vigor")
            .defineInRange("vigorWithdrawalMultiplier", 2, 1, 10);

    public static final ModConfigSpec.IntValue TEA_DRINK_TICKS = BUILDER
            .comment("Ticks it takes to drink a cup of tea")
            .defineInRange("teaDrinkTicks", 32, 1, 200);

    public static final ModConfigSpec.IntValue TEAPOT_CUPS = BUILDER
            .comment("Cups in a full teapot")
            .defineInRange("teapotCups", 6, 1, 64);

    public static final ModConfigSpec.IntValue SAMOVAR_WATER_ML = BUILDER
            .comment("Water one teapot of tea takes, ml")
            .defineInRange("samovarWaterMl", 1000, 0, 16000);

    public static final ModConfigSpec.IntValue SAMOVAR_WATER_CAPACITY = BUILDER
            .comment("Water tank of the samovar, ml")
            .defineInRange("samovarWaterCapacity", 4000, 1000, 64000);

    public static final ModConfigSpec.IntValue SAMOVAR_LEAVES_PER_POT = BUILDER
            .comment("Dried tea leaves one teapot of tea takes")
            .defineInRange("samovarLeavesPerPot", 2, 1, 64);

    public static final ModConfigSpec.IntValue SAMOVAR_BREW_SECONDS = BUILDER
            .comment("Seconds the samovar brews one teapot")
            .defineInRange("samovarBrewSeconds", 30, 1, 3600);

    public static final ModConfigSpec.IntValue TEA_PARTY_CUSHIONS = BUILDER
            .comment("Lounge cushions a tea party needs around the samovar")
            .defineInRange("teaPartyCushions", 4, 0, 64);

    public static final ModConfigSpec.IntValue TEA_PARTY_RADIUS = BUILDER
            .comment("Radius of the tea party: cushions around the samovar and the tea zone, blocks")
            .defineInRange("teaPartyRadius", 6, 2, 32);

    public static final ModConfigSpec.IntValue TEA_PARTY_MINUTES = BUILDER
            .comment("Minutes the tea zone of a tea party lasts")
            .defineInRange("teaPartyMinutes", 10, 1, 120);

    public static final ModConfigSpec.IntValue TEA_PARTY_KAIF_PER_MINUTE = BUILDER
            .comment("Kaif per minute the tea zone pulls negative kaif back toward zero")
            .defineInRange("teaPartyKaifPerMinute", 2, 0, 100);

    public static final ModConfigSpec.IntValue TEA_PARTY_SMOG_CLEANSE = BUILDER
            .comment("Smog levels a tea party removes once in each chunk of a 3x3 area")
            .defineInRange("teaPartySmogCleanse", 3, 0, 5);

    static {
        BUILDER.pop();
        BUILDER.comment("Seal of sobriety").push("seal");
    }

    public static final ModConfigSpec.IntValue SEAL_RADIUS = BUILDER
            .comment("Radius of the seal of sobriety, blocks")
            .defineInRange("sealRadius", 8, 1, 64);

    public static final ModConfigSpec.BooleanValue SEAL_ALLOWS_CASTS = BUILDER
            .comment("Whether smoke casts work inside a seal of sobriety")
            .define("sealAllowsCasts", false);

    public static final ModConfigSpec.IntValue SEAL_CRAFT_MAX_KAIF = BUILDER
            .comment("Highest kaif a smith may have to forge the seal of sobriety")
            .defineInRange("sealCraftMaxKaif", 10, -100, 100);

    public static final ModConfigSpec.IntValue SEAL_ANVIL_HITS = BUILDER
            .comment("Hammer hits the seal of sobriety takes on the anvil")
            .defineInRange("sealAnvilHits", 3, 1, 64);

    static {
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    private EcologyConfig() {
    }
}
