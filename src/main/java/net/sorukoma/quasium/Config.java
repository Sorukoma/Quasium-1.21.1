package net.sorukoma.quasium;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue QUASIUM_ARMOR_DODGE_PCT = BUILDER
            .comment("Percent chance a full set of Quasium armor dodges any damage except the void and /kill")
            .defineInRange("quasiumArmorDodgePct", 25, 0, 100);

    public static final ModConfigSpec.IntValue QUASIUM_ARMOR_HAZE_CHARGES = BUILDER
            .comment("Haze charges a full Quasium armor set holds; a charge takes a heavy or lethal hit the dodge missed")
            .defineInRange("quasiumArmorHazeCharges", 2, 0, 10);

    public static final ModConfigSpec.IntValue QUASIUM_ARMOR_HAZE_HIT_PCT = BUILDER
            .comment("A hit is heavy when it would take at least this percent of max health")
            .defineInRange("quasiumArmorHazeHitPct", 40, 1, 100);

    public static final ModConfigSpec.IntValue QUASIUM_ARMOR_HAZE_SECONDS = BUILDER
            .comment("Seconds of wearing the full set it takes to build one haze charge")
            .defineInRange("quasiumArmorHazeSeconds", 15, 1, 3600);

    public static final ModConfigSpec.IntValue QUASIUM_ARMOR_HAZE_MIN_KAIF = BUILDER
            .comment("Haze charges build up only while kaif is at least this high")
            .defineInRange("quasiumArmorHazeMinKaif", 30, -100, 100);

    public static final ModConfigSpec.IntValue KAIF_PER_SECOND_SMOKING = BUILDER
            .comment("How much kaif is gained per second of smoking hookah")
            .defineInRange("kaifPerSecondSmoking", 2, 0, 100);

    public static final ModConfigSpec.DoubleValue OVERDOSE_THRESHOLD_GRAMS = BUILDER
            .comment("Grams of tobacco burned within the overdose window that trigger an overdose, pulls and spells together")
            .defineInRange("overdoseGrams", 20.0, 1.0, 100.0);

    public static final ModConfigSpec.IntValue OVERDOSE_WINDOW_MINUTES = BUILDER
            .comment("Time window in minutes for tracking tobacco consumption before overdose triggers")
            .defineInRange("overdoseWindowMinutes", 10, 1, 30);

    // Hookah mechanics constants
    public static final ModConfigSpec.DoubleValue TOBACCO_STRENGTH_MULTIPLIER = BUILDER
            .comment("Multiplier for tobacco strength effects (kaif generation, etc.)")
            .defineInRange("tobaccoStrengthMultiplier", 0.4, 0.0, 2.0);

    public static final ModConfigSpec.DoubleValue TOBACCO_STRENGTH_BASE = BUILDER
            .comment("Base strength value for tobacco effects")
            .defineInRange("tobaccoStrengthBase", 0.3, 0.0, 1.0);

    public static final ModConfigSpec.DoubleValue TOBACCO_STRENGTH_MIN = BUILDER
            .comment("Minimum strength value for tobacco effects")
            .defineInRange("tobaccoStrengthMin", 0.0, 0.0, 1.0);

    public static final ModConfigSpec.IntValue PUFF_EFFECT_CAP_SECONDS = BUILDER
            .comment("Each pull adds its effect time to what is left of the same effect at the same level, up to this many seconds")
            .defineInRange("puffEffectCapSeconds", 300, 10, 3600);

    public static final ModConfigSpec.IntValue ROW_FRESH_PUFFS = BUILDER
            .comment("Pulls after the burning row of the bowl changes flavor during which tolerance does not cut kaif")
            .defineInRange("rowFreshPuffs", 5, 0, 50);

    public static final ModConfigSpec.IntValue PULL_MIN_MS = BUILDER
            .comment("Shortest pull that counts, ms: a shorter one gives no smoke, kaif or stats, but the tobacco it burned stays burned")
            .defineInRange("pullMinMs", 500, 0, 5000);

    public static final ModConfigSpec.IntValue BREATH_RECOVERY_MS = BUILDER
            .comment("Pause after an exhale before the pipe draws again, ms")
            .defineInRange("breathRecoveryMs", 500, 0, 5000);

    static {
        BUILDER.comment("Smokecraft: notes, distillation, casting").push("smokecraft");
    }

    public static final ModConfigSpec.IntValue LUNG_CAPACITY_MB = BUILDER
            .comment("Lung capacity in ml of smoke")
            .defineInRange("lungCapacityMb", 100, 50, 400);

    public static final ModConfigSpec.IntValue PUFF_FILL_MB = BUILDER
            .comment("Ml of smoke drawn into the lungs per second of pulling at full hookah efficiency")
            .defineInRange("puffFillMb", 25, 5, 100);

    public static final ModConfigSpec.IntValue HOLD_CHARGE_PER_SEC_PCT = BUILDER
            .comment("Spell power gained per second of holding smoke once the spell cost is inhaled, percent")
            .defineInRange("holdChargePerSecPct", 10, 0, 50);

    public static final ModConfigSpec.IntValue HOLD_MAX_MULTIPLIER_PCT = BUILDER
            .comment("Cap of the hold charge, percent of base power")
            .defineInRange("holdMaxMultiplierPct", 150, 100, 300);

    public static final ModConfigSpec.IntValue HOLD_COUGH_AFTER_SEC = BUILDER
            .comment("Seconds of holding smoke before the caster coughs it out")
            .defineInRange("holdCoughAfterSec", 8, 3, 30);

    public static final ModConfigSpec.IntValue COUGH_KAIF_LOSS = BUILDER
            .comment("Kaif lost on a cough")
            .defineInRange("coughKaifLoss", 5, 0, 50);

    public static final ModConfigSpec.IntValue COUGH_LOCK_SEC = BUILDER
            .comment("Seconds after a cough during which casting is impossible")
            .defineInRange("coughLockSec", 2, 0, 20);

    public static final ModConfigSpec.IntValue KAIF_PER_CAST = BUILDER
            .comment("Kaif gained per successful cast")
            .defineInRange("kaifPerCast", 1, 0, 10);

    public static final ModConfigSpec.IntValue KAIF_PER_CAST_KILL = BUILDER
            .comment("Kaif gained when a spell kills a mob")
            .defineInRange("kaifPerCastKill", 3, 0, 20);

    public static final ModConfigSpec.IntValue ESSENCE_PER_CAST = BUILDER
            .comment("Ml drained from every essence bottle of the bowl focus per cast")
            .defineInRange("essencePerCast", 1, 0, 20);

    public static final ModConfigSpec.IntValue SCAN_KAIF_ITEM = BUILDER
            .comment("Kaif needed to read the notes of items and blocks with the kaifometer")
            .defineInRange("scanKaifItem", 10, -100, 100);

    public static final ModConfigSpec.IntValue SCAN_KAIF_MOB = BUILDER
            .comment("Kaif needed to read the notes of mobs with the kaifometer")
            .defineInRange("scanKaifMob", 30, -100, 100);

    public static final ModConfigSpec.IntValue DERIVED_RECIPE_PCT = BUILDER
            .comment("An item without its own note profile gets this percent of the notes of the ingredients its recipe takes, split between the outputs")
            .defineInRange("derivedRecipePct", 50, 0, 100);

    public static final ModConfigSpec.IntValue DERIVED_NOTE_CAP = BUILDER
            .comment("Most ml of one note a derived profile can hold")
            .defineInRange("derivedNoteCap", 4, 1, 64);

    public static final ModConfigSpec.IntValue DERIVED_TOTAL_CAP = BUILDER
            .comment("Most ml of all notes together a derived profile can hold; a louder mix is scaled down")
            .defineInRange("derivedTotalCap", 8, 1, 64);

    public static final ModConfigSpec.IntValue DERIVED_COOKING_EMBER = BUILDER
            .comment("Ember ml a furnace, smoker, blast furnace or campfire recipe adds to the derived notes of what it cooks")
            .defineInRange("derivedCookingEmber", 1, 0, 10);

    public static final ModConfigSpec.IntValue INSIGHT_CAP = BUILDER
            .comment("Maximum stored insights")
            .defineInRange("insightCap", 20, 5, 100);

    public static final ModConfigSpec.IntValue INSIGHT_PER_ITEM = BUILDER
            .comment("Insights for the first scan of an item")
            .defineInRange("insightPerItem", 1, 0, 10);

    public static final ModConfigSpec.IntValue INSIGHT_PER_BLOCK = BUILDER
            .comment("Insights for the first scan of a block")
            .defineInRange("insightPerBlock", 1, 0, 10);

    public static final ModConfigSpec.IntValue INSIGHT_PER_MOB = BUILDER
            .comment("Insights for the first scan of a mob")
            .defineInRange("insightPerMob", 2, 0, 10);

    public static final ModConfigSpec.IntValue DISTILL_TICKS_PER_ITEM = BUILDER
            .comment("Ticks the distillation cube spends on one item")
            .defineInRange("distillTicksPerItem", 100, 10, 1200);

    public static final ModConfigSpec.IntValue DISTILL_WATER_PER_ITEM = BUILDER
            .comment("Ml of water the distillation cube boils off per item")
            .defineInRange("distillWaterPerItem", 50, 0, 1000);

    public static final ModConfigSpec.IntValue DISTILL_FUEL_PER_ITEM = BUILDER
            .comment("Furnace burn ticks consumed per distilled item (coal burns 1600, so 200 means 8 items per coal)")
            .defineInRange("distillFuelPerItem", 200, 1, 3200);

    public static final ModConfigSpec.IntValue DISTILL_WATER_CAPACITY = BUILDER
            .comment("Water tank size of the distillation cube, ml")
            .defineInRange("distillWaterCapacity", 2000, 1000, 16000);

    public static final ModConfigSpec.IntValue NOTE_BUFFER_CAPACITY = BUILDER
            .comment("Ml of each note the distillation cube can hold")
            .defineInRange("noteBufferCapacity", 64, 8, 1024);

    public static final ModConfigSpec.IntValue NOTE_JAR_CAPACITY = BUILDER
            .comment("Ml of one note a note jar can hold")
            .defineInRange("noteJarCapacity", 64, 8, 1024);

    public static final ModConfigSpec.IntValue NOTE_BOTTLE_CAPACITY = BUILDER
            .comment("Ml of a note that fits in one essence bottle")
            .defineInRange("noteBottleCapacity", 20, 1, 100);

    public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> WHISPER_ORES = BUILDER
            .comment("Blocks highlighted by the Whisper of Coals spell: block ids or #tags")
            .defineListAllowEmpty("whisperOres",
                    java.util.List.of("quasium:deepslate_quasium_ore", "#c:ores"),
                    () -> "#c:ores",
                    entry -> entry instanceof String);

    public static final ModConfigSpec.IntValue ZONE_RADIUS_T1 = BUILDER
            .comment("Radius of a tier I kaif zone (one smoker)")
            .defineInRange("zoneRadiusT1", 4, 2, 16);

    public static final ModConfigSpec.IntValue ZONE_RADIUS_T2 = BUILDER
            .comment("Radius of a tier II kaif zone (two smokers or a modular hookah)")
            .defineInRange("zoneRadiusT2", 6, 2, 16);

    public static final ModConfigSpec.IntValue ZONE_RADIUS_T3 = BUILDER
            .comment("Radius of a tier III kaif zone (three smokers and lounge furniture)")
            .defineInRange("zoneRadiusT3", 8, 2, 16);

    public static final ModConfigSpec.IntValue ZONE_JUKEBOX_BONUS_RADIUS = BUILDER
            .comment("Extra zone radius while a jukebox in the zone plays Lo-Fi Kaif Beats")
            .defineInRange("zoneJukeboxBonusRadius", 2, 0, 4);

    public static final ModConfigSpec.IntValue ZONE_SMOKER_MEMORY_SEC = BUILDER
            .comment("A player counts as smoking a hookah this many seconds after their last pull")
            .defineInRange("zoneSmokerMemorySec", 30, 5, 300);

    public static final ModConfigSpec.IntValue ZONE_CAST_DISCOUNT_PCT = BUILDER
            .comment("Cast cost discount (ml and grams) inside any kaif zone, percent")
            .defineInRange("zoneCastDiscountPct", 10, 0, 100);

    public static final ModConfigSpec.IntValue ZONE_KAIF_BONUS_PCT = BUILDER
            .comment("Extra kaif gained inside a tier II or III zone, percent")
            .defineInRange("zoneKaifBonusPct", 10, 0, 100);

    public static final ModConfigSpec.IntValue ZONE_AGGRO_REDUCTION_PCT = BUILDER
            .comment("How much smaller mob detection range gets inside a zone, percent")
            .defineInRange("zoneAggroReductionPct", 30, 0, 100);

    public static final ModConfigSpec.IntValue ZONE_TINT_PCT = BUILDER
            .comment("Accessibility: strength of the screen tint inside a zone, 0 disables it")
            .defineInRange("zoneTintPct", 100, 0, 100);

    public static final ModConfigSpec.IntValue NIRVANA_FURNITURE = BUILDER
            .comment("Lounge furniture blocks needed around a hookah for a tier III zone")
            .defineInRange("nirvanaFurniture", 2, 0, 16);

    public static final ModConfigSpec.IntValue SEATED_KAIF_DECAY_PCT = BUILDER
            .comment("Kaif decay while sitting on a lounge cushion, percent of normal")
            .defineInRange("seatedKaifDecayPct", 50, 0, 100);

    public static final ModConfigSpec.IntValue AROMA_LAMP_SECONDS_PER_ML = BUILDER
            .comment("Seconds an aroma lamp burns on one ml of essence")
            .defineInRange("aromaLampSecondsPerMl", 10, 1, 600);

    public static final ModConfigSpec.IntValue AROMA_LAMP_CAPACITY = BUILDER
            .comment("Ml of essence an aroma lamp holds")
            .defineInRange("aromaLampCapacity", 20, 1, 200);

    public static final ModConfigSpec.IntValue RING_WINDOW_MS = BUILDER
            .comment("Width of the green window on the smoke ring timing bar, milliseconds")
            .defineInRange("ringWindowMs", 250, 50, 1000);

    public static final ModConfigSpec.IntValue RING_PERFECT_MS = BUILDER
            .comment("Release within this many milliseconds of the window center for a perfect ring")
            .defineInRange("ringPerfectMs", 80, 10, 500);

    public static final ModConfigSpec.IntValue RING_PERFECT_POWER_PCT = BUILDER
            .comment("Power of a perfectly timed ring spell, percent")
            .defineInRange("ringPerfectPowerPct", 120, 100, 300);

    public static final ModConfigSpec.IntValue RING_COMBO_WINDOW_SEC = BUILDER
            .comment("Seconds between trick rings that still keep the combo going")
            .defineInRange("ringComboWindowSec", 4, 1, 30);

    public static final ModConfigSpec.IntValue RING_COMBO_KAIF = BUILDER
            .comment("Kaif for every trick ring once the combo reaches 3")
            .defineInRange("ringComboKaif", 2, 0, 20);

    public static final ModConfigSpec.IntValue RING_COMBO_INSIGHT = BUILDER
            .comment("Insights for reaching a combo of 3 trick rings")
            .defineInRange("ringComboInsight", 1, 0, 10);

    public static final ModConfigSpec.IntValue RING_LENS_POWER_PCT = BUILDER
            .comment("Power of a spell cast through your own smoke ring, percent")
            .defineInRange("ringLensPowerPct", 150, 100, 400);

    public static final int TRICK_GHOST_MAX_SEC = 10;

    public static final ModConfigSpec.IntValue TRICK_GHOST_SEC = BUILDER
            .comment("Seconds an exhale steeply up without a spell hangs as a ghost ball; a new pull in that time draws it back")
            .defineInRange("trickGhostSec", 2, 1, TRICK_GHOST_MAX_SEC);

    public static final ModConfigSpec.IntValue TRICK_JELLYFISH_SEC = BUILDER
            .comment("Seconds a trick ring blown steeply down lives as a jellyfish")
            .defineInRange("trickJellyfishSec", 8, 1, 60);

    public static final ModConfigSpec.IntValue TRICK_TORNADO_SEC = BUILDER
            .comment("Seconds an exhale without a spell into a low table spins as a tornado")
            .defineInRange("trickTornadoSec", 6, 1, 60);

    public static final ModConfigSpec.IntValue TRICK_TABLE_REACH = BUILDER
            .comment("Blocks between the smoker and a low table that still turns the exhale into a tornado")
            .defineInRange("trickTableReach", 2, 1, 8);

    public static final ModConfigSpec.IntValue HEAVY_FALL_DAMAGE_PCT = BUILDER
            .comment("Fall damage taken under the Heavy effect of Heavy Smoke, percent")
            .defineInRange("heavyFallDamagePct", 150, 100, 400);

    public static final ModConfigSpec.IntValue SMOKE_JAR_MINUTES = BUILDER
            .comment("Minutes a deployed smoke jar keeps its tier I zone alive")
            .defineInRange("smokeJarMinutes", 5, 1, 60);

    public static final ModConfigSpec.IntValue UPGRADE_CONDENSER_ML = BUILDER
            .comment("Extra lung capacity from the Condenser focus upgrade")
            .defineInRange("upgradeCondenserMl", 50, 0, 400);

    public static final ModConfigSpec.IntValue UPGRADE_COOLER_PCT = BUILDER
            .comment("Less tobacco burnt per cast with the Cooler focus upgrade, percent")
            .defineInRange("upgradeCoolerPct", 20, 0, 100);

    public static final ModConfigSpec.IntValue UPGRADE_COLLIMATOR_RANGE_PCT = BUILDER
            .comment("Range and length with the Collimator focus upgrade, percent")
            .defineInRange("upgradeCollimatorRangePct", 150, 100, 400);

    public static final ModConfigSpec.IntValue UPGRADE_COLLIMATOR_WIDTH_PCT = BUILDER
            .comment("Width and radius with the Collimator focus upgrade, percent")
            .defineInRange("upgradeCollimatorWidthPct", 50, 10, 100);

    public static final ModConfigSpec.IntValue UPGRADE_SPLITTER_POWER_PCT = BUILDER
            .comment("Power of each of the three Splitter projectiles, percent")
            .defineInRange("upgradeSplitterPowerPct", 50, 10, 100);

    public static final ModConfigSpec.IntValue UPGRADE_RESONATOR_POWER_PCT = BUILDER
            .comment("Power of the extra target hit by a Resonator focus, percent")
            .defineInRange("upgradeResonatorPowerPct", 60, 10, 100);

    public static final ModConfigSpec.IntValue DENSITY_PER_CAST = BUILDER
            .comment("Smoke density a cast adds to its chunk")
            .defineInRange("densityPerCast", 1, 0, 10);

    public static final ModConfigSpec.IntValue DENSITY_MAX = BUILDER
            .comment("Highest smoke density a chunk can hold")
            .defineInRange("densityMax", 100, 10, 1000);

    static {
        BUILDER.pop();
        BUILDER.comment("Draft: pipes, machines, smoke density").push("draft");
    }

    public static final ModConfigSpec.IntValue DRAFT_BUFFER_PER_PIPE = BUILDER
            .comment("Draft a network can store per pipe")
            .defineInRange("draftBufferPerPipe", 500, 0, 100000);

    public static final ModConfigSpec.IntValue CARGO_ML_PER_PIPE = BUILDER
            .comment("Notes in transit a network can hold per pipe, ml")
            .defineInRange("cargoMlPerPipe", 8, 1, 1000);

    public static final ModConfigSpec.IntValue SORTER_TANK_ML = BUILDER
            .comment("Essence the smoke sorter can hold, ml")
            .defineInRange("sorterTankMl", 64, 1, 10000);

    public static final ModConfigSpec.IntValue SORTER_NOTES_PER_ESSENCE = BUILDER
            .comment("Notes the smoke sorter burns per ml of essence")
            .defineInRange("sorterNotesPerEssence", 8, 1, 1000);

    public static final ModConfigSpec.IntValue BELLOWS_PULSE_DRAFT = BUILDER
            .comment("Draft per second for one second after pumping the bellows by hand")
            .defineInRange("bellowsPulseDraft", 64, 0, 10000);

    public static final ModConfigSpec.IntValue BELLOWS_COOLDOWN_TICKS = BUILDER
            .comment("Ticks between hand pumps of the bellows")
            .defineInRange("bellowsCooldownTicks", 20, 1, 1200);

    public static final ModConfigSpec.IntValue BELLOWS_REDSTONE_DRAFT = BUILDER
            .comment("Draft per second of redstone powered bellows")
            .defineInRange("bellowsRedstoneDraft", 16, 0, 10000);

    public static final ModConfigSpec.IntValue BOILER_DRAFT = BUILDER
            .comment("Draft per second of a burning smoke boiler")
            .defineInRange("boilerDraft", 64, 0, 10000);

    public static final ModConfigSpec.IntValue BOILER_WATER_ML_PER_SECOND = BUILDER
            .comment("Water a burning smoke boiler uses per second, ml")
            .defineInRange("boilerWaterMlPerSecond", 50, 0, 10000);

    public static final ModConfigSpec.IntValue BOILER_WATER_CAPACITY = BUILDER
            .comment("Water tank of the smoke boiler, ml")
            .defineInRange("boilerWaterCapacity", 4000, 1000, 100000);

    public static final ModConfigSpec.IntValue BOILER_DENSITY_PER_MINUTE = BUILDER
            .comment("Smoke density a burning boiler adds to its chunk per minute")
            .defineInRange("boilerDensityPerMinute", 4, 0, 100);

    public static final ModConfigSpec.IntValue FAN_DRAFT = BUILDER
            .comment("Draft per second of the quasium fan")
            .defineInRange("fanDraft", 256, 0, 100000);

    public static final ModConfigSpec.IntValue FAN_SECONDS_PER_EMBER_ML = BUILDER
            .comment("Seconds the quasium fan runs on one ml of Ember")
            .defineInRange("fanSecondsPerEmberMl", 10, 1, 3600);

    public static final ModConfigSpec.IntValue FAN_TANK_ML = BUILDER
            .comment("Ember tank of the quasium fan, ml")
            .defineInRange("fanTankMl", 64, 1, 10000);

    public static final ModConfigSpec.IntValue DISTILLER_DEMAND = BUILDER
            .comment("Draft per second the auto distiller needs")
            .defineInRange("distillerDemand", 32, 0, 10000);

    public static final ModConfigSpec.IntValue DISTILLER_SPEED_MULTIPLIER = BUILDER
            .comment("Distillation speed multiplier of a fully fed auto distiller")
            .defineInRange("distillerSpeedMultiplier", 2, 1, 10);

    public static final ModConfigSpec.IntValue PUMP_DEMAND = BUILDER
            .comment("Draft per second the essence pump needs")
            .defineInRange("pumpDemand", 16, 0, 10000);

    public static final ModConfigSpec.IntValue PUMP_ML_PER_SECOND = BUILDER
            .comment("Essence the pump moves per second, ml")
            .defineInRange("pumpMlPerSecond", 1, 1, 100);

    public static final ModConfigSpec.IntValue HAMMER_DEMAND = BUILDER
            .comment("Draft per second the pneumatic hammer needs")
            .defineInRange("hammerDemand", 64, 0, 10000);

    public static final ModConfigSpec.IntValue HAMMER_TICKS_PER_HIT = BUILDER
            .comment("Ticks between pneumatic hammer hits at full draft")
            .defineInRange("hammerTicksPerHit", 40, 5, 1200);

    public static final ModConfigSpec.IntValue VENT_DEMAND = BUILDER
            .comment("Draft per second the ventilation needs")
            .defineInRange("ventDemand", 48, 0, 10000);

    public static final ModConfigSpec.IntValue VENT_DENSITY_PER_MINUTE = BUILDER
            .comment("Smoke density the ventilation removes per minute in each chunk of a 3x3 area")
            .defineInRange("ventDensityPerMinute", 2, 0, 100);

    public static final ModConfigSpec.IntValue CANNON_DRAFT_PER_SHOT = BUILDER
            .comment("Draft one smoke cannon shot costs")
            .defineInRange("cannonDraftPerShot", 128, 0, 100000);

    public static final ModConfigSpec.IntValue CANNON_DAMAGE = BUILDER
            .comment("Smoke cannon damage per hit")
            .defineInRange("cannonDamage", 4, 0, 100);

    public static final ModConfigSpec.IntValue CANNON_POISON_SECONDS = BUILDER
            .comment("Poison seconds a smoke cannon hit gives")
            .defineInRange("cannonPoisonSeconds", 3, 0, 60);

    public static final ModConfigSpec.IntValue CANNON_RANGE = BUILDER
            .comment("Smoke cannon targeting range, blocks")
            .defineInRange("cannonRange", 20, 4, 64);

    public static final ModConfigSpec.IntValue CANNON_ASH_PER_SHOT = BUILDER
            .comment("Ash the smoke cannon spends per shot, ml")
            .defineInRange("cannonAshPerShot", 1, 0, 64);

    public static final ModConfigSpec.IntValue CANNON_DENSITY_PER_SHOT = BUILDER
            .comment("Smoke density one smoke cannon shot adds to its chunk")
            .defineInRange("cannonDensityPerShot", 2, 0, 100);

    public static final ModConfigSpec.IntValue CANNON_TANK_ML = BUILDER
            .comment("Ash tank of the smoke cannon, ml")
            .defineInRange("cannonTankMl", 64, 1, 10000);

    public static final ModConfigSpec.IntValue CHARGER_DEMAND = BUILDER
            .comment("Draft per second the flask charger needs")
            .defineInRange("chargerDemand", 32, 0, 10000);

    public static final ModConfigSpec.IntValue CHARGER_SECONDS_PER_CHARGE = BUILDER
            .comment("Seconds the flask charger needs for one charge at full draft")
            .defineInRange("chargerSecondsPerCharge", 10, 1, 3600);

    public static final ModConfigSpec.IntValue FLASK_MAX_CHARGES = BUILDER
            .comment("Flask charges a hookah can hold")
            .defineInRange("flaskMaxCharges", 8, 1, 64);

    public static final ModConfigSpec.IntValue FLASK_CHARGE_ESSENCE_MULTIPLIER = BUILDER
            .comment("Focus essence multiplier of a cast paid with a flask charge")
            .defineInRange("flaskChargeEssenceMultiplier", 2, 1, 10);

    static {
        BUILDER.pop();
        BUILDER.comment("Golems, smoke marks and the kiln").push("golems");
    }

    public static final ModConfigSpec.IntValue COAL_GOLEM_HEALTH = BUILDER
            .comment("Calloud golem health")
            .defineInRange("coalGolemHealth", 10, 1, 1000);

    public static final ModConfigSpec.IntValue COAL_GOLEM_RANGE = BUILDER
            .comment("Blocks a calloud golem looks for cooling hookahs")
            .defineInRange("coalGolemRange", 16, 4, 64);

    public static final ModConfigSpec.IntValue COAL_GOLEM_HEAT_PCT = BUILDER
            .comment("Heat percent below which a calloud golem refuels a hookah")
            .defineInRange("coalGolemHeatPct", 50, 1, 100);

    public static final ModConfigSpec.IntValue COAL_GOLEM_INVENTORY = BUILDER
            .comment("Coal a calloud golem can carry")
            .defineInRange("coalGolemInventory", 8, 1, 64);

    public static final ModConfigSpec.IntValue COAL_GOLEM_FIRE_SECONDS = BUILDER
            .comment("Seconds a calloud golem's hit sets the target on fire")
            .defineInRange("coalGolemFireSeconds", 2, 0, 60);

    public static final ModConfigSpec.IntValue CLAY_GOLEM_HEALTH = BUILDER
            .comment("Clay golem health")
            .defineInRange("clayGolemHealth", 20, 1, 1000);

    public static final ModConfigSpec.IntValue CLAY_GOLEM_STACKS = BUILDER
            .comment("Item stacks a clay golem can carry")
            .defineInRange("clayGolemStacks", 4, 1, 27);

    public static final ModConfigSpec.IntValue CLAY_UPGRADE_ML_PER_CHARGE = BUILDER
            .comment("Note ml that make one clay golem upgrade charge")
            .defineInRange("clayUpgradeMlPerCharge", 20, 1, 1000);

    public static final ModConfigSpec.IntValue CLAY_UPGRADE_ML_PER_TASK = BUILDER
            .comment("Note ml a clay golem upgrade spends per task")
            .defineInRange("clayUpgradeMlPerTask", 1, 0, 100);

    public static final ModConfigSpec.IntValue CLAY_MENTHOL_SPEED_PCT = BUILDER
            .comment("Clay golem speed bonus from Menthol, percent")
            .defineInRange("clayMentholSpeedPct", 25, 0, 400);

    public static final ModConfigSpec.IntValue CLAY_NECTAR_EXTRA_STACKS = BUILDER
            .comment("Extra stacks a clay golem carries with Nectar")
            .defineInRange("clayNectarExtraStacks", 2, 0, 27);

    public static final ModConfigSpec.IntValue CLAY_RESIN_EXTRA_HEALTH = BUILDER
            .comment("Extra health a clay golem gets from Resin")
            .defineInRange("clayResinExtraHealth", 10, 0, 1000);

    public static final ModConfigSpec.IntValue CLAY_GUARD_RADIUS = BUILDER
            .comment("Blocks around its bound guard mark where a clay golem fights hostile mobs")
            .defineInRange("clayGuardRadius", 8, 2, 32);

    public static final ModConfigSpec.DoubleValue CLAY_GUARD_DAMAGE = BUILDER
            .comment("Damage of one clay golem hit")
            .defineInRange("clayGuardDamage", 5.0, 0.0, 100.0);

    public static final ModConfigSpec.IntValue CLAY_GUARD_ATTACK_TICKS = BUILDER
            .comment("Ticks between clay golem hits when it is happy, a sad golem hits slower")
            .defineInRange("clayGuardAttackTicks", 30, 5, 1200);

    public static final ModConfigSpec.IntValue WARDEN_MAX_HEALTH = BUILDER
            .comment("Smoke warden max health, placed wardens pick it up on load")
            .defineInRange("wardenMaxHealth", 40, 1, 1000);

    public static final ModConfigSpec.IntValue WARDEN_DAMAGE = BUILDER
            .comment("Smoke warden shot damage")
            .defineInRange("wardenDamage", 4, 0, 100);

    public static final ModConfigSpec.IntValue WARDEN_POISON_SECONDS = BUILDER
            .comment("Poison seconds of a smoke warden shot")
            .defineInRange("wardenPoisonSeconds", 2, 0, 60);

    public static final ModConfigSpec.IntValue WARDEN_SLOW_SECONDS = BUILDER
            .comment("Slowness II seconds of a smoke warden shot, undead and spiders feel it too")
            .defineInRange("wardenSlowSeconds", 2, 0, 60);

    public static final ModConfigSpec.IntValue WARDEN_FIRE_RANGE = BUILDER
            .comment("Smoke warden range, blocks")
            .defineInRange("wardenFireRange", 16, 4, 64);

    public static final ModConfigSpec.IntValue WARDEN_FIRE_TICKS = BUILDER
            .comment("Ticks between smoke warden shots")
            .defineInRange("wardenFireTicks", 20, 5, 1200);

    public static final ModConfigSpec.IntValue WARDEN_SHOTS_PER_COAL = BUILDER
            .comment("Smoke warden shots per coal")
            .defineInRange("wardenShotsPerCoal", 20, 1, 1000);

    public static final ModConfigSpec.IntValue WARDEN_INVENTORY = BUILDER
            .comment("Coal a smoke warden can hold")
            .defineInRange("wardenInventory", 8, 1, 64);

    public static final ModConfigSpec.IntValue WARDEN_REPAIR_HEALTH = BUILDER
            .comment("Health one clay ball gives back to a smoke warden")
            .defineInRange("wardenRepairHealth", 8, 1, 1000);

    public static final ModConfigSpec.IntValue MARKS_PER_CHUNK = BUILDER
            .comment("Smoke marks allowed in one chunk")
            .defineInRange("marksPerChunk", 32, 1, 1024);

    public static final ModConfigSpec.IntValue KILN_ML_PER_ITEM = BUILDER
            .comment("Note ml the kiln spends per infused item")
            .defineInRange("kilnMlPerItem", 20, 1, 1000);

    public static final ModConfigSpec.IntValue KILN_SECONDS_PER_ITEM = BUILDER
            .comment("Seconds the kiln needs per infused item")
            .defineInRange("kilnSecondsPerItem", 10, 1, 3600);

    public static final ModConfigSpec.IntValue INFUSION_EMBER_FIRE_SECONDS = BUILDER
            .comment("Seconds an Ember infused weapon sets targets on fire")
            .defineInRange("infusionEmberFireSeconds", 3, 0, 60);

    public static final ModConfigSpec.IntValue INFUSION_EMBER_DROP_PCT = BUILDER
            .comment("Chance an Ember infused tool drops an ember, percent")
            .defineInRange("infusionEmberDropPct", 10, 0, 100);

    public static final ModConfigSpec.IntValue INFUSION_RESIN_DURABILITY_PCT = BUILDER
            .comment("Durability bonus of Resin infusion, percent")
            .defineInRange("infusionResinDurabilityPct", 10, 0, 100);

    public static final ModConfigSpec.IntValue INFUSION_DEW_EXTRA_DROPS = BUILDER
            .comment("Extra drops of Dew infused shears and rods")
            .defineInRange("infusionDewExtraDrops", 1, 0, 16);

    public static final ModConfigSpec.IntValue INFUSION_ASH_FINISH_PCT = BUILDER
            .comment("Health percent under which an Ash infused weapon burns the body")
            .defineInRange("infusionAshFinishPct", 10, 0, 100);

    public static final ModConfigSpec.DoubleValue COAL_GOLEM_SPEED = BUILDER
            .comment("Calloud golem movement speed")
            .defineInRange("coalGolemSpeed", 0.3, 0.05, 1.0);

    public static final ModConfigSpec.DoubleValue CLAY_GOLEM_SPEED = BUILDER
            .comment("Clay golem movement speed")
            .defineInRange("clayGolemSpeed", 0.25, 0.05, 1.0);

    public static final ModConfigSpec.IntValue GOLEM_HUNGER_MINUTES = BUILDER
            .comment("Real minutes a full golem satisfaction meter lasts while the golem idles, 40 is two Minecraft days")
            .defineInRange("golemSatietyMinutes", 40, 1, 1440);

    public static final ModConfigSpec.DoubleValue GOLEM_JOB_COST_PCT = BUILDER
            .comment("Satisfaction percent a golem spends per finished job on top of the idle drain")
            .defineInRange("golemJobCostPct", 0.3, 0.0, 100.0);

    public static final ModConfigSpec.IntValue FRIES_MUNCHIES_MIN_KAIF = BUILDER
            .comment("Kaif from which French fries are eaten as a smoker's snack")
            .defineInRange("friesMunchiesMinKaif", 30, -100, 100);

    public static final ModConfigSpec.IntValue FRIES_MUNCHIES_EXTRA_FOOD = BUILDER
            .comment("Extra hunger points French fries give as a smoker's snack")
            .defineInRange("friesMunchiesExtraFood", 3, 0, 20);

    public static final ModConfigSpec.IntValue FRIES_KAIF_HOLD_SECONDS = BUILDER
            .comment("Seconds a smoker's snack of French fries holds kaif from decaying")
            .defineInRange("friesKaifHoldSeconds", 60, 0, 3600);

    public static final ModConfigSpec.IntValue GOLEM_APPLE_PCT = BUILDER
            .comment("Satisfaction percent an apple gives a golem")
            .defineInRange("golemApplePct", 25, 1, 100);

    public static final ModConfigSpec.DoubleValue GOLEM_PET_PCT = BUILDER
            .comment("Satisfaction percent a golem gets from petting")
            .defineInRange("golemPetPct", 0.5, 0.0, 100.0);

    public static final ModConfigSpec.IntValue GOLEM_PET_COOLDOWN_SECONDS = BUILDER
            .comment("Seconds before the same golem can be petted again")
            .defineInRange("golemPetCooldownSeconds", 30, 0, 3600);

    public static final ModConfigSpec.IntValue GOLEM_MARK_RADIUS = BUILDER
            .comment("Blocks around a golem where it finds its bound marks and walks to them, program tables count twice as far")
            .defineInRange("golemMarkRadius", 100, 16, 160);

    public static final ModConfigSpec.IntValue PROGRAM_MAX_BLOCKS = BUILDER
            .comment("Most blocks a golem program can hold, nested ones count too")
            .defineInRange("programMaxBlocks", 64, 8, 256);

    public static final ModConfigSpec.IntValue PROGRAM_MAX_NESTING = BUILDER
            .comment("How many loop and condition brackets can sit inside each other in a golem program")
            .defineInRange("programMaxNesting", 4, 1, 8);

    public static final ModConfigSpec.IntValue PROGRAM_LOGIC_PER_TICK = BUILDER
            .comment("Most loop and condition blocks a golem passes in one tick before it waits for the next one")
            .defineInRange("programLogicPerTick", 32, 4, 256);

    public static final ModConfigSpec.IntValue PROGRAM_AREA_SCAN_PER_TICK = BUILDER
            .comment("Most area blocks a golem looks over in one tick when the first action of a For each body has nothing to do there")
            .defineInRange("programAreaScanPerTick", 512, 16, 16384);

    public static final ModConfigSpec.IntValue PROGRAM_SIGNAL_CHECK_TICKS = BUILDER
            .comment("Ticks between two looks of a golem waiting for a redstone signal in its program")
            .defineInRange("programSignalCheckTicks", 10, 1, 200);

    public static final ModConfigSpec.IntValue GOLEM_FISH_WAIT_MIN_SECONDS = BUILDER
            .comment("Shortest wait in seconds before a fish bites the line of a fisher golem, Lure takes off as for a player")
            .defineInRange("golemFishWaitMinSeconds", 6, 1, 300);

    public static final ModConfigSpec.IntValue GOLEM_FISH_WAIT_MAX_SECONDS = BUILDER
            .comment("Longest wait in seconds before a fish bites the line of a fisher golem")
            .defineInRange("golemFishWaitMaxSeconds", 34, 1, 600);

    public static final ModConfigSpec.IntValue MARK_LINK_RADIUS = BUILDER
            .comment("Blocks around a storage mark where the golem staff can link containers")
            .defineInRange("markLinkRadius", 8, 1, 32);

    public static final ModConfigSpec.IntValue MARK_MAX_LINKS = BUILDER
            .comment("Containers one storage mark can link with the golem staff")
            .defineInRange("markMaxLinks", 8, 1, 64);

    public static final ModConfigSpec.IntValue GOLEM_SMOKE_CUSHION_REACH = BUILDER
            .comment("Blocks around a hookah where a golem with the pipe looks for a free cushion")
            .defineInRange("golemSmokeCushionReach", 2, 1, 4);

    public static final ModConfigSpec.IntValue GOLEM_SMOKE_PULL_SECONDS = BUILDER
            .comment("Seconds a smoking golem pulls the hookah in each round")
            .defineInRange("golemSmokePullSeconds", 2, 1, 10);

    public static final ModConfigSpec.IntValue GOLEM_SMOKE_ROUND_SECONDS = BUILDER
            .comment("Seconds of one smoking round of a golem, its pull included")
            .defineInRange("golemSmokeRoundSeconds", 8, 2, 60);

    public static final ModConfigSpec.IntValue GOLEMS_PER_HOOKAH = BUILDER
            .comment("Golems that may smoke at one hookah and count in its kaif zone")
            .defineInRange("golemsPerHookah", 2, 0, 8);

    static {
        BUILDER.pop();
    }

    static final ModConfigSpec SPEC = BUILDER.build();
}
