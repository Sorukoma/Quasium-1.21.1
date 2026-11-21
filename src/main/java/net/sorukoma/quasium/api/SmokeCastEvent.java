package net.sorukoma.quasium.api;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import net.sorukoma.quasium.magic.SmokeSource;
import net.sorukoma.quasium.magic.Spell;
import org.jetbrains.annotations.Nullable;

public class SmokeCastEvent extends Event implements ICancellableEvent {
    private final ServerPlayer player;
    private final ResourceLocation spellId;
    private final Spell spell;
    private final SmokeSource source;
    private final boolean fromFlask;
    private int tobaccoUnits;
    private int essenceCost;
    private int kaifCost;
    @Nullable
    private Component refusal;

    public SmokeCastEvent(ServerPlayer player, ResourceLocation spellId, Spell spell, SmokeSource source, boolean fromFlask,
                          int tobaccoUnits, int essenceCost) {
        this.player = player;
        this.spellId = spellId;
        this.spell = spell;
        this.source = source;
        this.fromFlask = fromFlask;
        this.tobaccoUnits = tobaccoUnits;
        this.essenceCost = essenceCost;
    }

    public ServerPlayer player() {
        return player;
    }

    public ResourceLocation spellId() {
        return spellId;
    }

    public Spell spell() {
        return spell;
    }

    public SmokeSource source() {
        return source;
    }

    public boolean fromFlask() {
        return fromFlask;
    }

    public int tobaccoUnits() {
        return tobaccoUnits;
    }

    public void setTobaccoUnits(int tobaccoUnits) {
        this.tobaccoUnits = Math.max(0, tobaccoUnits);
    }

    public int essenceCost() {
        return essenceCost;
    }

    public void setEssenceCost(int essenceCost) {
        this.essenceCost = Math.max(0, essenceCost);
    }

    public int kaifCost() {
        return kaifCost;
    }

    public void setKaifCost(int kaifCost) {
        this.kaifCost = Math.max(0, kaifCost);
    }

    @Nullable
    public Component refusal() {
        return refusal;
    }

    public void refuse(Component reason) {
        refusal = reason;
        setCanceled(true);
    }
}
