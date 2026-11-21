package net.sorukoma.quasium.mind;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.sorukoma.quasium.progression.ConfigArgs;

import java.util.List;

public final class MindConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue TOLERANCE_CAP = BUILDER
            .comment("Highest tolerance a flavor can reach")
            .defineInRange("toleranceCap", 50, 0, 100);
    public static final ModConfigSpec.IntValue TOLERANCE_PER_SESSION = BUILDER
            .comment("Tolerance gained per smoking session of one flavor")
            .defineInRange("tolerancePerSession", 2, 0, 10);
    public static final ModConfigSpec.IntValue TOLERANCE_SESSION_GRAMS = BUILDER
            .comment("Grams of one flavor that make a session")
            .defineInRange("toleranceSessionGrams", 5, 1, 50);
    public static final ModConfigSpec.IntValue TOLERANCE_DECAY_PER_HOUR = BUILDER
            .comment("Tolerance lost per game hour (1000 ticks) by every flavor")
            .defineInRange("toleranceDecayPerHour", 1, 0, 5);

    static {
        BUILDER.comment("Research table, treatises, research nodes and the two schools").push("research");
    }

    public static final ModConfigSpec.IntValue TREATISE_INSIGHT = BUILDER
            .comment("Insight the research table takes for one treatise")
            .defineInRange("treatiseInsight", 1, 0, 20);
    public static final ModConfigSpec.IntValue TREATISE_SECONDS = BUILDER
            .comment("Seconds the research table writes one treatise")
            .defineInRange("treatiseSeconds", 10, 1, 120);
    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> TIER_TREATISES = BUILDER
            .comment("Treatises a node costs, tiers 1 to 4")
            .defineList("tierTreatises", List.of(1, 2, 3, 4), () -> 1, MindConfig::isCount);
    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> TIER_INSIGHT = BUILDER
            .comment("Insight a node costs, tiers 1 to 4")
            .defineList("tierInsight", List.of(2, 5, 10, 15), () -> 1, MindConfig::isCount);
    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> TIER_NOTE_ML = BUILDER
            .comment("Ml of every note a node asks for, tiers 1 to 4")
            .defineList("tierNoteMl", List.of(0, 20, 20, 40), () -> 0, MindConfig::isCount);
    public static final ModConfigSpec.IntValue SCHOOL_DISCOUNT_PCT = BUILDER
            .comment("Tea school: insight of every node is this much cheaper and kaif conditions are skipped; hookah school: notes are this much cheaper")
            .defineInRange("schoolDiscountPct", 20, 0, 90);
    public static final ModConfigSpec.IntValue LIVED_KAIF = BUILDER
            .comment("Kaif that counts as lived through for tier 4 nodes, besides an overdose of the second stage or any dream")
            .defineInRange("livedKaif", 80, 0, 100);
    public static final ModConfigSpec.IntValue SCHOOL_TIER = BUILDER
            .comment("Tier of the first node that asks the player to pick a school")
            .defineInRange("schoolTier", 2, 1, 4);
    public static final ModConfigSpec.IntValue REWARD_COPY_INSIGHT = BUILDER
            .comment("Insight for another copy of a book reward, such as the lo-fi disc; the first copy is free")
            .defineInRange("rewardCopyInsight", 5, 0, 100);

    static {
        BUILDER.pop();
        BUILDER.comment("Perception thresholds, item and mob scans live in the main config").push("perception");
    }

    public static final ModConfigSpec.IntValue PERCEPTION_RUINS = BUILDER
            .comment("Kaif from which a player reveals the veiled ruins of the First nearby; sets the player tag quasium.sees_ruins")
            .defineInRange("perceptionRuins", 60, 0, 100);
    public static final ModConfigSpec.IntValue PERCEPTION_MIRROR = BUILDER
            .comment("Kaif at or below which the Looking Glass branch opens; sets the player tag quasium.mirror_open")
            .defineInRange("perceptionMirror", -50, -100, 0);

    static {
        BUILDER.pop();
        BUILDER.comment("Insight drip and enlightenment").push("insight");
    }

    public static final ModConfigSpec.IntValue INSIGHT_DRIP_KAIF = BUILDER
            .comment("Kaif needed for the insight drip")
            .defineInRange("insightDripKaifThreshold", 80, 40, 100);
    public static final ModConfigSpec.IntValue INSIGHT_DRIP_KAIF_ENLIGHTENED = BUILDER
            .comment("Kaif needed for the insight drip with the Enlightened perk")
            .defineInRange("insightDripKaifEnlightened", 60, 20, 100);
    public static final ModConfigSpec.IntValue INSIGHT_DRIP_SECONDS = BUILDER
            .comment("Seconds at high kaif per dripped insight")
            .defineInRange("insightDripSeconds", 60, 1, 1200);
    public static final ModConfigSpec.IntValue INSIGHT_CAP_ENLIGHTENED = BUILDER
            .comment("Insight cap with the Enlightened perk")
            .defineInRange("insightCapEnlightened", 30, 5, 100);
    public static final ModConfigSpec.IntValue ENLIGHTENMENT_HOLD_SECONDS = BUILDER
            .comment("Seconds at kaif 100 that bring enlightenment")
            .defineInRange("enlightenmentHoldSeconds", 60, 1, 600);
    public static final ModConfigSpec.IntValue ENLIGHTENMENT_RADIUS = BUILDER
            .comment("Radius of highlighted quasium ore")
            .defineInRange("enlightenmentRadius", 32, 4, 64);
    public static final ModConfigSpec.IntValue ENLIGHTENMENT_GLOW_SECONDS = BUILDER
            .comment("How long the ore stays highlighted")
            .defineInRange("enlightenmentGlowSeconds", 30, 1, 300);
    public static final ModConfigSpec.IntValue ENLIGHTENMENT_COOLDOWN_TICKS = BUILDER
            .comment("Cooldown between two enlightenments")
            .defineInRange("enlightenmentCooldownTicks", 24000, 0, 240000);
    public static final ModConfigSpec.IntValue ENLIGHTENMENT_INSIGHT = BUILDER
            .comment("Insight given by enlightenment")
            .defineInRange("enlightenmentInsight", 5, 0, 100);
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ENLIGHTENMENT_ORES = BUILDER
            .comment("Blocks highlighted by enlightenment, block ids or #tags")
            .defineListAllowEmpty("enlightenmentOres", List.of("quasium:deepslate_quasium_ore"), () -> "", entry -> entry instanceof String);

    static {
        BUILDER.pop();
        BUILDER.comment("Perks of mastery, their prices live in the research nodes of the Kaif and Mind branch").push("perks");
    }

    public static final ModConfigSpec.IntValue IRON_LUNGS_PCT = BUILDER
            .comment("Iron Lungs: this much less air per puff and this much more lung capacity for casts")
            .defineInRange("ironLungsPct", 25, 0, 90);
    public static final ModConfigSpec.IntValue CHARCOAL_BURNER_PCT = BUILDER
            .comment("Charcoal Burner: the calloud of your hookah cools this much slower")
            .defineInRange("charcoalBurnerPct", 30, 0, 90);
    public static final ModConfigSpec.IntValue STONE_FACE_GRAMS = BUILDER
            .comment("Stone Face: every overdose threshold of the player moves up by these grams")
            .defineInRange("stoneFaceGrams", 10, 0, 100);
    public static final ModConfigSpec.IntValue ZEN_DECAY_PCT = BUILDER
            .comment("Zen: natural kaif decay is this much slower")
            .defineInRange("zenDecayPct", 30, 0, 90);
    public static final ModConfigSpec.IntValue SMOKE_RINGS_COOLDOWN_SECONDS = BUILDER
            .comment("Smoke Rings: a trick ring always lands clean, then waits this cooldown")
            .defineInRange("smokeRingsCooldownSeconds", 10, 0, 600);
    public static final ModConfigSpec.IntValue FOCUS_TWO_PCT = BUILDER
            .comment("Focus Upgrades II: the bonus of every focus upgrade on your bowl grows by this much, percent")
            .defineInRange("focusUpgradeBoostPct", 50, 0, 200);
    public static final ModConfigSpec.IntValue CONDENSATES_PCT = BUILDER
            .comment("Condensates: a cast takes this much less essence from the focus of your bowl, percent")
            .defineInRange("condensatesEssencePct", 50, 0, 90);

    static {
        BUILDER.pop();
        BUILDER.comment("Dreams and nightmares").push("dreams");
    }

    public static final ModConfigSpec.IntValue DREAM_KAIF = BUILDER
            .comment("Kaif needed to dream")
            .defineInRange("dreamKaifThreshold", 60, 0, 100);
    public static final ModConfigSpec.IntValue NIGHTMARE_KAIF = BUILDER
            .comment("Kaif at or below which sleep brings a nightmare")
            .defineInRange("nightmareKaifThreshold", -50, -100, 0);
    public static final ModConfigSpec.IntValue WEIGHT_INSPIRATION = BUILDER.defineInRange("weightInspiration", 30, 0, 100);
    public static final ModConfigSpec.IntValue WEIGHT_COMPASS = BUILDER.defineInRange("weightCompass", 25, 0, 100);
    public static final ModConfigSpec.IntValue WEIGHT_RECIPE = BUILDER.defineInRange("weightRecipe", 20, 0, 100);
    public static final ModConfigSpec.IntValue WEIGHT_INSIGHT = BUILDER.defineInRange("weightInsight", 15, 0, 100);
    public static final ModConfigSpec.IntValue WEIGHT_LUCID = BUILDER.defineInRange("weightLucid", 10, 0, 100);
    public static final ModConfigSpec.IntValue WEIGHT_LUCID_CONTROLLED = BUILDER
            .comment("Lucid dream weight after the node Dream Control")
            .defineInRange("weightLucidControlled", 20, 0, 100);
    public static final ModConfigSpec.IntValue INSPIRATION_PCT = BUILDER
            .comment("Inspiration: this much more essence in every bottle from the essence press until sunset")
            .defineInRange("inspirationPct", 20, 0, 200);
    public static final ModConfigSpec.IntValue COMPASS_SECONDS = BUILDER
            .comment("Smoke compass: seconds of smoke pointing at the nearest geode or quasium ore")
            .defineInRange("compassSeconds", 60, 1, 600);
    public static final ModConfigSpec.IntValue COMPASS_SEARCH_CHUNKS = BUILDER
            .comment("Smoke compass: search radius in chunks")
            .defineInRange("compassSearchChunks", 6, 1, 16);
    public static final ModConfigSpec.IntValue DREAM_INSIGHT = BUILDER
            .comment("Insight dream: insight given")
            .defineInRange("dreamInsight", 3, 0, 100);
    public static final ModConfigSpec.IntValue NIGHTMARE_HALLUCINATION_SECONDS = BUILDER
            .comment("Hallucinations after waking from a nightmare")
            .defineInRange("nightmareHallucinationSeconds", 30, 0, 600);
    public static final ModConfigSpec.IntValue DREAM_COOLDOWN_TICKS = BUILDER
            .comment("Ticks after a sleep before the next one can bring a dream or a nightmare, so getting in and out of bed gives one dream")
            .defineInRange("dreamCooldownTicks", 12000, 0, 240000);
    public static final ModConfigSpec.IntValue DREAM_DRAUGHT_ML = BUILDER
            .comment("Dream essence one sip takes; after the sip the next sleep brings a lucid dream at any kaif above a nightmare")
            .defineInRange("dreamDraughtMl", 5, 1, 100);

    static {
        BUILDER.pop();
        BUILDER.comment("Overdose 2.0, the smoke phantom and the kaif coma").push("overdose");
    }

    public static final ModConfigSpec.DoubleValue OVERDOSE_STAGE2_GRAMS = BUILDER
            .comment("Grams in the overdose window for the heavy stage")
            .defineInRange("overdoseStage2Grams", 80.0, 50.0, 150.0);
    public static final ModConfigSpec.DoubleValue OVERDOSE_STAGE3_GRAMS = BUILDER
            .comment("Grams in the overdose window for the kaif coma")
            .defineInRange("overdoseStage3Grams", 100.0, 60.0, 200.0);
    public static final ModConfigSpec.IntValue SICK_SMOKE_FROM_PCT = BUILDER
            .comment("Share of the overdose limit already in the window, percent, from which the exhale turns sickly yellow-green and thick")
            .defineInRange("sickSmokeFromPct", 60, 1, 99);
    public static final ModConfigSpec.IntValue SICK_COUGH_FROM_PCT = BUILDER
            .comment("Share of the overdose limit already in the window, percent, from which every exhale ends with a dry cough")
            .defineInRange("sickCoughFromPct", 90, 1, 100);
    public static final ModConfigSpec.IntValue COMA_SECONDS = BUILDER
            .comment("Length of the kaif coma")
            .defineInRange("comaDurationSec", 30, 5, 120);
    public static final ModConfigSpec.IntValue COMA_FOOD_LOSS = BUILDER
            .comment("Food points lost by waking from the coma")
            .defineInRange("comaFoodLoss", 6, 0, 20);
    public static final ModConfigSpec.IntValue COMA_WAKE_KAIF = BUILDER
            .comment("Kaif after waking from the coma")
            .defineInRange("comaWakeKaif", -50, -100, 0);
    public static final ModConfigSpec.BooleanValue REALISM_DEATH = BUILDER
            .comment("Death at the third overdose stage instead of the coma")
            .define("realismDeath", false);
    public static final ModConfigSpec.IntValue PHANTOM_HEALTH = BUILDER
            .comment("Smoke phantom health")
            .defineInRange("phantomHealth", 6, 1, 100);
    public static final ModConfigSpec.IntValue PHANTOM_STEAL_PER_SECOND = BUILDER
            .comment("Kaif a smoke phantom steals per second")
            .defineInRange("phantomStealPerSecond", 1, 0, 20);
    public static final ModConfigSpec.IntValue PHANTOM_STEAL_RADIUS = BUILDER
            .comment("Distance at which a smoke phantom steals kaif")
            .defineInRange("phantomStealRadius", 3, 1, 16);
    public static final ModConfigSpec.IntValue PHANTOM_DROP_CHANCE_PCT = BUILDER
            .comment("Chance that a smoke phantom drops delirium essence")
            .defineInRange("phantomDropChancePct", 10, 0, 100);
    public static final ModConfigSpec.IntValue PHANTOM_DROP_ML = BUILDER
            .comment("Delirium essence in that drop, ml")
            .defineInRange("phantomDropMl", 1, 1, 20);
    public static final ModConfigSpec.IntValue PHANTOM_MAX_NEARBY = BUILDER
            .comment("Smoke phantoms around one player at once, as many as client hallucinations")
            .defineInRange("phantomMaxNearby", 4, 0, 16);
    public static final ModConfigSpec.IntValue PHANTOM_SPAWN_MIN_SECONDS = BUILDER
            .comment("Shortest pause between smoke phantoms, as for client hallucinations")
            .defineInRange("phantomSpawnMinSeconds", 5, 1, 600);
    public static final ModConfigSpec.IntValue PHANTOM_SPAWN_MAX_SECONDS = BUILDER
            .comment("Longest pause between smoke phantoms, as for client hallucinations")
            .defineInRange("phantomSpawnMaxSeconds", 25, 1, 600);
    public static final ModConfigSpec.IntValue HALLUCINATION_INTENSITY_PCT = BUILDER
            .comment("Accessibility: how many hallucinations you see, 0 turns them off")
            .defineInRange("hallucinationIntensityPct", 100, 0, 100);
    public static final ModConfigSpec.IntValue SCREEN_SHAKE_PCT = BUILDER
            .comment("Accessibility: withdrawal shake while drawing a bow, 0 turns it off")
            .defineInRange("screenShakePct", 100, 0, 100);

    static {
        BUILDER.pop();
        BUILDER.comment("Hookah ghost and withdrawal").push("ghost");
    }

    public static final ModConfigSpec.IntValue GHOST_KAIF = BUILDER
            .comment("Kaif at or below which the ghost comes at night")
            .defineInRange("ghostKaif", -50, -100, 0);
    public static final ModConfigSpec.IntValue GHOST_RADIUS = BUILDER
            .comment("How close to the player the ghost appears")
            .defineInRange("ghostRadius", 8, 2, 32);
    public static final ModConfigSpec.IntValue GHOST_STAY_SECONDS = BUILDER
            .comment("How long the ghost waits for an answer")
            .defineInRange("ghostStaySeconds", 120, 10, 1200);
    public static final ModConfigSpec.IntValue GHOST_KAIF_GAIN = BUILDER
            .comment("Kaif the deal gives")
            .defineInRange("ghostKaifGain", 50, 0, 200);
    public static final ModConfigSpec.IntValue GHOST_PRICE_EMERALDS = BUILDER
            .comment("Deal price in emeralds")
            .defineInRange("ghostPriceEmeralds", 10, 0, 64);
    public static final ModConfigSpec.IntValue GHOST_PRICE_LEVELS = BUILDER
            .comment("Deal price in experience levels")
            .defineInRange("ghostPriceLevels", 5, 0, 100);
    public static final ModConfigSpec.IntValue GHOST_TOLERANCE = BUILDER
            .comment("Tolerance the deal adds to every flavor")
            .defineInRange("ghostTolerance", 20, 0, 100);
    public static final ModConfigSpec.IntValue GHOST_CURSE_TICKS = BUILDER
            .comment("How long kaif decays faster after the deal")
            .defineInRange("ghostCurseTicks", 24000, 0, 240000);
    public static final ModConfigSpec.IntValue GHOST_CURSE_MULTIPLIER = BUILDER
            .comment("Kaif decay multiplier after the deal")
            .defineInRange("ghostCurseMultiplier", 2, 1, 10);
    public static final ModConfigSpec.IntValue WITHDRAWAL_SHAKE_KAIF = BUILDER
            .comment("Kaif below which drawing a bow shakes the camera")
            .defineInRange("withdrawalShakeKaif", -20, -100, 0);
    public static final ModConfigSpec.IntValue WITHDRAWAL_FATIGUE_KAIF = BUILDER
            .comment("Kaif below which withdrawal gives Mining Fatigue I")
            .defineInRange("withdrawalFatigueKaif", -50, -100, 0);

    static {
        BUILDER.pop();
        BUILDER.comment("Titles").push("titles");
    }

    public static final ModConfigSpec.IntValue TITLE_SOMMELIER_FLAVORS = BUILDER
            .comment("Flavors tried for the Sommelier title")
            .defineInRange("sommelierFlavors", 21, 1, 100);
    public static final ModConfigSpec.IntValue TITLE_IRON_LUNGS_PUFFS = BUILDER
            .comment("Puffs for the Iron Lungs title")
            .defineInRange("ironLungsPuffs", 10000, 1, 1000000);

    static {
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    private MindConfig() {
    }

    private static boolean isCount(Object value) {
        return value instanceof Integer number && number >= 0;
    }

    public static int tier(ModConfigSpec.ConfigValue<List<? extends Integer>> table, int tier) {
        List<Integer> values = ConfigArgs.synced(table);
        return values.isEmpty() ? 0 : values.get(Math.max(0, Math.min(values.size(), tier) - 1));
    }
}
