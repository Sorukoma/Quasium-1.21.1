package net.sorukoma.quasium.api;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;

public class KaifComaEvent extends Event {
    private final ServerPlayer player;
    private final int durationTicks;

    public KaifComaEvent(ServerPlayer player, int durationTicks) {
        this.player = player;
        this.durationTicks = durationTicks;
    }

    public ServerPlayer player() {
        return player;
    }

    public int durationTicks() {
        return durationTicks;
    }
}
