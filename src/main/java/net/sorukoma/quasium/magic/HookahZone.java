package net.sorukoma.quasium.magic;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.network.PacketDistributor;
import net.sorukoma.quasium.Config;
import net.sorukoma.quasium.network.ZoneRingPacket;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class HookahZone {
    private record Seat(BlockPos cushion, long tick) {
    }

    private static final int FURNITURE_RECHECK_TICKS = 100;
    private static final int SEAT_HOLD_TICKS = 40;

    private final Map<UUID, Long> lastPuff = new HashMap<>();
    private final Set<UUID> golems = new HashSet<>();
    private final Map<UUID, Seat> seats = new HashMap<>();
    private final long[] heldUntil = new long[PlayerZones.NIRVANA + 1];
    private long furnitureCheckAt;
    private int furniture;

    public void notePuff(UUID player, long now) {
        lastPuff.put(player, now);
    }

    public void noteGolemPuff(UUID golem, long now) {
        lastPuff.put(golem, now);
        golems.add(golem);
    }

    public int smokers(long now) {
        long memory = Config.ZONE_SMOKER_MEMORY_SEC.getAsInt() * 20L;
        lastPuff.values().removeIf(tick -> now - tick > memory);
        golems.retainAll(lastPuff.keySet());
        return lastPuff.size() - golems.size() + Math.min(golems.size(), Config.GOLEMS_PER_HOOKAH.getAsInt());
    }

    public int golems(long now) {
        smokers(now);
        return Math.min(golems.size(), Config.GOLEMS_PER_HOOKAH.getAsInt());
    }

    public boolean puffedBy(UUID smoker, long now) {
        Long tick = lastPuff.get(smoker);
        return tick != null && now - tick <= Config.ZONE_SMOKER_MEMORY_SEC.getAsInt() * 20L;
    }

    public boolean puffedWithin(long now, int ticks) {
        for (long tick : lastPuff.values()) {
            if (now - tick <= ticks) return true;
        }
        return false;
    }

    public boolean seat(UUID golem, BlockPos cushion, long now) {
        seats.values().removeIf(seat -> now - seat.tick() > SEAT_HOLD_TICKS);
        if (!seats.containsKey(golem) && seats.size() >= Config.GOLEMS_PER_HOOKAH.getAsInt()) return false;
        seats.put(golem, new Seat(cushion.immutable(), now));
        return true;
    }

    public boolean takenByOther(BlockPos cushion, UUID golem, long now) {
        for (Map.Entry<UUID, Seat> seat : seats.entrySet()) {
            if (!seat.getKey().equals(golem) && seat.getValue().cushion().equals(cushion) && now - seat.getValue().tick() <= SEAT_HOLD_TICKS) return true;
        }
        return false;
    }

    public void leave(UUID golem) {
        seats.remove(golem);
    }

    public void tick(ServerLevel level, BlockPos pos, boolean smoldering, boolean modular, String flavor, int color) {
        long now = level.getGameTime();
        int smokers = smoldering ? smokers(now) : 0;
        if (smokers == 0) return;
        if (smokers >= 3 && now >= furnitureCheckAt) {
            furnitureCheckAt = now + FURNITURE_RECHECK_TICKS;
            furniture = KaifZones.countFurniture(level, pos, Config.ZONE_RADIUS_T3.getAsInt());
        }
        int tier = KaifZones.tierFor(smokers, modular, smokers >= 3 ? furniture : 0);
        int people = smokers - golems(now);
        int earned = KaifZones.tierFor(people, modular, people >= 3 ? furniture : 0);
        int radius = KaifZones.radiusFor(tier);
        if (KaifZones.lofiPlayingNear(level, pos, radius)) radius += Config.ZONE_JUKEBOX_BONUS_RADIUS.getAsInt();
        if (now > heldUntil[tier]) PacketDistributor.sendToPlayersTrackingChunk(level, new ChunkPos(pos), new ZoneRingPacket(pos, radius, color));
        Arrays.fill(heldUntil, 1, tier + 1, now + KaifZones.TIER_HOLD_TICKS);
        KaifZones.spread(level, pos, tier, earned, radius, flavor, color);
    }
}
