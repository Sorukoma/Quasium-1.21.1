package net.sorukoma.quasium.data;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.sorukoma.quasium.QuasiumMod; // Импорт для LOGGER
import net.sorukoma.quasium.advancements.triggers.QuasiumModAdvancements; // Добавлен импорт QuasiumModAdvancements

public class PlayerKaifData {
    public static final String PLAYER_KAIF_KEY = "QuasiumPlayerKaif";
    public static final int MAX_KAIF = 100;
    private static final int KAIF_ADVANCEMENT_THRESHOLD = 30; // Этот порог теперь будет использоваться в триггере достижения
    private static final ResourceLocation KAIF_ADVANCEMENT_ID = ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "kaif/kaif_level_30");

    public static int getKaifValue(Player player) {
        CompoundTag persistentData = player.getPersistentData();
        return persistentData.getInt(PLAYER_KAIF_KEY);
    }

    public static void setKaifValue(Player player, int value) {
        int oldKaif = getKaifValue(player);
        int newKaif = Math.max(0, Math.min(MAX_KAIF, value));
        
        CompoundTag persistentData = player.getPersistentData();
        persistentData.putInt(PLAYER_KAIF_KEY, newKaif);

        // Проверяем, если игрок на сервере
        if (player instanceof ServerPlayer serverPlayer) {
            // Вызываем наш новый триггер достижения
            QuasiumModAdvancements.KAIF_LEVEL.get().trigger(serverPlayer, newKaif);

            // Удаляем старую логику выдачи достижения, так как теперь она обрабатывается через триггер
            // if (newKaif >= KAIF_ADVANCEMENT_THRESHOLD && oldKaif < KAIF_ADVANCEMENT_THRESHOLD) {
            //     grantKaifAdvancement(serverPlayer);
            // }
        }
    }

    public static void addKaif(Player player, int amount) {
        int currentKaif = getKaifValue(player);
        setKaifValue(player, currentKaif + amount);
    }

    public static void subtractKaif(Player player, int amount) {
        int currentKaif = getKaifValue(player);
        setKaifValue(player, currentKaif - amount);
    }

    // Этот метод больше не нужен, так как достижение выдается через триггер
    // private static void grantKaifAdvancement(ServerPlayer player) {
    //     AdvancementHolder advancement = player.server.getAdvancements().get(KAIF_ADVANCEMENT_ID);

    //     if (advancement != null) {
    //         AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
    //         if (!progress.isDone()) {
    //             for (String criterion : progress.getRemainingCriteria()) {
    //                 player.getAdvancements().award(advancement, criterion);
    //             }
    //             QuasiumMod.LOGGER.info("Advancement {} granted to player {}", KAIF_ADVANCEMENT_ID, player.getName().getString());
    //         }
    //     } else {
    //         QuasiumMod.LOGGER.warn("Advancement {} not found for granting to player {}", KAIF_ADVANCEMENT_ID, player.getName().getString());
    //     }
    // }
}
