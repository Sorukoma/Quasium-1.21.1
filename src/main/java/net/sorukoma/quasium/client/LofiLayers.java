package net.sorukoma.quasium.client;

import net.sorukoma.quasium.magic.KaifZones;

import java.util.Arrays;

public final class LofiLayers {
    public static final int KEYS = 0;
    public static final int LEAD = 3;
    static final int FADE_TICKS = 40;
    static final int HOLD_TICKS = KaifZones.TIER_HOLD_TICKS;

    private final int[] hold = new int[LEAD + 1];
    private final int[] fade = new int[LEAD + 1];
    private boolean nirvanaLeft;

    public void tick(int tier) {
        boolean nirvanaHeld = hold[LEAD] > 0;
        for (int stem = KEYS + 1; stem <= LEAD; stem++) {
            hold[stem] = tier >= stem ? HOLD_TICKS : Math.max(0, hold[stem] - 1);
            fade[stem] += Integer.signum((hold[stem] > 0 ? FADE_TICKS : 0) - fade[stem]);
        }
        nirvanaLeft = nirvanaHeld && hold[LEAD] == 0;
    }

    public boolean shouldScratch() {
        return nirvanaLeft;
    }

    public float volume(int stem) {
        if (stem == KEYS) return 1f;
        float faded = fade[stem] / (float) FADE_TICKS;
        return faded * faded;
    }

    public void reset() {
        Arrays.fill(hold, 0);
        Arrays.fill(fade, 0);
        nirvanaLeft = false;
    }
}
