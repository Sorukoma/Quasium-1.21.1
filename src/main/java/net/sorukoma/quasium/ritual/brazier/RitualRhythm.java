package net.sorukoma.quasium.ritual.brazier;

import net.sorukoma.quasium.ritual.RitualMath;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class RitualRhythm {
    public enum Kind { HIT, MISS, REPEAT, PAUSED }

    public record Beat(Kind kind, int streak) {
        public static final Beat PAUSED = new Beat(Kind.PAUSED, 0);
    }

    private final double period;
    private final int windowMs;
    private final Map<UUID, Integer> streaks = new HashMap<>();
    private final Map<UUID, Long> scored = new HashMap<>();
    private long judged;

    RitualRhythm(int periodMs, int windowMs) {
        this.period = RitualMath.ticks(periodMs);
        this.windowMs = windowMs;
    }

    public double period() {
        return period;
    }

    Beat press(UUID player, double runTick) {
        long pulse = RitualMath.nearestPulse(runTick, period);
        if (pulse <= judged || !RitualMath.onBeat(runTick, period, windowMs)) {
            streaks.remove(player);
            return new Beat(Kind.MISS, 0);
        }
        if (scored.getOrDefault(player, 0L) == pulse) return new Beat(Kind.REPEAT, streaks.getOrDefault(player, 0));
        scored.put(player, pulse);
        return new Beat(Kind.HIT, streaks.merge(player, 1, Integer::sum));
    }

    boolean due(double runTick, int lagMs) {
        return runTick >= (judged + 1) * period + RitualMath.ticks(windowMs + lagMs);
    }

    boolean judgeNext() {
        long pulse = ++judged;
        streaks.keySet().removeIf(player -> scored.getOrDefault(player, 0L) < pulse);
        return scored.containsValue(pulse);
    }

    void skipNext() {
        judged++;
    }
}
