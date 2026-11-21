package net.sorukoma.quasium.magic.page;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public final class HeartEffects {
    private static final Map<String, HeartBehavior> BEHAVIORS = new HashMap<>();

    static {
        CoreHearts.registerAll();
        net.sorukoma.quasium.magic.page.effect.NoteHearts.all().forEach(HeartEffects::register);
    }

    private HeartEffects() {
    }

    static void register(String id, HeartBehavior behavior) {
        BEHAVIORS.put(id, behavior);
    }

    @Nullable
    public static HeartBehavior get(String id) {
        return BEHAVIORS.get(id);
    }

    public static boolean has(String id) {
        return BEHAVIORS.containsKey(id);
    }
}
