package net.sorukoma.quasium.magic;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.Holder;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class AromaEffects {
    public static final int REFRESH_TICKS = 100;
    private static final int EFFECT_TICKS = REFRESH_TICKS + 40;
    private static final int SATURATION_EVERY_TICKS = 600;
    private static final int FIRE_RESIST_TICKS = 100;

    private record Scent(String flavor, long until) {}

    private static final Map<UUID, Scent> ACTIVE = new HashMap<>();
    private static final Map<UUID, Long> LAST_FEED = new HashMap<>();

    private AromaEffects() {
    }

    public static String primaryFlavor(String essence) {
        if (essence == null || essence.isBlank()) return "";
        int plus = essence.indexOf('+');
        return plus < 0 ? essence : essence.substring(0, plus);
    }

    public static void apply(ServerPlayer player, String essence) {
        String flavor = primaryFlavor(essence);
        if (flavor.isEmpty()) return;
        long now = player.level().getGameTime();
        ACTIVE.put(player.getUUID(), new Scent(flavor, now + EFFECT_TICKS));
        switch (flavor) {
            case "mint" -> give(player, MobEffects.DIG_SPEED, EFFECT_TICKS);
            case "lavender" -> {
                player.resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_REST));
                give(player, MobEffects.SLOW_FALLING, EFFECT_TICKS);
            }
            case "coffee" -> give(player, MobEffects.NIGHT_VISION, EFFECT_TICKS + 200);
            case "berry", "watermelon" -> feed(player, now);
            case "ice" -> give(player, MobEffects.FIRE_RESISTANCE, FIRE_RESIST_TICKS + EFFECT_TICKS);
            case "nether" -> give(player, MobEffects.DAMAGE_BOOST, EFFECT_TICKS);
            default -> {
            }
        }
    }

    public static void feed(ServerPlayer player, long now) {
        Long last = LAST_FEED.get(player.getUUID());
        if (last != null && now - last < SATURATION_EVERY_TICKS) return;
        LAST_FEED.put(player.getUUID(), now);
        player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 1, 0, true, false));
    }

    private static void give(ServerPlayer player, Holder<MobEffect> effect, int ticks) {
        player.addEffect(new MobEffectInstance(effect, ticks, 0, true, false, true));
    }

    @Nullable
    public static String activeFlavor(Player player) {
        Scent scent = ACTIVE.get(player.getUUID());
        return scent == null || scent.until < player.level().getGameTime() ? null : scent.flavor;
    }

    public static boolean hidesFromMobs(Player player) {
        return "anise".equals(activeFlavor(player));
    }

    public static float gramsMultiplier(Player player) {
        return "nether".equals(activeFlavor(player)) ? 1.2f : 1f;
    }

    public static void forget(UUID playerId) {
        ACTIVE.remove(playerId);
        LAST_FEED.remove(playerId);
    }
}
