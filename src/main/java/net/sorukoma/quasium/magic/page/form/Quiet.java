package net.sorukoma.quasium.magic.page.form;

import net.sorukoma.quasium.magic.CastContext;

public final class Quiet {
    private static final int PUFF_EVERY_TICKS = 4;

    private Quiet() {
    }

    public static boolean of(CastContext ctx) {
        return ctx.spell().param("silent", 0f) > 0f;
    }

    public static boolean skips(boolean quiet, int tick) {
        return quiet && tick % PUFF_EVERY_TICKS != 0;
    }

    public static int stride(boolean quiet) {
        return quiet ? PUFF_EVERY_TICKS : 1;
    }
}
