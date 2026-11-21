package net.sorukoma.quasium.magic;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.sorukoma.quasium.Config;
import net.sorukoma.quasium.advancements.triggers.SmokecraftTrigger;
import net.sorukoma.quasium.network.ZoneStatePacket;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PlayerZones {
    public static final int NIRVANA = 3;
    private static final int MARK_TTL_TICKS = 25;
    private static final int EFFECT_TICKS = AromaEffects.REFRESH_TICKS + 40;

    private record Mark(int tier, int earned, String flavor, int color, long until) {}

    private static final Map<UUID, Mark> MARKS = new HashMap<>();
    private static final Map<UUID, Integer> SYNCED_TIER = new HashMap<>();
    private static final Map<UUID, Integer> EARNED_TIER = new HashMap<>();
    private static final Map<UUID, Float> KAIF_REMAINDER = new HashMap<>();

    private PlayerZones() {
    }

    public static void offer(Player player, int tier, int earned, String flavor, int color, long now) {
        Mark current = MARKS.get(player.getUUID());
        if (current != null && current.until >= now && current.tier > tier) return;
        MARKS.put(player.getUUID(), new Mark(tier, earned, flavor, color, now + MARK_TTL_TICKS));
    }

    public static int tier(Player player) {
        Mark mark = MARKS.get(player.getUUID());
        return mark == null || mark.until < player.level().getGameTime() ? 0 : mark.tier;
    }

    public static int castDiscountPct(Player player) {
        return tier(player) > 0 ? Config.ZONE_CAST_DISCOUNT_PCT.getAsInt() : 0;
    }

    public static boolean holdsWithoutCough(Player player) {
        return tier(player) >= 2;
    }

    public static int boostKaif(Player player, int amount) {
        if (amount <= 0 || tier(player) < 2) return amount;
        float bonus = KAIF_REMAINDER.getOrDefault(player.getUUID(), 0f) + amount * Config.ZONE_KAIF_BONUS_PCT.getAsInt() / 100f;
        int whole = (int) bonus;
        KAIF_REMAINDER.put(player.getUUID(), bonus - whole);
        return amount + whole;
    }

    public static void tick(ServerPlayer player) {
        long now = player.level().getGameTime();
        if (now % 10 != 0) return;
        int tier = tier(player);
        Mark mark = MARKS.get(player.getUUID());
        int synced = SYNCED_TIER.getOrDefault(player.getUUID(), 0);
        if (tier != synced) {
            SYNCED_TIER.put(player.getUUID(), tier);
            PacketDistributor.sendToPlayer(player, new ZoneStatePacket(tier, mark == null ? 0 : mark.color()));
        }
        int earned = tier == 0 ? 0 : mark.earned();
        if (earned != EARNED_TIER.getOrDefault(player.getUUID(), 0)) {
            EARNED_TIER.put(player.getUUID(), earned);
            for (int reached = 1; reached <= earned; reached++) SmokecraftTrigger.fire(player, "zone_" + reached);
        }
        if (tier == 0 || now % AromaEffects.REFRESH_TICKS != 0) return;
        if (tier >= 2) AromaEffects.apply(player, mark.flavor());
        if (tier >= NIRVANA) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, EFFECT_TICKS, 0, true, false, true));
            AromaEffects.feed(player, now);
        }
    }

    public static void forget(UUID playerId) {
        MARKS.remove(playerId);
        SYNCED_TIER.remove(playerId);
        EARNED_TIER.remove(playerId);
        KAIF_REMAINDER.remove(playerId);
    }
}
