package net.sorukoma.quasium.selection;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public final class SelectionConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    static {
        BUILDER.comment("Genetics: tobacco poles and hybrids").push("genetics");
    }

    public static final ModConfigSpec.IntValue MUTATION_QUALITY_BONUS_PCT = BUILDER
            .comment("Hybrid chance bonus per point of average parent quality, percent of the pole chance")
            .defineInRange("mutationQualityBonusPct", 10, 0, 100);

    static {
        BUILDER.pop();
        BUILDER.comment("Leaf quality in stars, 1..5").push("quality");
    }

    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> KAIF_MULTIPLIER_PCT = BUILDER
            .comment("Kaif of a self-mixed pack made from a leaf of 1..5 stars, percent")
            .defineList("kaifMultiplierPct", List.of(100, 110, 125, 150, 200), () -> 100,
                    entry -> entry instanceof Integer pct && pct >= 0 && pct <= 1000);

    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> DISTILL_BONUS_ML = BUILDER
            .comment("Extra ml of the main note when a leaf of 1..5 stars is distilled")
            .defineList("distillBonusMl", List.of(0, 0, 1, 1, 2), () -> 0,
                    entry -> entry instanceof Integer ml && ml >= 0 && ml <= 100);

    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> WILD_SEED_STAR_WEIGHTS = BUILDER
            .comment("Weights of 1, 2 and 3 stars, rolled for each stat of a seed found in grass")
            .defineList("wildSeedStarWeights", List.of(60, 32, 8), () -> 0,
                    entry -> entry instanceof Integer weight && weight >= 0 && weight <= 1000);

    public static final ModConfigSpec.IntValue HIVE_RADIUS = BUILDER
            .comment("A beehive or bee nest with honey within this many blocks gives the harvest +1 star")
            .defineInRange("hiveRadius", 8, 1, 32);

    public static final ModConfigSpec.IntValue EXACT_MATURE_WINDOW_SECONDS = BUILDER
            .comment("Seconds after a bush ripens when its harvest still gets the exact-mature star")
            .defineInRange("exactMatureWindowSeconds", 300, 1, 3600);

    static {
        BUILDER.pop();
        BUILDER.comment("Aphids on tobacco bushes").push("aphids");
    }

    public static final ModConfigSpec.IntValue APHID_DAILY_CHANCE_PCT = BUILDER
            .comment("Chance per in-game day that a growing bush catches aphids, percent")
            .defineInRange("aphidDailyChancePct", 2, 0, 100);

    public static final ModConfigSpec.IntValue APHID_GROWTH_PCT = BUILDER
            .comment("Growth speed of an infested bush, percent of normal")
            .defineInRange("aphidGrowthPct", 50, 0, 100);

    public static final ModConfigSpec.IntValue APHID_CURE_EMBER_ML = BUILDER
            .comment("Ember essence ml spent to smoke the aphids off one bush")
            .defineInRange("aphidCureEmberMl", 1, 1, 20);

    public static final ModConfigSpec.IntValue APHID_FLOWER_RADIUS = BUILDER
            .comment("Flowers within this many blocks lower the aphid chance")
            .defineInRange("aphidFlowerRadius", 4, 0, 16);

    public static final ModConfigSpec.IntValue APHID_FLOWER_REDUCTION_PCT = BUILDER
            .comment("How much nearby flowers lower the aphid chance, percent")
            .defineInRange("aphidFlowerReductionPct", 50, 0, 100);

    static {
        BUILDER.pop();
        BUILDER.comment("New varieties: Echo leaf, Ashen, Chorus").push("varieties");
    }

    public static final ModConfigSpec.IntValue ECHO_MUFFLE_SECONDS = BUILDER
            .comment("Seconds of vibration muffling after smoking Echo leaf")
            .defineInRange("echoMuffleSeconds", 90, 1, 600);

    public static final ModConfigSpec.IntValue ASHEN_FIRE_RESISTANCE_SECONDS = BUILDER
            .comment("Seconds of Fire Resistance after smoking Ashen")
            .defineInRange("ashenFireResistanceSeconds", 10, 1, 600);

    public static final ModConfigSpec.IntValue ASHEN_BREATH_DAMAGE = BUILDER
            .comment("Damage of the burning exhale unlocked by Ashen")
            .defineInRange("ashenBreathDamage", 3, 0, 40);

    public static final ModConfigSpec.IntValue ASHEN_BREATH_FIRE_SECONDS = BUILDER
            .comment("Seconds a mob burns after the burning exhale")
            .defineInRange("ashenBreathFireSeconds", 3, 0, 60);

    public static final ModConfigSpec.IntValue CHORUS_TELEPORT_RANGE = BUILDER
            .comment("Max blocks of the exhale teleport unlocked by Chorus")
            .defineInRange("chorusTeleportRange", 8, 1, 32);

    public static final ModConfigSpec.IntValue CHORUS_TELEPORT_COOLDOWN_SECONDS = BUILDER
            .comment("Cooldown of the Chorus exhale teleport, seconds")
            .defineInRange("chorusTeleportCooldownSeconds", 10, 0, 600);

    static {
        BUILDER.pop();
        BUILDER.comment("Quasium geode").push("geode");
    }

    public static final ModConfigSpec.IntValue GEODE_RADIUS_MIN = BUILDER
            .comment("Smallest geode shell radius the worldgen test accepts")
            .defineInRange("geodeRadiusMin", 7, 3, 16);

    public static final ModConfigSpec.IntValue GEODE_RADIUS_MAX = BUILDER
            .comment("Largest geode shell radius the worldgen test accepts")
            .defineInRange("geodeRadiusMax", 11, 3, 16);

    public static final ModConfigSpec.IntValue GEODE_REVEAL_KAIF = BUILDER
            .comment("Kaif at which the geode crust stops looking like deepslate and starts to shimmer")
            .defineInRange("geodeRevealKaif", 30, 0, 100);

    public static final ModConfigSpec.IntValue GEODE_HUM_RADIUS = BUILDER
            .comment("Blocks within which a revealed geode hums")
            .defineInRange("geodeHumRadius", 8, 1, 32);

    public static final ModConfigSpec.IntValue CLUSTER_DROP_MIN_KAIF = BUILDER
            .comment("Kaif below which a quasium cluster drops nothing, like the ore")
            .defineInRange("clusterDropMinKaif", 10, 0, 100);

    public static final ModConfigSpec.IntValue CLUSTER_DROP_MIN = BUILDER
            .comment("Fewest raw quasium from a grown cluster")
            .defineInRange("clusterDropMin", 2, 0, 16);

    public static final ModConfigSpec.IntValue CLUSTER_DROP_MAX = BUILDER
            .comment("Most raw quasium from a grown cluster")
            .defineInRange("clusterDropMax", 4, 0, 16);

    static {
        BUILDER.pop();
        BUILDER.comment("Hookah master villager: wild leaves he buys, flavored tobacco and catalysts he sells, prices in emeralds").push("hookah_master");
    }

    public static final ModConfigSpec.IntValue LEAVES_BUY_COUNT = BUILDER
            .comment("Level 1: wild leaves in any state bought for one emerald")
            .defineInRange("leavesBuyCount", 4, 1, 64);

    public static final ModConfigSpec.IntValue DRIED_LEAVES_BUY_COUNT = BUILDER
            .comment("Level 2: dried wild leaves bought at once")
            .defineInRange("driedLeavesBuyCount", 8, 1, 64);

    public static final ModConfigSpec.IntValue DRIED_LEAVES_PRICE = BUILDER
            .comment("Level 2: emeralds paid for the dried leaves")
            .defineInRange("driedLeavesPrice", 2, 1, 64);

    public static final ModConfigSpec.IntValue THREE_STAR_LEAVES_COUNT = BUILDER
            .comment("Level 3: 3-star leaves bought at once")
            .defineInRange("threeStarLeavesCount", 4, 1, 64);

    public static final ModConfigSpec.IntValue THREE_STAR_PRICE = BUILDER
            .comment("Level 3: emeralds paid for the 3-star leaves")
            .defineInRange("threeStarPrice", 4, 1, 64);

    public static final ModConfigSpec.IntValue FOUR_STAR_PRICE = BUILDER
            .comment("Level 4: emeralds paid for one 4-star leaf")
            .defineInRange("fourStarPrice", 8, 1, 64);

    public static final ModConfigSpec.IntValue FIVE_STAR_PRICE = BUILDER
            .comment("Level 5: emeralds paid for one 5-star leaf")
            .defineInRange("fiveStarPrice", 16, 1, 64);

    public static final ModConfigSpec.IntValue TOBACCO_COMMON_PRICE = BUILDER
            .comment("Levels 1 and 2: emeralds for one pack of a simple flavored tobacco; the smoke caravan asks the same")
            .defineInRange("tobaccoCommonPrice", 3, 1, 64);

    public static final ModConfigSpec.IntValue TOBACCO_RARE_PRICE = BUILDER
            .comment("Levels 3 to 5: emeralds for one pack of a rare flavored tobacco; the smoke caravan asks the same")
            .defineInRange("tobaccoRarePrice", 6, 1, 64);

    public static final ModConfigSpec.IntValue BLAZE_POWDER_COUNT = BUILDER
            .comment("Level 3: blaze powder sold at once")
            .defineInRange("blazePowderCount", 4, 1, 64);

    public static final ModConfigSpec.IntValue BLAZE_POWDER_PRICE = BUILDER
            .comment("Level 3: emeralds for the blaze powder")
            .defineInRange("blazePowderPrice", 4, 1, 64);

    public static final ModConfigSpec.IntValue CHORUS_FRUIT_COUNT = BUILDER
            .comment("Level 4: chorus fruit sold at once")
            .defineInRange("chorusFruitCount", 4, 1, 64);

    public static final ModConfigSpec.IntValue CHORUS_FRUIT_PRICE = BUILDER
            .comment("Level 4: emeralds for the chorus fruit")
            .defineInRange("chorusFruitPrice", 8, 1, 64);

    public static final ModConfigSpec.IntValue ECHO_SHARD_PRICE = BUILDER
            .comment("Level 5: emeralds for one echo shard")
            .defineInRange("echoShardPrice", 24, 1, 64);

    static {
        BUILDER.pop();
        BUILDER.comment("Secret recipe: two flavors picked by the world seed").push("secret_mix");
    }

    public static final ModConfigSpec.IntValue SECRET_MIX_KAIF_BONUS_PCT = BUILDER
            .comment("Extra kaif of a pack mixed by the secret recipe of this world, percent")
            .defineInRange("secretMixKaifBonusPct", 20, 0, 200);

    static {
        BUILDER.pop();
        BUILDER.comment("Smoke caravan").push("caravan");
    }

    public static final ModConfigSpec.IntValue CARAVAN_DAILY_CHANCE_PCT = BUILDER
            .comment("Chance per in-game day that a smoke caravan comes to a random player, percent; no roll while a caravan is still on the road")
            .defineInRange("caravanVisitChancePct", 8, 0, 100);

    public static final ModConfigSpec.IntValue CARAVAN_STAY_MINUTES = BUILDER
            .comment("How long a smoke caravan stays before it leaves with its llamas, minutes of world time, counted even while its chunk is unloaded; 20 is one in-game day")
            .defineInRange("caravanStayMinutes", 20, 1, 600);

    public static final ModConfigSpec.IntValue CARAVAN_DAILY_FLAVORS = BUILDER
            .comment("Vanilla flavor sources the caravan sells each day")
            .defineInRange("caravanFlavorOffers", 1, 0, 8);

    public static final ModConfigSpec.IntValue CARAVAN_FLAVOR_MAX_USES = BUILDER
            .comment("How many times each flavor source offer of the caravan can be bought")
            .defineInRange("caravanFlavorMaxUses", 2, 1, 16);

    public static final ModConfigSpec.IntValue CARAVAN_FLAVOR_PRICE = BUILDER
            .comment("Emeralds for one flavor source offer")
            .defineInRange("caravanFlavorPrice", 2, 1, 64);

    public static final ModConfigSpec.IntValue CARAVAN_TOBACCO_OFFERS = BUILDER
            .comment("Random flavored tobacco packs the caravan sells each visit, at the hookah master's prices")
            .defineInRange("caravanTobaccoOffers", 2, 0, 16);

    public static final ModConfigSpec.IntValue CARAVAN_TOBACCO_MAX_USES = BUILDER
            .comment("How many packs of each flavored tobacco offer the caravan has")
            .defineInRange("caravanTobaccoMaxUses", 3, 1, 16);

    public static final ModConfigSpec.IntValue CARAVAN_SELF_MIX_MIN_STARS = BUILDER
            .comment("Fewest stars of a self-mixed pack the caravan buys")
            .defineInRange("caravanSelfMixMinStars", 4, 1, 5);

    public static final ModConfigSpec.IntValue CARAVAN_SELF_MIX_MIN_GRAMS = BUILDER
            .comment("Fewest grams left in a self-mixed pack the caravan buys")
            .defineInRange("caravanSelfMixMinGrams", 8, 1, 30);

    public static final ModConfigSpec.IntValue CARAVAN_SELF_MIX_PRICE = BUILDER
            .comment("Emeralds the caravan pays for a self-mixed pack")
            .defineInRange("caravanSelfMixPrice", 5, 1, 64);

    static {
        BUILDER.pop();
        BUILDER.comment("Cartographer").push("cartographer");
    }

    public static final ModConfigSpec.IntValue LOUNGE_MAP_PRICE = BUILDER
            .comment("Emeralds and a compass a cartographer of level 2 asks for a map to an abandoned hookah lounge")
            .defineInRange("loungeMapPrice", 12, 1, 64);

    static {
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    private SelectionConfig() {
    }
}
