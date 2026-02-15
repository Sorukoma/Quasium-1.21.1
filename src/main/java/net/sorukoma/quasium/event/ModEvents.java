package net.sorukoma.quasium.event;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer; // Добавлен импорт для ServerPlayer
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent; // Добавлен импорт RegisterCommandsEvent
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.network.PacketDistributor; // Добавлен импорт PacketDistributor
import net.sorukoma.quasium.Config;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.command.KaifCommand; // Добавлен импорт KaifCommand
import net.sorukoma.quasium.data.PlayerKaifData;
import net.sorukoma.quasium.item.KaifometerItem;
import net.sorukoma.quasium.item.ModItems;
import net.sorukoma.quasium.network.KaifUpdatePacket; // Добавлен импорт KaifUpdatePacket

@EventBusSubscriber(modid = QuasiumMod.MOD_ID)
public class ModEvents {
    private static final String LAST_KAIF_DECREASE_TICK_KEY = "QuasiumLastKaifDecreaseTick";
    private static final int KAIF_DECREASE_INTERVAL_TICKS = 20 * 10; // 10 секунд = 200 тиков

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        // QuasiumMod.LOGGER.info("LivingIncomingDamageEvent fired for entity: " + event.getEntity().getName().getString()); // Удалено лог-сообщение
        if (event.getEntity() instanceof Player player) {
            if (hasFullQuasiumArmor(player)) {
                if (Math.random() < Config.QUASIUM_ARMOR_DODGE_CHANCE.get()) {
                    player.sendSystemMessage(Component.literal("Уклонение!"));

                    // Повреждение брони
                    for (ItemStack armorStack : player.getInventory().armor) {
                        if (!armorStack.isEmpty() && armorStack.getItem() instanceof ArmorItem) {
                            armorStack.hurtAndBreak(1, player, armorStack.getEquipmentSlot());
                        }
                    }
                    event.setAmount(0f); // Отмена урона

                    // Увеличение кайфа при уклонении
                    PlayerKaifData.addKaif(player, 5);
                    player.sendSystemMessage(Component.literal("Кайф увеличен на 5! Текущий кайф: " + PlayerKaifData.getKaifValue(player)));

                    // Отправляем пакет клиенту для обновления GUI
                    if (player instanceof ServerPlayer serverPlayer) {
                        PacketDistributor.sendToPlayer(serverPlayer, new KaifUpdatePacket(PlayerKaifData.getKaifValue(player)));
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLivingTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide) {
            long currentTick = player.level().getGameTime();
            long lastDecreaseTick = player.getPersistentData().getLong(LAST_KAIF_DECREASE_TICK_KEY);

            if (currentTick - lastDecreaseTick >= KAIF_DECREASE_INTERVAL_TICKS) {
                int currentKaif = PlayerKaifData.getKaifValue(player);
                if (currentKaif > 0) {
                    PlayerKaifData.subtractKaif(player, 1);
                    player.getPersistentData().putLong(LAST_KAIF_DECREASE_TICK_KEY, currentTick);

                    // Отправляем пакет клиенту для обновления GUI
                    if (player instanceof ServerPlayer serverPlayer) {
                        PacketDistributor.sendToPlayer(serverPlayer, new KaifUpdatePacket(PlayerKaifData.getKaifValue(player)));
                    }
                } else {
                    player.getPersistentData().putLong(LAST_KAIF_DECREASE_TICK_KEY, currentTick);
                }
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

    private static ItemStack findKaifometer(Player player) {
        // Проверяем основную руку
        if (player.getMainHandItem().getItem() == ModItems.KAIFOMETER.get()) {
            return player.getMainHandItem();
        }
        // Проверяем вторую руку
        if (player.getOffhandItem().getItem() == ModItems.KAIFOMETER.get()) {
            return player.getOffhandItem();
        }
        // Проверяем весь инвентарь
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() == ModItems.KAIFOMETER.get()) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }
}
