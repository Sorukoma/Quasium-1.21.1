package net.sorukoma.quasium.ritual.artifact;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.ritual.RitualConfig;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID)
public final class SignalColumns {
    private static final int PUFF_EVERY_TICKS = 5;
    private static final int STEP_BLOCKS = 4;
    private static final List<Column> ACTIVE = new ArrayList<>();

    private record Column(ResourceKey<Level> dimension, BlockPos base, long until) {
    }

    private SignalColumns() {
    }

    public static void raise(ServerLevel level, BlockPos base) {
        ACTIVE.add(new Column(level.dimension(), base.immutable(), level.getGameTime() + RitualConfig.SIGNAL_PIPE_COLUMN_SEC.getAsInt() * 20L));
    }

    public static boolean standing(ServerLevel level, BlockPos base) {
        return ACTIVE.stream().anyMatch(column -> column.dimension() == level.dimension() && column.base().equals(base));
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || ACTIVE.isEmpty()) return;
        long now = level.getGameTime();
        ACTIVE.removeIf(column -> column.dimension() == level.dimension() && column.until() < now);
        if (now % PUFF_EVERY_TICKS != 0) return;
        int height = RitualConfig.SIGNAL_PIPE_HEIGHT.getAsInt();
        for (Column column : ACTIVE) {
            if (column.dimension() != level.dimension()) continue;
            for (ServerPlayer player : level.players()) {
                for (int rise = 1; rise <= height; rise += STEP_BLOCKS) {
                    level.sendParticles(player, ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, true, column.base().getX() + 0.5,
                            column.base().getY() + rise, column.base().getZ() + 0.5, 1, 0.15, 0.4, 0.15, 0.01);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        ACTIVE.clear();
    }
}
