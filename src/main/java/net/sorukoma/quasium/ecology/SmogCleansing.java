package net.sorukoma.quasium.ecology;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.sorukoma.quasium.advancements.triggers.SmokecraftTrigger;

public final class SmogCleansing {
    private static final double WITNESS_RANGE = 24.0;

    private SmogCleansing() {
    }

    public static int cleanse(ServerLevel level, BlockPos center, int levels) {
        ChunkPos middle = new ChunkPos(center);
        int removed = 0;
        boolean heaviest = false;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (!level.hasChunk(middle.x + dx, middle.z + dz)) continue;
                LevelChunk chunk = level.getChunk(middle.x + dx, middle.z + dz);
                int before = SmokeDensity.smog(chunk).level();
                if (before > 0) removed += before - SmokeDensity.addSmog(chunk, -levels);
                heaviest |= before >= SmogMath.MAX_LEVEL;
            }
        }
        if (removed > 0) celebrate(level, center, heaviest);
        return removed;
    }

    public static void lavender(ServerLevel level, BlockPos lamp, long time) {
        int perMinute = EcologyConfig.LAVENDER_DENSITY_PER_MINUTE.getAsInt();
        if (perMinute > 0 && time % SmogMath.ticksBetween(perMinute) == 0) SmokeDensity.addAround(level, lamp, 1, -1);
        if (time % (EcologyConfig.LAVENDER_SMOG_MINUTES.getAsInt() * (long) SmogMath.TICKS_PER_MINUTE) == 0) cleanse(level, lamp, 1);
    }

    private static void celebrate(ServerLevel level, BlockPos center, boolean heaviest) {
        level.playSound(null, center, EcologyRegistry.SMOG_CLEANSE.get(), SoundSource.BLOCKS, 1f, 1f);
        level.sendParticles(ParticleTypes.CLOUD, center.getX() + 0.5, center.getY() + 1.2, center.getZ() + 0.5, 12, 0.6, 0.4, 0.6, 0.02);
        if (!heaviest) return;
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(center.getX() + 0.5, center.getY(), center.getZ() + 0.5) <= WITNESS_RANGE * WITNESS_RANGE) {
                SmokecraftTrigger.fire(player, "smog_cleansed");
            }
        }
    }
}
