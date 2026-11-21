package net.sorukoma.quasium.data;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import net.sorukoma.quasium.Config;
import net.sorukoma.quasium.advancements.triggers.SmokecraftTrigger;
import net.sorukoma.quasium.api.TobaccoConsumptionEvent;
import net.sorukoma.quasium.component.ModDataComponents;
import net.sorukoma.quasium.mind.MindConfig;
import net.sorukoma.quasium.mind.MindData;
import net.sorukoma.quasium.mind.MindRegistry;
import net.sorukoma.quasium.mind.MindStats;
import net.sorukoma.quasium.mind.Tolerance;
import net.sorukoma.quasium.mind.overdose.KaifComa;
import net.sorukoma.quasium.mind.research.Perk;
import net.sorukoma.quasium.mind.research.ResearchRules;
import net.sorukoma.quasium.network.OverdoseStatusPacket;
import net.sorukoma.quasium.tea.VigorEffect;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerOverdoseData {

    private static final int TOBACCO_TICKS_PER_GRAM = 250;
    private static final int UNITS_PER_TENTH = TOBACCO_TICKS_PER_GRAM / 10;

    private static final Map<UUID, Integer> tickConsumptionAccumulators = new HashMap<>();
    private static final Map<UUID, Long> lastFlushSecond = new HashMap<>();
    private static final Map<UUID, ArrayDeque<ConsumptionEntry>> consumptionLogs = new HashMap<>();
    private static final Map<UUID, Integer> stages = new HashMap<>();
    private static final Map<UUID, Float> windowGrams = new HashMap<>();
    private static final Map<UUID, Integer> unitsToStat = new HashMap<>();

    private record ConsumptionEntry(long gameTime, float grams) {
    }

    public static void recordConsumption(ServerPlayer player, int tickUnitsConsumed) {
        recordConsumption(player, tickUnitsConsumed, null);
    }

    public static void recordConsumption(ServerPlayer player, int tickUnitsConsumed, @Nullable ModDataComponents.HookahBowlContents bowl) {
        TobaccoConsumptionEvent event = NeoForge.EVENT_BUS.post(new TobaccoConsumptionEvent(player, tickUnitsConsumed));
        if (event.isCanceled() || event.units() <= 0) return;
        tickConsumptionAccumulators.merge(player.getUUID(), event.units(), Integer::sum);
        Tolerance.smoked(player, bowl, event.units());
    }

    public static void tickCheck(ServerPlayer player) {
        UUID uuid = player.getUUID();
        long currentTime = player.level().getGameTime();
        long currentSecond = currentTime / 20;

        Long lastSec = lastFlushSecond.get(uuid);
        if (lastSec == null || currentSecond != lastSec) {
            Integer accumulated = tickConsumptionAccumulators.remove(uuid);
            if (accumulated != null && accumulated > 0) {
                float grams = accumulated / (float) TOBACCO_TICKS_PER_GRAM;
                consumptionLogs.computeIfAbsent(uuid, k -> new ArrayDeque<>()).addLast(new ConsumptionEntry(currentTime, grams));
                int units = unitsToStat.getOrDefault(uuid, 0) + accumulated;
                unitsToStat.put(uuid, units % UNITS_PER_TENTH);
                MindStats.award(player, MindRegistry.GRAMS_SMOKED, units / UNITS_PER_TENTH);
            }
            lastFlushSecond.put(uuid, currentSecond);
        }

        ArrayDeque<ConsumptionEntry> log = consumptionLogs.get(uuid);
        float totalGrams = 0;
        if (log != null) {
            long windowStart = currentTime - Config.OVERDOSE_WINDOW_MINUTES.getAsInt() * 60L * 20L;
            while (!log.isEmpty() && log.getFirst().gameTime() < windowStart) log.removeFirst();
            for (ConsumptionEntry entry : log) totalGrams += entry.grams();
        }
        windowGrams.put(uuid, totalGrams);
        checkStage(player, stageFor(player, totalGrams));
    }

    public static int stageFor(Player player, float grams) {
        float extra = stoneFaceGrams(player);
        if (grams >= MindConfig.OVERDOSE_STAGE3_GRAMS.get() + extra) return 3;
        if (grams >= MindConfig.OVERDOSE_STAGE2_GRAMS.get() + extra) return 2;
        return grams >= Config.OVERDOSE_THRESHOLD_GRAMS.get() + extra ? 1 : 0;
    }

    private static void checkStage(ServerPlayer player, int stage) {
        if (VigorEffect.pausesOverdose(player)) return;
        int before = stages.getOrDefault(player.getUUID(), 0);
        if (stage == before) return;
        stages.put(player.getUUID(), stage);
        if (before == 0) onset(player);
        if (stage == 0) {
            player.sendSystemMessage(Component.translatable("quasium.message.overdose_end"));
            PacketDistributor.sendToPlayer(player, new OverdoseStatusPacket(false));
        }
        if (stage > before && stage >= 2) {
            MindData.raise(player, ResearchRules.LIVED);
            player.sendSystemMessage(Component.translatable("quasium.message.overdose_stage_" + stage));
            SmokecraftTrigger.fire(player, "overdose_" + stage);
        }
        if (stage == 3 && before < 3) KaifComa.enter(player);
        MindData.changed(player);
    }

    private static void onset(ServerPlayer player) {
        PlayerKaifData.setKaifValue(player, PlayerKaifData.getKaifValue(player) / 2);
        player.sendSystemMessage(Component.translatable("quasium.message.overdose_start"));
        SmokecraftTrigger.fire(player, "overdose");
        MindStats.award(player, MindRegistry.OVERDOSES, 1);
        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 600, 2));
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 200, 0));
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 400, 1));
        PacketDistributor.sendToPlayer(player, new OverdoseStatusPacket(true));
    }

    public static boolean isOverdosed(Player player) {
        return stage(player) >= 1;
    }

    public static int stage(Player player) {
        return stages.getOrDefault(player.getUUID(), 0);
    }

    public static float windowGrams(Player player) {
        return windowGrams.getOrDefault(player.getUUID(), 0f);
    }

    public static float windowShare(Player player) {
        return windowGrams(player) / (float) (Config.OVERDOSE_THRESHOLD_GRAMS.get() + stoneFaceGrams(player));
    }

    private static float stoneFaceGrams(Player player) {
        return Perk.STONE_FACE.ownedBy(player) ? MindConfig.STONE_FACE_GRAMS.getAsInt() : 0;
    }

    public static void clearWindow(ServerPlayer player) {
        consumptionLogs.remove(player.getUUID());
        tickConsumptionAccumulators.remove(player.getUUID());
        windowGrams.remove(player.getUUID());
    }

    public static void onLogout(UUID uuid) {
        tickConsumptionAccumulators.remove(uuid);
        lastFlushSecond.remove(uuid);
        consumptionLogs.remove(uuid);
        stages.remove(uuid);
        windowGrams.remove(uuid);
        unitsToStat.remove(uuid);
    }

    public static void onRespawn(UUID uuid) {
        consumptionLogs.remove(uuid);
        tickConsumptionAccumulators.remove(uuid);
        lastFlushSecond.remove(uuid);
        windowGrams.remove(uuid);
        stages.put(uuid, 0);
    }
}
