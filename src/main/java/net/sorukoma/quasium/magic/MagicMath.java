package net.sorukoma.quasium.magic;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MagicMath {
    public static final int TICKS_PER_SECOND = 20;
    public static final long RING_SWEEP_MS = 800;
    private static final int RING_TRUST_MS = 150;

    private MagicMath() {
    }

    public static float fillLungs(float lungs, float capacity, float fillPerSecond, float efficiency) {
        return Math.min(capacity, lungs + fillPerSecond / TICKS_PER_SECOND * Math.max(0f, efficiency));
    }

    public static float holdMultiplier(int holdTicks, int chargePerSecondPct, int maxMultiplierPct) {
        float multiplier = 1f + holdTicks / (float) TICKS_PER_SECOND * chargePerSecondPct / 100f;
        return Math.min(Math.max(1f, maxMultiplierPct / 100f), multiplier);
    }

    public static boolean coughs(int holdTicks, int coughAfterSeconds) {
        return holdTicks >= coughAfterSeconds * TICKS_PER_SECOND;
    }

    public static Map<String, Integer> countEssences(List<String> essences) {
        Map<String, Integer> counts = new HashMap<>();
        for (String essence : essences) {
            if (essence != null && !essence.isBlank()) counts.merge(essence, 1, Integer::sum);
        }
        return counts;
    }

    public static int matchAlternative(List<Map<String, Integer>> alternatives, List<String> essences) {
        Map<String, Integer> counts = countEssences(essences);
        if (counts.isEmpty()) return -1;
        for (int i = 0; i < alternatives.size(); i++) {
            if (alternatives.get(i).equals(counts)) return i;
        }
        return -1;
    }

    public static boolean fitsInBuffers(Map<String, Integer> buffers, Map<String, Integer> incoming, int capacity) {
        for (Map.Entry<String, Integer> note : incoming.entrySet()) {
            if (buffers.getOrDefault(note.getKey(), 0) + note.getValue() > capacity) return false;
        }
        return true;
    }

    public static int fuelTicksNeeded(int processTicks, int fuelTicksPerItem) {
        return Math.max(1, Math.round(fuelTicksPerItem / (float) Math.max(1, processTicks)));
    }

    public static int withDiscount(int cost, int discountPct) {
        return Math.max(0, Math.round(cost * (100 - discountPct) / 100f));
    }

    public static long ringOffsetMs(long elapsedMs) {
        return Math.abs(elapsedMs % RING_SWEEP_MS - RING_SWEEP_MS / 2);
    }

    public static int checkedRingOffset(int claimedMs, long serverElapsedMs) {
        int measured = (int) ringOffsetMs(serverElapsedMs);
        int claimed = Math.max(0, claimedMs);
        return Math.abs(claimed - measured) <= RING_TRUST_MS ? claimed : measured;
    }
}
