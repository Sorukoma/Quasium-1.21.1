package net.sorukoma.quasium.client;

import java.util.Collection;
import java.util.Set;

public final class ClientHybrids {
    private static volatile Set<String> known = Set.of();

    private ClientHybrids() {
    }

    public static Set<String> known() {
        return known;
    }

    public static void set(Collection<String> keys) {
        known = Set.copyOf(keys);
    }
}
