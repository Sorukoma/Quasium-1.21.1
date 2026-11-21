package net.sorukoma.quasium.mind;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashSet;
import java.util.Set;

public final class ClientMind {
    private static CompoundTag data = new CompoundTag();
    private static int version;

    private ClientMind() {
    }

    public static void set(CompoundTag snapshot) {
        data = snapshot;
        version++;
    }

    public static void reset() {
        set(new CompoundTag());
    }

    public static int version() {
        return version;
    }

    public static CompoundTag data() {
        return data;
    }

    public static Set<String> strings(String key) {
        return MindData.strings(data, key);
    }

    public static Set<ResourceLocation> knownSpells() {
        Set<ResourceLocation> spells = new LinkedHashSet<>();
        for (String id : strings(MindData.SPELLS)) {
            ResourceLocation spell = ResourceLocation.tryParse(id);
            if (spell != null) spells.add(spell);
        }
        return spells;
    }

    public static boolean researched(ResourceLocation node) {
        return strings(MindData.RESEARCH).contains(node.toString());
    }

    public static boolean perk(String perk) {
        return strings(MindData.PERKS).contains(perk);
    }

    public static boolean flag(String flag) {
        return strings(MindData.FLAGS).contains(flag);
    }

    public static String school() {
        return data.getString(MindData.SCHOOL);
    }

    public static int section(String section, String key) {
        return data.getCompound(section).getInt(key);
    }
}
