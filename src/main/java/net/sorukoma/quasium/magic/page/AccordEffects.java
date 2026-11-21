package net.sorukoma.quasium.magic.page;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public final class AccordEffects {
    private static final Map<String, AccordBehavior> BEHAVIORS = new HashMap<>();

    static {
        net.sorukoma.quasium.magic.page.effect.Accords.all().forEach(AccordEffects::register);
    }

    private AccordEffects() {
    }

    static void register(String id, AccordBehavior behavior) {
        BEHAVIORS.put(id, behavior);
    }

    @Nullable
    public static AccordBehavior get(String id) {
        return BEHAVIORS.get(id);
    }
}
