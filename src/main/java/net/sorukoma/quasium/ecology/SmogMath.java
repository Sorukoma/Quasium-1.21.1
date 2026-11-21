package net.sorukoma.quasium.ecology;

public final class SmogMath {
    public static final int MAX_LEVEL = 5;
    public static final int TICKS_PER_MINUTE = 1200;

    public record Rules(int maxDensity, int onsetMinutes, int resetDensity, int fadeMinutes) {
    }

    public record Minute(int density, int level, int fullMinutes, int cleanMinutes) {
    }

    private SmogMath() {
    }

    public static Minute minute(Minute before, int removed, Rules rules) {
        int density = before.density();
        int level = before.level();
        int full = density >= rules.maxDensity() ? before.fullMinutes() + 1 : 0;
        if (full >= rules.onsetMinutes()) {
            level = Math.min(MAX_LEVEL, level + 1);
            density = Math.min(density, rules.resetDensity());
            full = 0;
        }
        density = Math.max(0, density - Math.max(0, removed));
        int clean = density == 0 && level > 0 ? before.cleanMinutes() + 1 : 0;
        if (clean >= rules.fadeMinutes()) {
            level--;
            clean = 0;
        }
        return new Minute(density, level, full, clean);
    }

    public static int treeRemoval(int trees, double perTree, int cap, double roll) {
        return Math.min(cap, randomRound(trees * perTree, roll));
    }

    public static int withBonus(int amount, int pct, double roll) {
        return amount + randomRound(amount * pct / 100.0, roll);
    }

    public static int randomRound(double value, double roll) {
        int whole = (int) Math.floor(value);
        return whole + (roll < value - whole ? 1 : 0);
    }

    public static int spreadChancePct(int level, int pctPerLevel) {
        return level * pctPerLevel;
    }

    public static int spreadTargets(int level, int neighboursAtMax) {
        return level >= MAX_LEVEL ? neighboursAtMax : 1;
    }

    public static int dymnikCap(int level, int minSmog, int perLevel) {
        return level < minSmog ? 0 : (level - minSmog + 1) * perLevel;
    }

    public static int ticksBetween(int perMinute) {
        return perMinute <= 0 ? Integer.MAX_VALUE : Math.max(1, TICKS_PER_MINUTE / perMinute);
    }
}
