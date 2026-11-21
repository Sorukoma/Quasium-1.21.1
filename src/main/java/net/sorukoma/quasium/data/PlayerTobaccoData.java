package net.sorukoma.quasium.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.sorukoma.quasium.mind.MindConfig;
import net.sorukoma.quasium.mind.MindData;
import net.sorukoma.quasium.network.TobaccoDataSyncPacket;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class PlayerTobaccoData {
    private static final String TRIED_FLAVORS_KEY = "QuasiumTriedFlavors";
    private static final String TRIED_SELF_MIXED_STRENGTHS_KEY = "QuasiumTriedSelfMixedStrengths";
    private static final String FLAVOR_PREFERENCES_KEY = "QuasiumTriedFlavorPreferences";
    private static final String TOLERANCE_KEY = "QuasiumTolerance";
    private static final String SESSION_GRAMS_KEY = "QuasiumToleranceGrams";

    public static Map<String, Boolean> getFlavorPreferences(Player player) {
        Map<String, Boolean> result = new LinkedHashMap<>();
        String data = player.getPersistentData().getString(FLAVOR_PREFERENCES_KEY);
        if (!data.isEmpty()) {
            for (String entry : data.split(",")) {
                String[] parts = entry.split("=", 2);
                if (parts.length == 2) result.put(parts[0], Boolean.parseBoolean(parts[1]));
            }
        }
        return result;
    }

    public static void addFirstFlavorResult(Player player, String flavor) {
        if (flavor == null || flavor.isBlank()) return;
        Map<String, Boolean> results = getFlavorPreferences(player);
        for (String part : flavor.split("\\+")) {
            if (part.isBlank() || results.containsKey(part)) continue;
            results.put(part, PlayerFlavorPreferenceData.liked(player, part));
        }
        if (!results.isEmpty()) {
            player.getPersistentData().putString(FLAVOR_PREFERENCES_KEY, results.entrySet().stream()
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .collect(Collectors.joining(",")));
            syncToClient(player);
        }
    }

    public static Set<String> getTriedFlavors(Player player) {
        String data = player.getPersistentData().getString(TRIED_FLAVORS_KEY);
        if (data.isEmpty()) return new HashSet<>();
        return new HashSet<>(Arrays.asList(data.split(",")));
    }

    public static Set<Float> getTriedSelfMixedStrengths(Player player) {
        String data = player.getPersistentData().getString(TRIED_SELF_MIXED_STRENGTHS_KEY);
        if (data.isEmpty()) return new HashSet<>();
        return Arrays.stream(data.split(","))
                .map(s -> Math.round(Float.parseFloat(s) * 10.0f) / 10.0f)
                .collect(Collectors.toCollection(HashSet::new));
    }

    public static void addTriedFlavor(Player player, String flavor) {
        Set<String> flavors = getTriedFlavors(player);
        if (flavors.contains(flavor)) return;
        flavors.add(flavor);
        player.getPersistentData().putString(TRIED_FLAVORS_KEY, String.join(",", flavors));
        syncToClient(player);
    }

    public static void addTriedFlavors(Player player, Set<String> newFlavors) {
        Set<String> flavors = getTriedFlavors(player);
        boolean changed = false;
        for (String flavor : newFlavors) {
            if (!flavors.contains(flavor)) {
                flavors.add(flavor);
                changed = true;
            }
        }
        if (changed) {
            player.getPersistentData().putString(TRIED_FLAVORS_KEY, String.join(",", flavors));
            syncToClient(player);
        }
    }

    public static void addTriedSelfMixedStrength(Player player, float strength) {
        float rounded = Math.round(strength * 10.0f) / 10.0f;
        Set<Float> strengths = getTriedSelfMixedStrengths(player);
        if (strengths.contains(rounded)) return;
        strengths.add(rounded);
        player.getPersistentData().putString(TRIED_SELF_MIXED_STRENGTHS_KEY,
                strengths.stream().map(f -> String.format("%.1f", f)).collect(Collectors.joining(",")));
        syncToClient(player);
    }

    public static int tolerance(Player player, String flavor) {
        return player.getPersistentData().getCompound(TOLERANCE_KEY).getInt(flavor);
    }

    public static CompoundTag toleranceTag(Player player) {
        return player.getPersistentData().getCompound(TOLERANCE_KEY).copy();
    }

    public static void addTolerance(Player player, String flavor, int amount) {
        CompoundTag tolerances = player.getPersistentData().getCompound(TOLERANCE_KEY);
        int value = Math.max(0, Math.min(MindConfig.TOLERANCE_CAP.getAsInt(), tolerances.getInt(flavor) + amount));
        if (value == 0) tolerances.remove(flavor);
        else tolerances.putInt(flavor, value);
        player.getPersistentData().put(TOLERANCE_KEY, tolerances);
        MindData.changed(player);
    }

    public static int addSessionGrams(Player player, String flavor, float grams) {
        CompoundTag sessions = player.getPersistentData().getCompound(SESSION_GRAMS_KEY);
        float total = sessions.getFloat(flavor) + grams;
        int sessionGrams = MindConfig.TOLERANCE_SESSION_GRAMS.getAsInt();
        int done = (int) (total / sessionGrams);
        sessions.putFloat(flavor, total - done * sessionGrams);
        player.getPersistentData().put(SESSION_GRAMS_KEY, sessions);
        if (done > 0) addTolerance(player, flavor, done * MindConfig.TOLERANCE_PER_SESSION.getAsInt());
        return done;
    }

    private static void syncToClient(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new TobaccoDataSyncPacket(
                    getTriedFlavors(player), getTriedSelfMixedStrengths(player), getFlavorPreferences(player)
            ));
        }
    }

    public static void syncOnLogin(ServerPlayer serverPlayer) {
        Set<String> flavors = getTriedFlavors(serverPlayer);
        Set<Float> strengths = getTriedSelfMixedStrengths(serverPlayer);
        if (!flavors.isEmpty() || !strengths.isEmpty() || !getFlavorPreferences(serverPlayer).isEmpty()) {
            PacketDistributor.sendToPlayer(serverPlayer, new TobaccoDataSyncPacket(
                    flavors, strengths, getFlavorPreferences(serverPlayer)
            ));
        }
    }
}