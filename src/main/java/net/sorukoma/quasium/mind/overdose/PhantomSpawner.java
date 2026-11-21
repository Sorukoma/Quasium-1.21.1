package net.sorukoma.quasium.mind.overdose;

import net.minecraft.server.level.ServerPlayer;
import net.sorukoma.quasium.data.PlayerOverdoseData;
import net.sorukoma.quasium.mind.MindConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PhantomSpawner {
    private static final Map<UUID, Long> NEXT = new HashMap<>();
    private static final double HAUNT_RANGE = 32;

    private PhantomSpawner() {
    }

    public static void tick(ServerPlayer player) {
        if (PlayerOverdoseData.stage(player) < 2 || KaifComa.inComa(player)) {
            NEXT.remove(player.getUUID());
            return;
        }
        long now = player.level().getGameTime();
        if (now < NEXT.getOrDefault(player.getUUID(), now)) return;
        int min = MindConfig.PHANTOM_SPAWN_MIN_SECONDS.getAsInt() * 20;
        int max = Math.max(min, MindConfig.PHANTOM_SPAWN_MAX_SECONDS.getAsInt() * 20);
        NEXT.put(player.getUUID(), now + player.getRandom().nextIntBetweenInclusive(min, max));
        if (haunting(player) < MindConfig.PHANTOM_MAX_NEARBY.getAsInt()) player.level().addFreshEntity(SmokePhantom.near(player));
    }

    public static int haunting(ServerPlayer player) {
        return player.level().getEntitiesOfClass(SmokePhantom.class, player.getBoundingBox().inflate(HAUNT_RANGE),
                phantom -> phantom.haunts(player)).size();
    }

    public static void forget(UUID player) {
        NEXT.remove(player);
    }
}
