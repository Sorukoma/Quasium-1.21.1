package net.sorukoma.quasium.smokelands;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class SmokelandsConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    static {
        BUILDER.push("portal");
    }

    public static final ModConfigSpec.IntValue PORTAL_KAIF = BUILDER
            .comment("Kaif needed to pass the smoke portal")
            .defineInRange("portalKaif", 80, 0, 100);
    public static final ModConfigSpec.IntValue PORTAL_SECONDS = BUILDER
            .comment("Seconds a player has to stand in the smoke portal before it carries him away")
            .defineInRange("portalSeconds", 3, 1, 20);
    public static final ModConfigSpec.IntValue RUINED_PORTAL_CHANCE_PCT = BUILDER
            .comment("Chance that a ruined smoke portal rises on a spot of its overworld grid, one spot per 32x32 chunks, percent. 0 turns them off")
            .defineInRange("ruinedPortalChancePct", 50, 0, 100);

    static {
        BUILDER.pop().push("world");
    }

    public static final ModConfigSpec.IntValue SEA_WALK_KAIF = BUILDER
            .comment("Kaif from which the Sea of Smoke holds a player up; below it the player sinks")
            .defineInRange("seaWalkKaif", 60, 0, 100);
    public static final ModConfigSpec.IntValue SEA_SINK_KAIF_PER_SECOND = BUILDER
            .comment("Kaif lost per second while sinking in the Sea of Smoke")
            .defineInRange("seaSinkKaifPerSecond", 2, 0, 20);
    public static final ModConfigSpec.IntValue SMOLDER_KAIF_PER_SECOND = BUILDER
            .comment("Kaif lost per second while standing sunk in smolder of the Coal Marsh")
            .defineInRange("smolderKaifPerSecond", 1, 0, 20);
    public static final ModConfigSpec.IntValue RUINS_SIGHT_RADIUS = BUILDER
            .comment("Blocks from the middle of hidden ruins of the First at which a player who sees ruins makes them show for everyone")
            .defineInRange("ruinsSightRadius", 24, 4, 64);
    public static final ModConfigSpec.IntValue DRAFT_CYCLE_SECONDS = BUILDER
            .comment("Length of one draft cycle, the pulse of the Smokelands sky that replaces day and night, seconds")
            .defineInRange("draftCycleSeconds", 120, 10, 2400);

    static {
        BUILDER.pop().push("storm");
    }

    public static final ModConfigSpec.IntValue STORM_CALM_MIN_SECONDS = BUILDER
            .comment("Shortest calm between two cinder storms, seconds")
            .defineInRange("stormCalmMinSeconds", 100, 1, 12000);
    public static final ModConfigSpec.IntValue STORM_CALM_MAX_SECONDS = BUILDER
            .comment("Longest calm between two cinder storms, seconds")
            .defineInRange("stormCalmMaxSeconds", 200, 1, 12000);
    public static final ModConfigSpec.IntValue STORM_SECONDS = BUILDER
            .comment("How long a cinder storm lasts, seconds")
            .defineInRange("stormSeconds", 25, 1, 1200);
    public static final ModConfigSpec.IntValue STORM_VISIBILITY_CUT_PCT = BUILDER
            .comment("How much of the view distance a cinder storm hides, percent")
            .defineInRange("stormVisibilityCutPct", 80, 0, 95);
    public static final ModConfigSpec.IntValue STORM_CAST_MISS_PCT = BUILDER
            .comment("Chance that a cast goes astray during a cinder storm, percent")
            .defineInRange("stormCastMissPct", 15, 0, 100);
    public static final ModConfigSpec.IntValue STORM_DYMNIK_MULTIPLIER = BUILDER
            .comment("Dymnik spawn multiplier during a cinder storm")
            .defineInRange("stormDymnikMultiplier", 3, 1, 10);
    public static final ModConfigSpec.IntValue DYMNIKS_PER_PLAYER = BUILDER
            .comment("Dymniks the Smokelands keep around one player outside a storm")
            .defineInRange("dymniksPerPlayer", 2, 0, 16);

    static {
        BUILDER.pop().push("mobs");
    }

    public static final ModConfigSpec.IntValue MIST_PHANTOM_HEALTH = BUILDER
            .defineInRange("mistPhantomHealth", 30, 1, 1024);
    public static final ModConfigSpec.IntValue MIST_PHANTOM_DAMAGE = BUILDER
            .defineInRange("mistPhantomDamage", 4, 0, 100);
    public static final ModConfigSpec.IntValue MIST_PHANTOM_NAUSEA_SECONDS = BUILDER
            .defineInRange("mistPhantomNauseaSeconds", 4, 0, 60);
    public static final ModConfigSpec.IntValue TAR_SLIME_HEALTH = BUILDER
            .defineInRange("tarSlimeHealth", 18, 1, 1024);
    public static final ModConfigSpec.IntValue TAR_SLIME_SLOWNESS_LEVEL = BUILDER
            .comment("Slowness level a tar slime sticks on touch, 3 means Slowness III")
            .defineInRange("tarSlimeSlownessLevel", 3, 1, 10);
    public static final ModConfigSpec.IntValue TAR_SLIME_SLOWNESS_SECONDS = BUILDER
            .defineInRange("tarSlimeSlownessSeconds", 4, 1, 60);
    public static final ModConfigSpec.IntValue WISP_HEALTH = BUILDER
            .defineInRange("wispHealth", 4, 1, 1024);
    public static final ModConfigSpec.IntValue WISP_LIGHT = BUILDER
            .defineInRange("wispLight", 12, 0, 15);
    public static final ModConfigSpec.IntValue WISP_ZONE_RADIUS = BUILDER
            .comment("Radius of the tamed wisp micro zone where kaif does not decay")
            .defineInRange("wispZoneRadius", 3, 1, 16);
    public static final ModConfigSpec.IntValue WISP_ORE_GLOW_RADIUS = BUILDER
            .defineInRange("wispOreGlowRadius", 8, 1, 32);
    public static final ModConfigSpec.DoubleValue WISP_FLYING_SPEED = BUILDER
            .comment("Flying speed of the draft wisp, an allay flies at 0.1")
            .defineInRange("wispFlyingSpeed", 0.1, 0.02, 1.0);
    public static final ModConfigSpec.DoubleValue WISP_FOLLOW_SPEED = BUILDER
            .comment("Speed multiplier of a tamed wisp catching up with its owner; 1.3 is a bit slower than a walking player")
            .defineInRange("wispFollowSpeed", 1.3, 0.2, 4.0);
    public static final ModConfigSpec.IntValue DYMNIK_EVOLVE_SECONDS = BUILDER
            .comment("Seconds a dymnik spends in the Smokelands or in dense smoke before it turns into a garevshchik")
            .defineInRange("dymnikEvolveSeconds", 120, 1, 36000);
    public static final ModConfigSpec.IntValue GAREVSHCHIK_EVOLVE_SECONDS = BUILDER
            .comment("Seconds a garevshchik spends over the Sea of Smoke before it turns into a mist phantom")
            .defineInRange("garevshchikEvolveSeconds", 180, 1, 36000);
    public static final ModConfigSpec.IntValue EVOLVE_DENSITY = BUILDER
            .comment("Chunk smoke density from which dymniks evolve outside the Smokelands too")
            .defineInRange("evolveDensity", 80, 0, 100);

    static {
        BUILDER.pop().push("sultan");
    }

    public static final ModConfigSpec.IntValue SULTAN_HEALTH = BUILDER
            .defineInRange("sultanHealth", 300, 1, 4096);
    public static final ModConfigSpec.IntValue ARENA_RADIUS = BUILDER
            .defineInRange("arenaRadius", 32, 8, 64);
    public static final ModConfigSpec.IntValue WAKE_DISTANCE = BUILDER
            .comment("A player closer than this to the throne wakes the Sultan, blocks")
            .defineInRange("wakeDistance", 16, 2, 64);
    public static final ModConfigSpec.IntValue PHASE_TWO_PCT = BUILDER
            .comment("Health percent where phase two starts")
            .defineInRange("phaseTwoPct", 66, 1, 100);
    public static final ModConfigSpec.IntValue PHASE_THREE_PCT = BUILDER
            .comment("Health percent where phase three starts")
            .defineInRange("phaseThreePct", 33, 1, 100);
    public static final ModConfigSpec.IntValue RING_DAMAGE = BUILDER
            .defineInRange("ringDamage", 6, 0, 100);
    public static final ModConfigSpec.DoubleValue RING_SPEED = BUILDER
            .comment("Blocks per tick")
            .defineInRange("ringSpeed", 0.8, 0.05, 4.0);
    public static final ModConfigSpec.IntValue RINGS_PER_VOLLEY = BUILDER
            .defineInRange("ringsPerVolley", 3, 1, 12);
    public static final ModConfigSpec.IntValue RING_INTERVAL_SECONDS = BUILDER
            .defineInRange("ringIntervalSeconds", 4, 1, 60);
    public static final ModConfigSpec.IntValue INHALE_INTERVAL_SECONDS = BUILDER
            .defineInRange("inhaleIntervalSeconds", 20, 1, 600);
    public static final ModConfigSpec.IntValue INHALE_SECONDS = BUILDER
            .defineInRange("inhaleSeconds", 5, 1, 60);
    public static final ModConfigSpec.IntValue INHALE_LUNGS_PER_SECOND = BUILDER
            .comment("Ml of smoke the inhale steals from the lungs of every player in the arena per second")
            .defineInRange("inhaleLungsPerSecond", 10, 0, 400);
    public static final ModConfigSpec.IntValue INHALE_KAIF_PER_SECOND = BUILDER
            .defineInRange("inhaleKaifPerSecond", 1, 0, 20);
    public static final ModConfigSpec.IntValue CLOUD_INTERVAL_SECONDS = BUILDER
            .defineInRange("cloudIntervalSeconds", 8, 1, 600);
    public static final ModConfigSpec.IntValue CLOUD_RADIUS = BUILDER
            .defineInRange("cloudRadius", 4, 1, 16);
    public static final ModConfigSpec.IntValue CLOUD_DAMAGE = BUILDER
            .defineInRange("cloudDamage", 3, 0, 100);
    public static final ModConfigSpec.IntValue CLOUD_POISON_SECONDS = BUILDER
            .defineInRange("cloudPoisonSeconds", 4, 0, 60);
    public static final ModConfigSpec.IntValue SUMMON_INTERVAL_SECONDS = BUILDER
            .defineInRange("summonIntervalSeconds", 30, 1, 600);
    public static final ModConfigSpec.IntValue SUMMON_COUNT = BUILDER
            .defineInRange("summonCount", 4, 0, 16);
    public static final ModConfigSpec.IntValue BURNOUT_INTERVAL_SECONDS = BUILDER
            .comment("How often the crown coal burns out in phase three, seconds")
            .defineInRange("burnoutIntervalSeconds", 45, 1, 600);
    public static final ModConfigSpec.IntValue VULNERABLE_SECONDS = BUILDER
            .comment("Window after a burnout to douse the crown with dew, seconds")
            .defineInRange("vulnerableSeconds", 10, 1, 60);
    public static final ModConfigSpec.IntValue DOUSE_DEW_ML = BUILDER
            .comment("Ml of dew one douse takes from the bottle")
            .defineInRange("douseDewMl", 5, 1, 100);
    public static final ModConfigSpec.IntValue STUN_SECONDS = BUILDER
            .defineInRange("stunSeconds", 5, 1, 60);
    public static final ModConfigSpec.IntValue STUN_DAMAGE_MULTIPLIER = BUILDER
            .defineInRange("stunDamageMultiplier", 2, 1, 10);
    public static final ModConfigSpec.IntValue BLAST_DAMAGE = BUILDER
            .comment("Smoke blast when the crown is not doused in time")
            .defineInRange("blastDamage", 12, 0, 100);
    public static final ModConfigSpec.IntValue BLAST_RADIUS = BUILDER
            .defineInRange("blastRadius", 8, 1, 32);
    public static final ModConfigSpec.IntValue REWARD_DREAM_QUARTZ = BUILDER
            .defineInRange("rewardDreamQuartz", 16, 0, 64);
    public static final ModConfigSpec.IntValue RESPAWN_DAYS = BUILDER
            .comment("Game days after the Sultan's death before the respawn ritual can bring him back")
            .defineInRange("respawnDays", 3, 0, 100);
    public static final ModConfigSpec.IntValue VOW_INHALE_PCT = BUILDER
            .comment("Pause between inhales of a Sultan who took the dream quartz vow, percent of the usual one")
            .defineInRange("vowInhalePct", 60, 10, 100);
    public static final ModConfigSpec.IntValue VOW_WINDOW_PCT = BUILDER
            .comment("Burnout window of a Sultan who took the eternal coal vow, percent of the usual one")
            .defineInRange("vowWindowPct", 50, 10, 100);
    public static final ModConfigSpec.IntValue VOW_DREAM_QUARTZ = BUILDER
            .comment("Extra dream quartz the Sultan drops for each vow he took")
            .defineInRange("vowDreamQuartz", 4, 0, 16);

    static {
        BUILDER.pop().push("resources");
    }

    public static final ModConfigSpec.IntValue CLOT_ML = BUILDER
            .comment("Essence in one note clot, ml; it is bottled in four equal parts")
            .defineInRange("clotMl", 64, 4, 1024);
    public static final ModConfigSpec.IntValue PRIMORDIAL_LEAVES_PER_HARVEST = BUILDER
            .defineInRange("primordialLeavesPerHarvest", 2, 1, 16);

    static {
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    private SmokelandsConfig() {
    }
}
