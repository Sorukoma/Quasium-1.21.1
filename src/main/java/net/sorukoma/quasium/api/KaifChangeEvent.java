package net.sorukoma.quasium.api;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public class KaifChangeEvent extends Event implements ICancellableEvent {
    private final Player player;
    private final int before;
    private int delta;

    public KaifChangeEvent(Player player, int before, int delta) {
        this.player = player;
        this.before = before;
        this.delta = delta;
    }

    public Player player() {
        return player;
    }

    public int before() {
        return before;
    }

    public int delta() {
        return delta;
    }

    public void setDelta(int delta) {
        this.delta = delta;
    }
}
