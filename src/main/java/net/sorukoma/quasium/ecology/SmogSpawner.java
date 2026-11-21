package net.sorukoma.quasium.ecology;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

public final class SmogSpawner {
    private static final int TRIES = 4;
    private static final double NEAR_PLAYER = 64.0;
    private static final double TOO_CLOSE = 8.0;

    private SmogSpawner() {
    }

    public static void spawn(ServerLevel level) {
        if (level.getDifficulty() == Difficulty.PEACEFUL) return;
        for (long key : EcologyIndex.snapshot(level)) {
            LevelChunk chunk = EcologyTicker.loaded(level, key);
            if (chunk != null && SmokeDensity.smog(chunk).level() > 0 && watched(level, chunk.getPos())) fill(level, chunk);
        }
    }

    public static int fill(ServerLevel level, LevelChunk chunk) {
        int smog = SmokeDensity.smog(chunk).level();
        int spawned = 0;
        if (!level.isDay()) {
            int cap = SmogMath.dymnikCap(smog, EcologyConfig.DYMNIK_MIN_SMOG.getAsInt(), EcologyConfig.DYMNIK_CAP_PER_LEVEL.getAsInt());
            spawned += topUp(level, chunk, SmogMobs.DYMNIK.get(), cap, true);
        }
        if (smog >= EcologyConfig.GAREVSHCHIK_MIN_SMOG.getAsInt()) {
            spawned += topUp(level, chunk, SmogMobs.GAREVSHCHIK.get(), EcologyConfig.GAREVSHCHIK_CAP.getAsInt(), false);
        }
        return spawned;
    }

    public static int count(ServerLevel level, ChunkPos pos, EntityType<?> type) {
        return level.getEntities(type, column(level, pos), Entity::isAlive).size();
    }

    public static AABB column(ServerLevel level, ChunkPos pos) {
        return new AABB(pos.getMinBlockX(), level.getMinBuildHeight(), pos.getMinBlockZ(),
                pos.getMaxBlockX() + 1, level.getMaxBuildHeight(), pos.getMaxBlockZ() + 1);
    }

    private static int topUp(ServerLevel level, LevelChunk chunk, EntityType<?> type, int cap, boolean flying) {
        if (cap <= 0 || count(level, chunk.getPos(), type) >= cap) return 0;
        BlockPos spot = spot(level, chunk, type, flying);
        return spot != null && type.spawn(level, spot, MobSpawnType.EVENT) != null ? 1 : 0;
    }

    @Nullable
    private static BlockPos spot(ServerLevel level, LevelChunk chunk, EntityType<?> type, boolean flying) {
        ChunkPos pos = chunk.getPos();
        for (int attempt = 0; attempt < TRIES; attempt++) {
            int x = pos.getMinBlockX() + level.random.nextInt(16);
            int z = pos.getMinBlockZ() + level.random.nextInt(16);
            int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos candidate = new BlockPos(x, ground + (flying ? 1 + level.random.nextInt(3) : 0), z);
            if (!level.getFluidState(candidate.below()).isEmpty() && !flying) continue;
            if (level.noCollision(type.getSpawnAABB(x + 0.5, candidate.getY(), z + 0.5)) && !playerNear(level, candidate)) return candidate;
        }
        return null;
    }

    private static boolean playerNear(ServerLevel level, BlockPos pos) {
        return level.getNearestPlayer(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, TOO_CLOSE, false) != null;
    }

    private static boolean watched(ServerLevel level, ChunkPos pos) {
        double x = pos.getMiddleBlockX();
        double z = pos.getMiddleBlockZ();
        for (ServerPlayer player : level.players()) {
            double dx = player.getX() - x;
            double dz = player.getZ() - z;
            if (!player.isSpectator() && dx * dx + dz * dz <= NEAR_PLAYER * NEAR_PLAYER) return true;
        }
        return false;
    }
}
