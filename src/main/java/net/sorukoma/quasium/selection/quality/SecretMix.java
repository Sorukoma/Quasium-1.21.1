package net.sorukoma.quasium.selection.quality;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public final class SecretMix {
    public static final List<String> FLAVORS = List.of(
            "mint", "berry", "lemon", "lime", "orange", "grapefruit", "ice", "honey", "nether", "lavender", "cocoa",
            "apple", "watermelon", "grape", "cherry", "vanilla", "coffee", "cinnamon", "coconut", "rose", "anise");

    private static final int SIZE = 2;
    private static final long SALT = 0x5EC7E7L;

    private SecretMix() {
    }

    public static List<String> flavors(long worldSeed) {
        List<String> pool = new ArrayList<>(FLAVORS);
        Collections.shuffle(pool, new Random(worldSeed ^ SALT));
        return pool.subList(0, SIZE).stream().sorted().toList();
    }

    public static boolean matches(long worldSeed, String essenceTypes) {
        Set<String> used = Arrays.stream(essenceTypes.split("\\+"))
                .filter(type -> !type.isBlank())
                .collect(Collectors.toSet());
        return used.equals(Set.copyOf(flavors(worldSeed)));
    }

    public static int kaifPct(long worldSeed, String essenceTypes, int kaifPct, int bonusPct) {
        return matches(worldSeed, essenceTypes) ? kaifPct * (100 + bonusPct) / 100 : kaifPct;
    }
}
