package net.sorukoma.quasium.api;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;

import java.util.List;

public class RitualCompleteEvent extends Event {
    private final ServerLevel level;
    private final BlockPos center;
    private final ResourceLocation ritualId;
    private final List<ServerPlayer> participants;

    public RitualCompleteEvent(ServerLevel level, BlockPos center, ResourceLocation ritualId, List<ServerPlayer> participants) {
        this.level = level;
        this.center = center;
        this.ritualId = ritualId;
        this.participants = List.copyOf(participants);
    }

    public ServerLevel level() {
        return level;
    }

    public BlockPos center() {
        return center;
    }

    public ResourceLocation ritualId() {
        return ritualId;
    }

    public List<ServerPlayer> participants() {
        return participants;
    }
}
