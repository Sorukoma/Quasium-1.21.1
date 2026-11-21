package net.sorukoma.quasium.event;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.minecraft.world.phys.AABB;
import net.sorukoma.quasium.Config;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.block.entity.HookahBlockEntity;
import net.sorukoma.quasium.command.FlavorPreferenceCommand;
import net.sorukoma.quasium.command.GolemCommand;
import net.sorukoma.quasium.command.KaifCommand;
import net.sorukoma.quasium.data.PlayerKaifData;
import net.sorukoma.quasium.data.PlayerOreLuckData;
import net.sorukoma.quasium.data.PlayerOverdoseData;
import net.sorukoma.quasium.data.PlayerTobaccoData;
import net.sorukoma.quasium.entity.HookahConnectionEntity;
import net.sorukoma.quasium.network.ProgressionArticlesSyncPacket;
import net.sorukoma.quasium.progression.BookRewards;
import net.sorukoma.quasium.progression.ProgressionArticle;
import net.sorukoma.quasium.progression.manager.ProgressionArticleManager;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID)
public class ModEvents {
    private static final String LAST_KAIF_DECREASE_TICK_KEY = "QuasiumLastKaifDecreaseTick";
    private static final String LAST_KAIF_RECOVERY_TICK_KEY = "QuasiumLastKaifRecoveryTick";
    private static final String LAST_NEGATIVE_KAIF_NAUSEA_TICK_KEY = "QuasiumLastNegativeKaifNauseaTick";
    private static final int KAIF_DECREASE_INTERVAL_TICKS = 20 * 10;  // every 10 seconds
    private static final int KAIF_RECOVERY_INTERVAL_TICKS = 20 * 30;  // every 30 seconds (3x slower than decrease)
    private static final int KAIF_OVERDOSE_DECAY_INTERVAL_TICKS = 20 * 5;  // every 5 seconds during overdose
    private static final int NEGATIVE_KAIF_NAUSEA_INTERVAL_TICKS = 20 * 15;  // every 15 seconds

    @SubscribeEvent
    public static void onLivingTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide) {
            long currentTick = player.level().getGameTime();
            int currentKaif = PlayerKaifData.getKaifValue(player);
            boolean isOverdosed = PlayerOverdoseData.isOverdosed(player);

            // Overdose: kaif drops every 5 seconds regardless of sign (even below 0)
            if (isOverdosed) {
                long lastOverdoseDecayTick = player.getPersistentData().getLong("QuasiumLastOverdoseDecayTick");
                if (currentTick - lastOverdoseDecayTick >= KAIF_OVERDOSE_DECAY_INTERVAL_TICKS
                        && !net.sorukoma.quasium.tea.VigorEffect.pausesOverdose(player)) {
                    PlayerKaifData.subtractKaif(player, 2);
                    player.getPersistentData().putLong("QuasiumLastOverdoseDecayTick", currentTick);
                }
            } else {
                // Normal: positive kaif decreases over time
                long lastDecreaseTick = player.getPersistentData().getLong(LAST_KAIF_DECREASE_TICK_KEY);
                int decayPct = (net.sorukoma.quasium.entity.SeatEntity.isSeated(player) ? Config.SEATED_KAIF_DECAY_PCT.getAsInt() : 100)
                        * net.sorukoma.quasium.mind.KaifDecay.percent(player) / 100;
                long decayInterval = decayPct <= 0 ? Long.MAX_VALUE : KAIF_DECREASE_INTERVAL_TICKS * 100L / decayPct;
                if (currentTick - lastDecreaseTick >= decayInterval) {
                    if (currentKaif > 0 && net.sorukoma.quasium.magic.PlayerZones.tier(player) == 0) {
                        PlayerKaifData.subtractKaif(player, 1);
                    }
                    player.getPersistentData().putLong(LAST_KAIF_DECREASE_TICK_KEY, currentTick);
                }

                // Normal: negative kaif slowly recovers toward 0
                long lastRecoveryTick = player.getPersistentData().getLong(LAST_KAIF_RECOVERY_TICK_KEY);
                if (currentTick - lastRecoveryTick >= KAIF_RECOVERY_INTERVAL_TICKS) {
                    if (currentKaif < 0) {
                        PlayerKaifData.addKaif(player, 1);
                    }
                    player.getPersistentData().putLong(LAST_KAIF_RECOVERY_TICK_KEY, currentTick);
                }
            }

            // Negative kaif: periodic nausea (every 15 seconds)
            currentKaif = PlayerKaifData.getKaifValue(player);
            if (currentKaif < 0) {
                long lastNauseaTick = player.getPersistentData().getLong(LAST_NEGATIVE_KAIF_NAUSEA_TICK_KEY);
                if (currentTick - lastNauseaTick >= NEGATIVE_KAIF_NAUSEA_INTERVAL_TICKS) {
                    int nauseaAmplifier = Math.min(Math.abs(currentKaif) / 10, 4);
                    int durationTicks = 100 + Math.abs(currentKaif) * 5;
                    player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, durationTicks, nauseaAmplifier));
                    player.getPersistentData().putLong(LAST_NEGATIVE_KAIF_NAUSEA_TICK_KEY, currentTick);
                }
            }

            // Overdose check: track tobacco consumption and update overdose state
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                PlayerOverdoseData.tickCheck(serverPlayer);
                net.sorukoma.quasium.data.KaifAbilitiesHandler.tick(serverPlayer);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity().level().isClientSide()) return;

        Player player = event.getEntity();
        PlayerOverdoseData.onLogout(player.getUUID());

        // Сохраняем и выгружаем состояние наспинного кальяна, чтобы не терять прогресс
        // и не держать ссылку на разлогиненного игрока (утечка/протухший ServerPlayer).
        net.sorukoma.quasium.data.BackpackHookahState backpackState =
                net.sorukoma.quasium.data.BackpackHookahState.getState(player.getUUID());
        if (backpackState != null) {
            if (backpackState.isPuffing()) {
                backpackState.stopPuffing();
            }
            backpackState.saveToPlayer();
            net.sorukoma.quasium.data.BackpackHookahState.removeState(player.getUUID());
        }

        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        // Find all HookahConnectionEntities for this player and clean them up
        List<HookahConnectionEntity> connections = serverLevel.getEntitiesOfClass(
                HookahConnectionEntity.class,
                new AABB(player.blockPosition()).inflate(32.0),
                connection -> connection.getAttachedPlayerUUID() != null &&
                              connection.getAttachedPlayerUUID().equals(player.getUUID())
        );

        for (HookahConnectionEntity connection : connections) {
            if (connection.getHookahBlockPos() != null) {
                net.minecraft.world.level.block.entity.BlockEntity be = serverLevel.getBlockEntity(connection.getHookahBlockPos());
                if (be instanceof HookahBlockEntity hookahBE) {
                    connection.stopPuffing(hookahBE, player);
                }
            }
            connection.discard();
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity().level().isClientSide()) return;

        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            PlayerKaifData.syncOnLogin(serverPlayer);
            PlayerTobaccoData.syncOnLogin(serverPlayer);
            PlayerOreLuckData.syncOnLogin(serverPlayer);
        }
    }

    /**
     * Синхронизирует определения статей прогрессии (датапак) с клиентом. Срабатывает при
     * входе игрока и при /reload, поэтому покрывает оба случая. Без этого на выделенном
     * сервере книга прогрессии у игроков пустая — менеджер статей существует только
     * на серверной стороне.
     */
    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        event.getRelevantPlayers().forEach(BookRewards::sync);
        ProgressionArticleManager manager = QuasiumMod.getProgressionArticleManager();
        if (manager == null) return;

        List<ProgressionArticle> articles = new ArrayList<>(manager.getArticles().values());
        ProgressionArticlesSyncPacket packet = new ProgressionArticlesSyncPacket(articles);

        if (event.getPlayer() != null) {
            PacketDistributor.sendToPlayer(event.getPlayer(), packet);
        } else {
            for (net.minecraft.server.level.ServerPlayer player : event.getPlayerList().getPlayers()) {
                PacketDistributor.sendToPlayer(player, packet);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        PlayerOverdoseData.onRespawn(event.getEntity().getUUID());

        // Пере-синхронизируем сохранённые данные на клиент нового тела игрока
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            PlayerKaifData.setKaifValue(serverPlayer, PlayerKaifData.getKaifValue(serverPlayer));
            PlayerTobaccoData.syncOnLogin(serverPlayer);
            PlayerOreLuckData.syncOnLogin(serverPlayer);
        }
    }

    /**
     * Копирует наши per-player данные при клонировании игрока (смерть/смена измерения).
     * В NeoForge корневой getPersistentData() НЕ переносится автоматически на новое тело
     * при смерти, поэтому каждый kaif/табак/рюкзак-кальян терялся бы. Копируем все
     * ключи с префиксом "Quasium".
     */
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        net.minecraft.nbt.CompoundTag oldData = event.getOriginal().getPersistentData();
        net.minecraft.nbt.CompoundTag newData = event.getEntity().getPersistentData();
        for (String key : oldData.getAllKeys()) {
            if (key.startsWith("Quasium")) {
                newData.put(key, oldData.get(key).copy());
            }
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        new KaifCommand(event.getDispatcher());
        FlavorPreferenceCommand.register(event.getDispatcher());
        GolemCommand.register(event.getDispatcher());
    }

}
