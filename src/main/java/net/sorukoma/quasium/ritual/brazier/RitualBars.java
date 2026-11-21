package net.sorukoma.quasium.ritual.brazier;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.sorukoma.quasium.ritual.RitualConfig;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class RitualBars {
    private final Component name;
    private final ServerBossEvent progress;
    private final ServerBossEvent instability;

    RitualBars(ResourceLocation ritual) {
        name = RitualRun.displayName(ritual);
        progress = new ServerBossEvent(name, BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);
        instability = new ServerBossEvent(Component.empty(), BossEvent.BossBarColor.GREEN, BossEvent.BossBarOverlay.NOTCHED_10);
    }

    void update(List<ServerPlayer> participants, float done, float unstable, boolean group, boolean starved) {
        int threshold = RitualConfig.RITUAL_BREAK_THRESHOLD.getAsInt();
        float share = Math.min(1f, unstable / threshold);
        progress.setProgress(Math.min(1f, done));
        String key = starved ? "quasium.ritual.bar.progress_starved" : group ? "quasium.ritual.bar.progress_group" : "quasium.ritual.bar.progress";
        progress.setName(Component.translatable(key, name, Math.round(done * 100)));
        instability.setProgress(share);
        instability.setColor(share < 0.5f ? BossEvent.BossBarColor.GREEN : share < 0.8f ? BossEvent.BossBarColor.YELLOW : BossEvent.BossBarColor.RED);
        instability.setName(Component.translatable("quasium.ritual.bar.instability", Math.round(unstable), threshold));
        follow(progress, participants);
        follow(instability, participants);
    }

    void hide() {
        progress.removeAllPlayers();
        instability.removeAllPlayers();
    }

    private static void follow(ServerBossEvent bar, List<ServerPlayer> participants) {
        Set<ServerPlayer> watching = new HashSet<>(bar.getPlayers());
        for (ServerPlayer player : participants) {
            if (!watching.remove(player)) bar.addPlayer(player);
        }
        watching.forEach(bar::removePlayer);
    }
}
