package net.sorukoma.quasium.data;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.sorukoma.quasium.Config;
import net.sorukoma.quasium.network.OverdoseStatusPacket;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerOverdoseData {

    private static final int TOBACCO_TICKS_PER_GRAM = 250;

    // Per-player consumption tracking: per-second accumulator (tick-units)
    private static final Map<UUID, Integer> tickConsumptionAccumulators = new HashMap<>();
    private static final Map<UUID, Long> lastFlushSecond = new HashMap<>();

    // Consumption log: deque of (gameTime, grams) entries
    private static final Map<UUID, ArrayDeque<ConsumptionEntry>> consumptionLogs = new HashMap<>();

    // Current overdose state
    private static final Map<UUID, Boolean> overdoseState = new HashMap<>();

    private static class ConsumptionEntry {
        final long gameTime;
        final float grams;

        ConsumptionEntry(long gameTime, float grams) {
            this.gameTime = gameTime;
            this.grams = grams;
        }
    }

    public static void recordConsumption(ServerPlayer player, int tickUnitsConsumed) {
        UUID uuid = player.getUUID();
        tickConsumptionAccumulators.merge(uuid, tickUnitsConsumed, Integer::sum);
    }

    public static void tickCheck(ServerPlayer player) {
        UUID uuid = player.getUUID();
        long currentTime = player.level().getGameTime();
        long currentSecond = currentTime / 20;

        // Flush per-second accumulator at second boundaries
        Long lastSec = lastFlushSecond.get(uuid);
        if (lastSec == null || currentSecond != lastSec) {
            Integer accumulated = tickConsumptionAccumulators.get(uuid);
            if (accumulated != null && accumulated > 0) {
                float grams = accumulated / (float) TOBACCO_TICKS_PER_GRAM;
                ArrayDeque<ConsumptionEntry> log = consumptionLogs.computeIfAbsent(uuid, k -> new ArrayDeque<>());
                log.addLast(new ConsumptionEntry(currentTime, grams));
                tickConsumptionAccumulators.remove(uuid);
            }
            lastFlushSecond.put(uuid, currentSecond);
        }

        // Purge expired entries and calculate total grams in window
        ArrayDeque<ConsumptionEntry> log = consumptionLogs.get(uuid);
        long windowTicks = Config.OVERDOSE_WINDOW_MINUTES.getAsInt() * 60 * 20;

        if (log != null) {
            long windowStart = currentTime - windowTicks;
            while (!log.isEmpty() && log.getFirst().gameTime < windowStart) {
                log.removeFirst();
            }
        }

        float totalGrams = 0;
        if (log != null) {
            for (ConsumptionEntry entry : log) {
                totalGrams += entry.grams;
            }
        }

        checkOverdoseState(player, totalGrams);
    }

    private static void checkOverdoseState(ServerPlayer player, float totalGrams) {
        UUID uuid = player.getUUID();
        boolean wasOverdosed = overdoseState.getOrDefault(uuid, false);
        float threshold = (float) Config.OVERDOSE_THRESHOLD_GRAMS.getAsDouble();
        boolean isOverdosed = totalGrams >= threshold;

        if (wasOverdosed != isOverdosed) {
            overdoseState.put(uuid, isOverdosed);

            if (isOverdosed) {
                // Kaif drops by 50% immediately on overdose onset
                int currentKaif = PlayerKaifData.getKaifValue(player);
                int halvedKaif = currentKaif / 2;
                PlayerKaifData.setKaifValue(player, halvedKaif);

                player.sendSystemMessage(Component.translatable("quasium.message.overdose_start"));
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 600, 2));
                player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 200, 0));
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 400, 1));
            } else {
                player.sendSystemMessage(Component.translatable("quasium.message.overdose_end"));
            }

            PacketDistributor.sendToPlayer(player, new OverdoseStatusPacket(isOverdosed));
        }
    }

    public static boolean isOverdosed(Player player) {
        return overdoseState.getOrDefault(player.getUUID(), false);
    }

    public static void onLogout(UUID uuid) {
        tickConsumptionAccumulators.remove(uuid);
        lastFlushSecond.remove(uuid);
        consumptionLogs.remove(uuid);
        overdoseState.remove(uuid);
    }

    public static void onRespawn(UUID uuid) {
        consumptionLogs.remove(uuid);
        tickConsumptionAccumulators.remove(uuid);
        lastFlushSecond.remove(uuid);
        overdoseState.put(uuid, false);
    }
}
