package net.sorukoma.quasium.api;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public class TobaccoConsumptionEvent extends Event implements ICancellableEvent {
    private final ServerPlayer player;
    private int units;

    public TobaccoConsumptionEvent(ServerPlayer player, int units) {
        this.player = player;
        this.units = units;
    }

    public ServerPlayer player() {
        return player;
    }

    public int units() {
        return units;
    }

    public void setUnits(int units) {
        this.units = Math.max(0, units);
    }
}
