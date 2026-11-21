package net.sorukoma.quasium.ecology;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.sorukoma.quasium.Config;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.api.RitualCompleteEvent;
import org.jetbrains.annotations.Nullable;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID)
public final class EcologyTicker {
    private EcologyTicker() {
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        long now = level.getGameTime();
        if (now % SmogMath.TICKS_PER_MINUTE == 0) minute(level);
        if (now % SmogEffects.CHECK_TICKS == 0) {
            SmogEffects.tick(level, now);
            SmogVillagers.cough(level);
        }
        if (now % (EcologyConfig.SPAWN_INTERVAL_SECONDS.getAsInt() * 20L) == 0) SmogSpawner.spawn(level);
    }

    public static void minute(ServerLevel level) {
        long[] chunks = EcologyIndex.snapshot(level);
        for (long key : chunks) {
            LevelChunk chunk = loaded(level, key);
            if (chunk != null) step(level, chunk);
        }
        SmogSpread.drift(level, chunks);
        for (long key : chunks) {
            LevelChunk chunk = loaded(level, key);
            if (chunk == null) continue;
            EcologyNetwork.sendChunk(chunk);
            EcologyIndex.settle(level, chunk);
        }
    }

    public static void step(ServerLevel level, LevelChunk chunk) {
        int density = SmokeDensity.density(chunk);
        ChunkSmog smog = SmokeDensity.smog(chunk);
        int removed = EcologyConfig.DENSITY_DECAY_PER_MINUTE.getAsInt();
        if (density > 0) {
            removed += SmogMath.treeRemoval(ChunkTrees.count(chunk), EcologyConfig.TREE_DENSITY_PER_MINUTE.getAsDouble(),
                    EcologyConfig.TREE_DENSITY_CAP_PER_MINUTE.getAsInt(), level.random.nextDouble());
        }
        SmogMath.Minute after = SmogMath.minute(new SmogMath.Minute(density, smog.level(), smog.fullMinutes(), smog.cleanMinutes()),
                removed, rules());
        SmokeDensity.setDensity(chunk, after.density());
        SmokeDensity.setSmog(chunk, new ChunkSmog(after.level(), after.fullMinutes(), after.cleanMinutes()));
    }

    public static SmogMath.Rules rules() {
        return new SmogMath.Rules(Config.DENSITY_MAX.getAsInt(), EcologyConfig.SMOG_ONSET_MINUTES.getAsInt(),
                EcologyConfig.SMOG_RESET_DENSITY.getAsInt(), EcologyConfig.SMOG_FADE_MINUTES.getAsInt());
    }

    public static void zoneTick(ServerLevel level, BlockPos center, int callEvery) {
        int perMinute = EcologyConfig.ZONE_DENSITY_PER_MINUTE.getAsInt();
        if (perMinute > 0 && level.getGameTime() % SmogMath.ticksBetween(perMinute) < callEvery) SmokeDensity.add(level, center, 1);
    }

    @SubscribeEvent
    public static void onRitualComplete(RitualCompleteEvent event) {
        SmokeDensity.add(event.level(), event.center(), EcologyConfig.RITUAL_DENSITY.getAsInt());
    }

    @Nullable
    public static LevelChunk loaded(ServerLevel level, long key) {
        return level.getChunkSource().getChunkNow(ChunkPos.getX(key), ChunkPos.getZ(key));
    }
}
