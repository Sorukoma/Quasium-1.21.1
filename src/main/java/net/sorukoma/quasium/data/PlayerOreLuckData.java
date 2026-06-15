package net.sorukoma.quasium.data;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.sorukoma.quasium.network.OreLuckDataSyncPacket;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class PlayerOreLuckData {
    private static final String ACHIEVED_FORTUNE_LEVELS_KEY = "QuasiumOreAchievedFortuneLevels";

    public static Set<Integer> getAchievedFortuneLevels(Player player) {
        String data = player.getPersistentData().getString(ACHIEVED_FORTUNE_LEVELS_KEY);
        if (data.isEmpty()) return new HashSet<>();
        return Arrays.stream(data.split(","))
                .map(Integer::parseInt)
                .collect(Collectors.toCollection(HashSet::new));
    }

    public static boolean addAchievedFortuneLevel(ServerPlayer serverPlayer, int fortuneLevel) {
        Set<Integer> levels = getAchievedFortuneLevels(serverPlayer);
        if (levels.contains(fortuneLevel)) return false;
        levels.add(fortuneLevel);
        serverPlayer.getPersistentData().putString(ACHIEVED_FORTUNE_LEVELS_KEY,
                levels.stream().map(String::valueOf).collect(Collectors.joining(",")));
        syncToClient(serverPlayer);
        return true;
    }

    private static void syncToClient(ServerPlayer serverPlayer) {
        PacketDistributor.sendToPlayer(serverPlayer, new OreLuckDataSyncPacket(getAchievedFortuneLevels(serverPlayer)));
    }

    public static void syncOnLogin(ServerPlayer serverPlayer) {
        Set<Integer> levels = getAchievedFortuneLevels(serverPlayer);
        PacketDistributor.sendToPlayer(serverPlayer, new OreLuckDataSyncPacket(levels));
    }
}