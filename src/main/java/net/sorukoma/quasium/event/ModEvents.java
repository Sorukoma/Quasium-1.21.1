package net.sorukoma.quasium.event;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades.ItemListing;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.BasicItemListing;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.minecraft.world.phys.AABB;
import net.sorukoma.quasium.Config;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.block.ModBlocks;
import net.sorukoma.quasium.block.entity.HookahBlockEntity;
import net.sorukoma.quasium.command.KaifCommand;
import net.sorukoma.quasium.data.PlayerKaifData;
import net.sorukoma.quasium.data.PlayerOreLuckData;
import net.sorukoma.quasium.data.PlayerOverdoseData;
import net.sorukoma.quasium.data.PlayerTobaccoData;
import net.sorukoma.quasium.entity.HookahConnectionEntity;
import net.sorukoma.quasium.item.ModItems;
import net.sorukoma.quasium.item.ProbabilisticItemListing;
import net.sorukoma.quasium.network.ModPackets;
import net.sorukoma.quasium.network.PacketStopHookahPuffSoundClient;
import net.sorukoma.quasium.network.PacketSyncHookahConnection;

import java.util.List;
import java.util.Optional;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID)
public class ModEvents {
    private static final String LAST_KAIF_DECREASE_TICK_KEY = "QuasiumLastKaifDecreaseTick";
    private static final String LAST_KAIF_RECOVERY_TICK_KEY = "QuasiumLastKaifRecoveryTick";
    private static final String LAST_NEGATIVE_KAIF_NAUSEA_TICK_KEY = "QuasiumLastNegativeKaifNauseaTick";
    private static final int KAIF_DECREASE_INTERVAL_TICKS = 20 * 10;  // every 10 seconds
    private static final int KAIF_RECOVERY_INTERVAL_TICKS = 20 * 30;  // every 30 seconds (3x slower than decrease)
    private static final int KAIF_OVERDOSE_DECAY_INTERVAL_TICKS = 20 * 5;  // every 5 seconds during overdose
    private static final int NEGATIVE_KAIF_NAUSEA_INTERVAL_TICKS = 20 * 15;  // every 15 seconds

    // ─── Вероятности появления торгов у жителей (меняйте для тестирования) ───

    /** Вероятность появления блока кальяна у священника (5 уровень, мастер). */
    private static final float HOOKAH_BLOCK_TRADE_PROBABILITY = 0.3F;
    /** Вероятность появления обычных табаков у священника (3 уровень). */
    private static final float REGULAR_TOBACCO_TRADE_PROBABILITY = 0.5F;
    /** Вероятность появления трубки кальяна у инструментальщика (2 уровень). */
    private static final float HOOKAH_PIPE_TRADE_PROBABILITY = 0.5F;

    // ─── Торги жителей ───

    @SubscribeEvent
    public static void onVillagerTrades(VillagerTradesEvent event) {
        if (event.getType() == VillagerProfession.CLERIC) {
            // 5 уровень (мастер) — блок кальяна
            List<ItemListing> level5 = event.getTrades().get(5);
            level5.add(new ProbabilisticItemListing(
                    new BasicItemListing(64, new ItemStack(ModBlocks.HOOKAH_BLOCK.get()), 1, 30, 0.05F),
                    HOOKAH_BLOCK_TRADE_PROBABILITY
            ));

            // 3 уровень (journeyman) — обычные табаки (mint, citrus, berry), 15 эм. за 2 шт
            List<ItemListing> level3 = event.getTrades().get(3);
            level3.add(new ProbabilisticItemListing(
                    new BasicItemListing(15, new ItemStack(ModItems.MINT_TOBACCO.get(), 2), 2, 10, 0.05F),
                    REGULAR_TOBACCO_TRADE_PROBABILITY
            ));
            level3.add(new ProbabilisticItemListing(
                    new BasicItemListing(15, new ItemStack(ModItems.CITRUS_TOBACCO.get(), 2), 2, 10, 0.05F),
                    REGULAR_TOBACCO_TRADE_PROBABILITY
            ));
            level3.add(new ProbabilisticItemListing(
                    new BasicItemListing(15, new ItemStack(ModItems.BERRY_TOBACCO.get(), 2), 2, 10, 0.05F),
                    REGULAR_TOBACCO_TRADE_PROBABILITY
            ));
        }

        if (event.getType() == VillagerProfession.TOOLSMITH) {
            // 2 уровень (apprentice) — трубка кальяна
            List<ItemListing> level2 = event.getTrades().get(2);
            level2.add(new ProbabilisticItemListing(
                    new BasicItemListing(10, new ItemStack(ModItems.HOOKAH_PIPE.get()), 2, 10, 0.05F),
                    HOOKAH_PIPE_TRADE_PROBABILITY
            ));
        }
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide()) return;

        if (event.getEntity() instanceof Player player) {
            if (hasFullQuasiumArmor(player)) {
                if (player.level().random.nextFloat() < Config.QUASIUM_ARMOR_DODGE_CHANCE.get()) {
                    player.sendSystemMessage(Component.literal("Уклонение!"));

                    for (ItemStack armorStack : player.getInventory().armor) {
                        if (!armorStack.isEmpty() && armorStack.getItem() instanceof ArmorItem) {
                            armorStack.hurtAndBreak(1, player, armorStack.getEquipmentSlot());
                        }
                    }
                    event.setAmount(0f);

                    PlayerKaifData.addKaif(player, 5);
                    player.sendSystemMessage(Component.literal("Кайф увеличен на 5! Текущий кайф: " + PlayerKaifData.getKaifValue(player)));
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLivingTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide) {
            long currentTick = player.level().getGameTime();
            int currentKaif = PlayerKaifData.getKaifValue(player);
            boolean isOverdosed = PlayerOverdoseData.isOverdosed(player);

            // Overdose: kaif drops every 5 seconds regardless of sign (even below 0)
            if (isOverdosed) {
                long lastOverdoseDecayTick = player.getPersistentData().getLong("QuasiumLastOverdoseDecayTick");
                if (currentTick - lastOverdoseDecayTick >= KAIF_OVERDOSE_DECAY_INTERVAL_TICKS) {
                    PlayerKaifData.subtractKaif(player, 2);
                    player.getPersistentData().putLong("QuasiumLastOverdoseDecayTick", currentTick);
                }
            } else {
                // Normal: positive kaif decreases over time
                long lastDecreaseTick = player.getPersistentData().getLong(LAST_KAIF_DECREASE_TICK_KEY);
                if (currentTick - lastDecreaseTick >= KAIF_DECREASE_INTERVAL_TICKS) {
                    if (currentKaif > 0) {
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
            PlayerTobaccoData.syncOnLogin(serverPlayer);
            PlayerOreLuckData.syncOnLogin(serverPlayer);
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
    }

    private static boolean hasFullQuasiumArmor(Player player) {
        ItemStack helmet = player.getInventory().getArmor(3);
        ItemStack chestplate = player.getInventory().getArmor(2);
        ItemStack leggings = player.getInventory().getArmor(1);
        ItemStack boots = player.getInventory().getArmor(0);

        return !helmet.isEmpty() && !chestplate.isEmpty() && !leggings.isEmpty() && !boots.isEmpty()
                && helmet.getItem() == ModItems.QUASIUM_HELMET.get()
                && chestplate.getItem() == ModItems.QUASIUM_CHESTPLATE.get()
                && leggings.getItem() == ModItems.QUASIUM_LEGGINGS.get()
                && boots.getItem() == ModItems.QUASIUM_BOOTS.get();
    }
}