package net.sorukoma.quasium.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.sorukoma.quasium.mind.Insight;
import net.sorukoma.quasium.network.ScanDataSyncPacket;

import java.util.LinkedHashSet;
import java.util.Set;

public final class PlayerMagicData {
    private static final String KEY = "QuasiumMagic";
    private static final String SCANNED = "Scanned";
    private static final String INSIGHT = "Insight";

    private PlayerMagicData() {
    }

    private static CompoundTag root(Player player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(KEY, Tag.TAG_COMPOUND)) data.put(KEY, new CompoundTag());
        return data.getCompound(KEY);
    }

    public static Set<String> getScanned(Player player) {
        Set<String> scanned = new LinkedHashSet<>();
        ListTag list = root(player).getList(SCANNED, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) scanned.add(list.getString(i));
        return scanned;
    }

    public static boolean isScanned(Player player, String key) {
        return getScanned(player).contains(key);
    }

    public static boolean addScanned(ServerPlayer player, String key) {
        ListTag list = root(player).getList(SCANNED, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            if (list.getString(i).equals(key)) return false;
        }
        list.add(StringTag.valueOf(key));
        root(player).put(SCANNED, list);
        sync(player);
        return true;
    }

    public static int getInsight(Player player) {
        return root(player).getInt(INSIGHT);
    }

    public static int addInsight(ServerPlayer player, int amount) {
        int before = getInsight(player);
        int after = Math.max(0, Math.min(Insight.cap(player), before + amount));
        root(player).putInt(INSIGHT, after);
        sync(player);
        return after - before;
    }

    public static void reset(ServerPlayer player) {
        player.getPersistentData().remove(KEY);
        sync(player);
    }

    public static void sync(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new ScanDataSyncPacket(getScanned(player), getInsight(player)));
    }
}
