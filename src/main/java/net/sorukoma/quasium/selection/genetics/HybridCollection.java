package net.sorukoma.quasium.selection.genetics;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.network.PacketDistributor;
import net.sorukoma.quasium.advancements.triggers.SmokecraftTrigger;
import net.sorukoma.quasium.item.TobaccoVarieties;
import net.sorukoma.quasium.mind.MindRegistry;
import net.sorukoma.quasium.mind.MindStats;
import net.sorukoma.quasium.selection.SelectionRegistry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public final class HybridCollection {
    public static final String FIRST_HYBRID_EVENT = "hybrid_first";
    public static final String WILD_PAIRS_EVENT = "hybrid_wild_pairs";

    private HybridCollection() {
    }

    public static Set<String> of(Player player) {
        return Set.copyOf(player.getData(SelectionRegistry.KNOWN_HYBRIDS));
    }

    public static boolean discover(ServerPlayer player, List<String> lineage) {
        if (!Lineage.isHybrid(lineage)) return false;
        String key = Lineage.key(lineage);
        WorldHybrids.of(player.serverLevel()).add(key);
        TreeSet<String> known = new TreeSet<>(player.getData(SelectionRegistry.KNOWN_HYBRIDS));
        if (!known.add(key)) return false;
        player.setData(SelectionRegistry.KNOWN_HYBRIDS, List.copyOf(known));
        sync(player);
        MindStats.award(player, MindRegistry.HYBRIDS_FOUND, 1);
        SmokecraftTrigger.fire(player, FIRST_HYBRID_EVENT);
        if (knowsAllWildPairs(known)) SmokecraftTrigger.fire(player, WILD_PAIRS_EVENT);
        return true;
    }

    public static Set<String> world(ServerLevel level) {
        return Set.copyOf(WorldHybrids.of(level).keys);
    }

    public static void sync(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new HybridCollectionPayload(player.getData(SelectionRegistry.KNOWN_HYBRIDS)));
    }

    public static boolean knowsAllWildPairs(Collection<String> known) {
        List<String> pairs = wildPairs();
        return !pairs.isEmpty() && known.containsAll(pairs);
    }

    public static List<String> wildPairs() {
        List<String> wild = TobaccoVarieties.wild();
        List<String> pairs = new ArrayList<>();
        for (int i = 0; i < wild.size(); i++) {
            for (int j = i + 1; j < wild.size(); j++) pairs.add(Lineage.key(List.of(wild.get(i), wild.get(j))));
        }
        return pairs;
    }

    private static final class WorldHybrids extends SavedData {
        private final Set<String> keys = new TreeSet<>();

        static WorldHybrids of(ServerLevel level) {
            return level.getServer().overworld().getDataStorage()
                    .computeIfAbsent(new Factory<>(WorldHybrids::new, WorldHybrids::load), "quasium_hybrids");
        }

        private static WorldHybrids load(CompoundTag tag, HolderLookup.Provider registries) {
            WorldHybrids data = new WorldHybrids();
            ListTag list = tag.getList("keys", Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) data.keys.add(list.getString(i));
            return data;
        }

        void add(String key) {
            if (keys.add(key)) setDirty();
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            ListTag list = new ListTag();
            keys.forEach(key -> list.add(StringTag.valueOf(key)));
            tag.put("keys", list);
            return tag;
        }
    }
}
