package net.sorukoma.quasium.ecology;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.ChunkWatchEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.sorukoma.quasium.QuasiumMod;

import java.util.HashMap;
import java.util.Map;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID)
public final class EcologyIndex {
    private static final Map<ResourceKey<Level>, LongSet> ACTIVE = new HashMap<>();

    private EcologyIndex() {
    }

    static void touch(LevelChunk chunk) {
        if (chunk.getLevel() instanceof ServerLevel level && isDirty(chunk)) {
            ACTIVE.computeIfAbsent(level.dimension(), key -> new LongOpenHashSet()).add(chunk.getPos().toLong());
        }
    }

    public static long[] snapshot(ServerLevel level) {
        LongSet chunks = ACTIVE.get(level.dimension());
        return chunks == null ? new long[0] : chunks.toLongArray();
    }

    static void settle(ServerLevel level, LevelChunk chunk) {
        if (isDirty(chunk)) return;
        LongSet chunks = ACTIVE.get(level.dimension());
        if (chunks != null) chunks.remove(chunk.getPos().toLong());
    }

    private static boolean isDirty(LevelChunk chunk) {
        return SmokeDensity.density(chunk) > 0 || !SmokeDensity.smog(chunk).isClear();
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getChunk() instanceof LevelChunk chunk) touch(chunk);
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            LongSet chunks = ACTIVE.get(level.dimension());
            if (chunks != null) chunks.remove(event.getChunk().getPos().toLong());
        }
    }

    @SubscribeEvent
    public static void onChunkSent(ChunkWatchEvent.Sent event) {
        if (isDirty(event.getChunk())) EcologyNetwork.sendChunk(event.getPlayer(), event.getChunk());
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) ACTIVE.remove(level.dimension());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        ACTIVE.clear();
    }
}
