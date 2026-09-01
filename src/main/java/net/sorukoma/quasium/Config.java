package net.sorukoma.quasium;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.DoubleValue QUASIUM_ARMOR_DODGE_CHANCE = BUILDER
            .comment("The chance to dodge an attack when wearing a full set of Quasium armor (0.0 to 1.0)")
            .defineInRange("quasiumArmorDodgeChance", 0.8, 0.0, 1.0);

    public static final ModConfigSpec.IntValue KAIF_PER_SECOND_SMOKING = BUILDER
            .comment("How much kaif is gained per second of smoking hookah")
            .defineInRange("kaifPerSecondSmoking", 2, 0, 100);

    public static final ModConfigSpec.DoubleValue OVERDOSE_THRESHOLD_GRAMS = BUILDER
            .comment("Grams of tobacco consumed within the overdose window that triggers overdose")
            .defineInRange("overdoseThresholdGrams", 50.0, 1.0, 100.0);

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

    static final ModConfigSpec SPEC = BUILDER.build();
}
