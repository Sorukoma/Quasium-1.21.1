package net.sorukoma.quasium.selection.genetics;

import net.minecraft.util.RandomSource;
import net.sorukoma.quasium.item.TobaccoVarieties;

import java.util.Collection;
import java.util.List;

public final class Genetics {
    public static final int MIN_STARS = 1;
    public static final int MAX_STARS = 5;

    public record Stars(int strength, int yield, int quality) {
    }

    private Genetics() {
    }

    public static int roll(RandomSource random) {
        return random.nextInt(3) - 1;
    }

    public static int inherit(int first, int second, int roll) {
        return clamp(Math.round((first + second) / 2.0f + roll));
    }

    public static int recombine(int hybrid, int pure, int roll) {
        return clamp(Math.round((2 * hybrid + pure) / 3.0f + roll));
    }

    public static double hybridChance(double poleChance, double averageQuality, int qualityBonusPct) {
        return poleChance * (1 + qualityBonusPct / 100.0 * averageQuality);
    }

    public static Stars seedStats(Collection<String> lineage) {
        int strength = 0;
        int yield = 0;
        int quality = 0;
        for (String variety : lineage) {
            strength += TobaccoVarieties.strength(variety);
            yield += TobaccoVarieties.yield(variety);
            quality += TobaccoVarieties.quality(variety);
        }
        float members = lineage.size();
        return new Stars(clamp(Math.round(strength / members)), clamp(Math.round(yield / members)), clamp(Math.round(quality / members)));
    }

    public static int wildStars(RandomSource random, List<? extends Integer> weights) {
        int total = weights.stream().mapToInt(weight -> Math.max(0, weight)).sum();
        return total <= 0 ? MIN_STARS : weightedStars(random.nextInt(total), weights);
    }

    public static int weightedStars(int pick, List<? extends Integer> weights) {
        int left = pick;
        for (int star = 0; star < weights.size(); star++) {
            left -= Math.max(0, weights.get(star));
            if (left < 0) return clamp(star + 1);
        }
        return MIN_STARS;
    }

    public static int clamp(int stars) {
        return Math.max(MIN_STARS, Math.min(MAX_STARS, stars));
    }
}
