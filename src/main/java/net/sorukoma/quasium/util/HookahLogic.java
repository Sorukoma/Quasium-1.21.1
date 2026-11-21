package net.sorukoma.quasium.util;

import net.sorukoma.quasium.component.ModDataComponents.BowlTobaccoGroup;
import net.sorukoma.quasium.component.ModDataComponents.HookahBowlContents;
import net.sorukoma.quasium.item.TobaccoVarieties;
import net.sorukoma.quasium.magic.SmokeSource;
import org.jetbrains.annotations.Nullable;

public final class HookahLogic {
    public static final int WARMUP_PUFFS = 10;
    public static final int TAIL_GRAMS = 5;
    public static final float HEAT_REGULAR = 1.0f;
    public static final float HEAT_SELF_MIXED = 1.5f;
    public static final float HEAT_PESCHANIK = 2.0f;
    public static final float COOLER_FACTOR = 0.8f;
    public static final float OVERHEAT_FACTOR = 1.8f;
    public static final int OVERFILL_GRAMS = 15;
    public static final float LIT_SHARE = 0.95f;
    public static final int COALS_HOLD_HEAT_TICKS = 600;

    private HookahLogic() { }

    public enum SmokingStage { WARMUP, PEAK, TAIL }

    public enum Mood { COLD, WARMUP, PEAK, TAIL, OVERHEAT }

    public static SmokingStage getSmokingStage(int warmupPuffsRemaining, int remainingGrams) {
        if (warmupPuffsRemaining > 0) return SmokingStage.WARMUP;
        if (remainingGrams <= TAIL_GRAMS) return SmokingStage.TAIL;
        return SmokingStage.PEAK;
    }

    public static float getPuffEfficiency(float heatRatio, SmokingStage stage, boolean diffuser) {
        float efficiency = heatRatio >= 0.6f ? 1.0f : Math.max(0f, heatRatio / 0.6f * 0.8f);
        if (diffuser) efficiency *= 1.1f;
        efficiency *= switch (stage) {
            case WARMUP -> 0.2f;
            case PEAK -> 1.0f;
            case TAIL -> 0.7f;
        };
        return Math.min(1.0f, efficiency);
    }

    public static int calculateKaif(int baseKaif, float efficiency, float strength) {
        return Math.max(0, Math.round(baseKaif * efficiency * Math.max(0.1f, strength)));
    }

    public static float strength(@Nullable HookahBowlContents contents, double regular, double empty) {
        if (contents == null) return (float) empty;
        return (float) contents.slotAssignments().values().stream().map(contents.groups()::get)
                .mapToDouble(group -> group.isRegular() ? regular : group.strength() * group.kaifPct() / 100.0)
                .average().orElse(empty);
    }

    public static float heatThreshold(@Nullable HookahBowlContents contents) {
        if (contents == null) return HEAT_REGULAR;
        boolean selfMixed = false;
        for (BowlTobaccoGroup group : contents.groups()) {
            if (!group.isSelfMixed()) continue;
            if (TobaccoVarieties.PESCHANIK.equals(group.base())) return HEAT_PESCHANIK;
            selfMixed = true;
        }
        return selfMixed ? HEAT_SELF_MIXED : HEAT_REGULAR;
    }

    public static float drawHeat(float threshold, boolean cooler) {
        return cooler ? threshold * COOLER_FACTOR : threshold;
    }

    public static boolean lit(float heat, float threshold, boolean cooler) {
        return heat >= drawHeat(threshold, cooler) * LIT_SHARE;
    }

    public static float overheatLine(float threshold) {
        return threshold * OVERHEAT_FACTOR;
    }

    public static float burnPerTick(float heat, float threshold, int occupiedSlots) {
        float rate = heat > overheatLine(threshold) ? 2f : 1f;
        return occupiedSlots > OVERFILL_GRAMS ? rate * 1.5f : rate;
    }

    public static int stackedEffectTicks(int remainingTicks, int addedTicks, int capTicks) {
        int longest = Math.max(remainingTicks, addedTicks);
        return Math.max(longest, Math.min(remainingTicks + addedTicks, capTicks));
    }

    public static Mood mood(float heat, float threshold, boolean cooler, int warmupPuffsRemaining, int tobaccoUnits) {
        if (tobaccoUnits <= 0 || !lit(heat, threshold, cooler)) return Mood.COLD;
        if (heat > overheatLine(threshold)) return Mood.OVERHEAT;
        return switch (getSmokingStage(warmupPuffsRemaining, tobaccoUnits / SmokeSource.UNITS_PER_GRAM)) {
            case WARMUP -> Mood.WARMUP;
            case PEAK -> Mood.PEAK;
            case TAIL -> Mood.TAIL;
        };
    }
}
