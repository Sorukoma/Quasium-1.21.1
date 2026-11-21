package net.sorukoma.quasium.ritual;

import java.math.BigDecimal;

public final class RitualMath {
    public static final int TICKS_PER_SECOND = 20;
    public static final int MS_PER_TICK = 1000 / TICKS_PER_SECOND;
    private static final double SLACK = 1e-4;

    public record Weights(int hitRelief, int missPenalty, int emptyPenalty, int driftDivisor, int kaifDivisor, int groupBonus,
                          int filterBonus) {
        public static Weights current() {
            return new Weights(RitualConfig.HIT_RELIEF.getAsInt(), RitualConfig.MISS_PENALTY.getAsInt(),
                    RitualConfig.EMPTY_PULSE_PENALTY.getAsInt(), RitualConfig.DRIFT_DIVISOR.getAsInt(), RitualConfig.KAIF_DIVISOR.getAsInt(),
                    RitualConfig.GROUP_BONUS.getAsInt(), RitualConfig.FILTER_BONUS.getAsInt());
        }
    }

    private RitualMath() {
    }

    public static double ticks(int ms) {
        return ms / (double) MS_PER_TICK;
    }

    public static String seconds(int ms) {
        return BigDecimal.valueOf(ms, 3).stripTrailingZeros().toPlainString();
    }

    public static long nearestPulse(double runTick, double period) {
        return Math.round(runTick / period);
    }

    public static double offsetMs(double runTick, double period) {
        return (runTick - nearestPulse(runTick, period) * period) * MS_PER_TICK;
    }

    public static boolean onBeat(double runTick, double period, int windowMs) {
        return nearestPulse(runTick, period) >= 1 && Math.abs(offsetMs(runTick, period)) <= windowMs;
    }

    public static float streakMultiplier(int streak, int fullStreak, int maxPct) {
        float top = maxPct / 100f;
        if (fullStreak <= 1) return top;
        int counted = Math.clamp(streak, 1, fullStreak);
        return 1f + (top - 1f) * (counted - 1) / (fullStreak - 1);
    }

    public static int workForPuffs(int puffs, int hitsPer100Puffs) {
        return Math.max(1, (puffs * hitsPer100Puffs + 99) / 100);
    }

    public static int cleanHits(int work, int fullStreak, int maxPct) {
        int hits = 0;
        double done = 0;
        while (!reached(done, work)) done += streakMultiplier(++hits, fullStreak, maxPct);
        return hits;
    }

    public static boolean reached(double done, int work) {
        return done >= work - SLACK;
    }

    public static float drift(int base, float averageKaif, boolean group, boolean filter, Weights weights) {
        float pressure = base - averageKaif / Math.max(1, weights.kaifDivisor()) - (group ? weights.groupBonus() : 0)
                - (filter ? weights.filterBonus() : 0);
        return Math.max(0f, pressure) / Math.max(1, weights.driftDivisor());
    }

    public static float emptyPulse(int penalty, float coverage) {
        return penalty * (1f - Math.clamp(coverage, 0f, 1f));
    }

    public static double networkWork(float efficiency, int networkPct, int maxPct, double period) {
        return Math.clamp(efficiency, 0f, 1f) * maxPct / 100.0 * networkPct / 100.0 / period;
    }

    public static float draftFactor(float draftPerSecond, int rate) {
        return rate <= 0 ? 1f : Math.min(1f, Math.max(0f, draftPerSecond / rate));
    }

    public static int dodgePct(int basePct, int threadedPieces, int perPiecePct, int capPct) {
        if (threadedPieces <= 0) return basePct;
        return Math.max(basePct, Math.min(capPct, basePct + threadedPieces * perPiecePct));
    }

    public static float novaDamage(float charge, int novaPct) {
        return charge * novaPct / 100f;
    }
}
