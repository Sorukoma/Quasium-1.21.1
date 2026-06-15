package net.sorukoma.quasium.data;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.network.TobaccoDataSyncPacket;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class PlayerTobaccoData {
    private static final String TRIED_FLAVORS_KEY = "QuasiumTriedFlavors";
    private static final String TRIED_SELF_MIXED_STRENGTHS_KEY = "QuasiumTriedSelfMixedStrengths";

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

    private static void syncToClient(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new TobaccoDataSyncPacket(
                    getTriedFlavors(player), getTriedSelfMixedStrengths(player)
            ));
        }
    }

    public static void syncOnLogin(ServerPlayer serverPlayer) {
        Set<String> flavors = getTriedFlavors(serverPlayer);
        Set<Float> strengths = getTriedSelfMixedStrengths(serverPlayer);
        if (!flavors.isEmpty() || !strengths.isEmpty()) {
            PacketDistributor.sendToPlayer(serverPlayer, new TobaccoDataSyncPacket(flavors, strengths));
        }
    }
}