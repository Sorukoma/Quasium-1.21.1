package net.sorukoma.quasium.ritual.brazier;

import net.minecraft.nbt.CompoundTag;
import net.sorukoma.quasium.ritual.RitualMath;

public record RitualPulse(long origin, int periodMs, int windowMs) {
    public double period() {
        return RitualMath.ticks(periodMs);
    }

    public double runTime(double gameTime) {
        return gameTime - origin;
    }

    public long lastPulse(double gameTime) {
        return (long) Math.floor(runTime(gameTime) / period());
    }

    public double phase(double gameTime) {
        double beats = runTime(gameTime) / period();
        return beats - Math.floor(beats);
    }

    public boolean onBeat(double gameTime) {
        return RitualMath.onBeat(runTime(gameTime), period(), windowMs);
    }

    CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("Origin", origin);
        tag.putInt("PeriodMs", periodMs);
        tag.putInt("WindowMs", windowMs);
        return tag;
    }

    static RitualPulse load(CompoundTag tag) {
        return new RitualPulse(tag.getLong("Origin"), Math.max(1, tag.getInt("PeriodMs")), tag.getInt("WindowMs"));
    }
}
