package net.sorukoma.quasium.mind;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.LinkedHashSet;
import java.util.Set;

public final class MindData {
    public static final String KEY = "QuasiumMind";
    public static final String RESEARCH = "Research";
    public static final String SPELLS = "Spells";
    public static final String PERKS = "Perks";
    public static final String FLAGS = "Flags";
    public static final String TITLES = "Titles";
    public static final String DREAMS = "Dreams";
    public static final String SCHOOL = "School";

    private MindData() {
    }

    public static CompoundTag root(Player player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(KEY, Tag.TAG_COMPOUND)) data.put(KEY, new CompoundTag());
        return data.getCompound(KEY);
    }

    public static Set<String> strings(CompoundTag tag, String key) {
        Set<String> values = new LinkedHashSet<>();
        ListTag list = tag.getList(key, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) values.add(list.getString(i));
        return values;
    }

    public static Set<String> strings(Player player, String key) {
        return strings(root(player), key);
    }

    public static boolean has(Player player, String key, String value) {
        return strings(player, key).contains(value);
    }

    public static boolean add(Player player, String key, String value) {
        CompoundTag root = root(player);
        ListTag list = root.getList(key, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            if (list.getString(i).equals(value)) return false;
        }
        list.add(StringTag.valueOf(value));
        root.put(key, list);
        changed(player);
        return true;
    }

    public static long time(Player player, String key) {
        return root(player).getLong(key);
    }

    public static void setTime(Player player, String key, long value) {
        root(player).putLong(key, value);
        changed(player);
    }

    public static boolean flag(Player player, String flag) {
        return has(player, FLAGS, flag);
    }

    public static boolean raise(Player player, String flag) {
        return add(player, FLAGS, flag);
    }

    public static void changed(Player player) {
        if (player instanceof ServerPlayer serverPlayer) MindSync.markDirty(serverPlayer);
    }
}
