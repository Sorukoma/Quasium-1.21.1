package net.sorukoma.quasium.magic.page.effect;

public final class ToleranceMath {
    private ToleranceMath() {
    }

    public static float factor(int earlierHits, int freeHits, int stepPct, int maxPct) {
        int tolerated = earlierHits + 1 - freeHits;
        if (tolerated <= 0) return 1f;
        return 1f - Math.min(maxPct, tolerated * stepPct) / 100f;
    }
}
