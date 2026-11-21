package net.sorukoma.quasium.selection.quality;

import net.sorukoma.quasium.selection.genetics.Genetics;

import java.util.List;

public final class QualityMath {
    private QualityMath() {
    }

    public static int atHarvest(boolean exactMature, boolean wetSoil, boolean workedHive) {
        return Genetics.clamp(Genetics.MIN_STARS + star(exactMature) + star(wetSoil) + star(workedHive));
    }

    public static int fermentedClean(int stars) {
        return Genetics.clamp(stars + 1);
    }

    public static int perStar(List<? extends Integer> table, int stars) {
        if (table.isEmpty()) return 0;
        return table.get(Math.max(0, Math.min(table.size() - 1, stars - 1)));
    }

    public static double aphidChance(int dailyPct, boolean flowersNear, int flowerReductionPct) {
        double chance = dailyPct / 100.0;
        return flowersNear ? chance * (1 - flowerReductionPct / 100.0) : chance;
    }

    public static int growthPct(boolean infested, int basePct, int aphidGrowthPct) {
        return infested ? basePct * aphidGrowthPct / 100 : basePct;
    }

    private static int star(boolean earned) {
        return earned ? 1 : 0;
    }
}
