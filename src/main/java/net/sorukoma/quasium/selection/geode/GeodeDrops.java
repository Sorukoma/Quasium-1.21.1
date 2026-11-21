package net.sorukoma.quasium.selection.geode;

import net.minecraft.util.RandomSource;
import net.sorukoma.quasium.selection.SelectionConfig;

import java.util.function.IntUnaryOperator;

public final class GeodeDrops {
    private static final int KAIF_PER_FORTUNE = 30;
    private static final int MAX_FORTUNE = 3;

    private GeodeDrops() {
    }

    public static int clusterCount(int kaif, RandomSource random) {
        return clusterCount(kaif, SelectionConfig.CLUSTER_DROP_MIN_KAIF.get(), SelectionConfig.CLUSTER_DROP_MIN.get(),
                SelectionConfig.CLUSTER_DROP_MAX.get(), random::nextInt);
    }

    public static int clusterCount(int kaif, int minKaif, int min, int max, IntUnaryOperator nextInt) {
        if (kaif < minKaif) return 0;
        int fortune = Math.min(MAX_FORTUNE, (kaif - minKaif) / KAIF_PER_FORTUNE);
        return min + nextInt.applyAsInt(Math.max(0, Math.min(max - min, fortune)) + 1);
    }
}
