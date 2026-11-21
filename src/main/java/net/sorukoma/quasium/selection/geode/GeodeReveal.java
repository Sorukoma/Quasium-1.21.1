package net.sorukoma.quasium.selection.geode;

import net.sorukoma.quasium.progression.ConfigArgs;
import net.sorukoma.quasium.selection.SelectionConfig;

public final class GeodeReveal {
    private GeodeReveal() {
    }

    public static boolean revealed(int kaif) {
        return revealed(kaif, ConfigArgs.synced(SelectionConfig.GEODE_REVEAL_KAIF));
    }

    public static boolean revealed(int kaif, int revealKaif) {
        return kaif >= revealKaif;
    }
}
