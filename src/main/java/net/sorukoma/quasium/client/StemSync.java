package net.sorukoma.quasium.client;

import org.lwjgl.openal.AL10;

final class StemSync {
    private final int[] sources;
    private int started;

    StemSync(int stems) {
        sources = new int[stems];
    }

    void started(int stem, int source) {
        sources[stem] = source;
        if (++started < sources.length) return;
        AL10.alSourceRewindv(sources);
        AL10.alSourcePlayv(sources);
    }
}
