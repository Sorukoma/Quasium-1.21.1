package net.sorukoma.quasium.util;

public final class HookahLogic {
    private HookahLogic() { }

    public enum SmokingStage { WARMUP, PEAK, TAIL }

    public static SmokingStage getSmokingStage(int warmupPuffsRemaining, int remainingGrams) {
        if (warmupPuffsRemaining > 0) return SmokingStage.WARMUP;
        if (remainingGrams <= 5) return SmokingStage.TAIL;
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
}
