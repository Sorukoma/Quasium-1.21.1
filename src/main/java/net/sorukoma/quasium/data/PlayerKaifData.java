package net.sorukoma.quasium.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.advancements.triggers.QuasiumModAdvancements;
import net.sorukoma.quasium.network.KaifUpdatePacket;

public class PlayerKaifData {
    public static final String PLAYER_KAIF_KEY = "QuasiumPlayerKaif";
    public static final int MAX_KAIF = 100;
    public static final int MIN_KAIF = -100;
    private static final int KAIF_ADVANCEMENT_THRESHOLD = 30;
    private static final net.minecraft.resources.ResourceLocation KAIF_ADVANCEMENT_ID = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "kaif/kaif_level_30");

    public static int getKaifValue(Player player) {
        CompoundTag persistentData = player.getPersistentData();
        return persistentData.getInt(PLAYER_KAIF_KEY);
    }

    public static void setKaifValue(Player player, int value) {
        int newKaif = Math.max(MIN_KAIF, Math.min(MAX_KAIF, value));

        CompoundTag persistentData = player.getPersistentData();
        persistentData.putInt(PLAYER_KAIF_KEY, newKaif);

        if (player instanceof ServerPlayer serverPlayer) {
            if (newKaif >= 0) {
                QuasiumModAdvancements.KAIF_LEVEL.get().trigger(serverPlayer, newKaif);
            }
            PacketDistributor.sendToPlayer(serverPlayer, new KaifUpdatePacket(newKaif));
        }
    }

    /**
     * Отправляет текущее значение kaif клиенту. Нужно при входе игрока: kaif хранится в NBT
     * на сервере, а ClientKaifData стартует с 0, поэтому без явной синхронизации вернувшийся
     * игрок видел бы 0 (HUD кайфометра) до первого изменения kaif.
     */
    public static void syncOnLogin(ServerPlayer serverPlayer) {
        PacketDistributor.sendToPlayer(serverPlayer, new KaifUpdatePacket(getKaifValue(serverPlayer)));
    }

    public static void addKaif(Player player, int amount) {
        change(player, net.sorukoma.quasium.magic.PlayerZones.boostKaif(player, amount));
    }

    public static void subtractKaif(Player player, int amount) {
        change(player, -amount);
    }

    private static void change(Player player, int delta) {
        int before = getKaifValue(player);
        net.sorukoma.quasium.api.KaifChangeEvent event = net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(
                new net.sorukoma.quasium.api.KaifChangeEvent(player, before, delta));
        if (!event.isCanceled()) setKaifValue(player, before + event.delta());
    }
}
