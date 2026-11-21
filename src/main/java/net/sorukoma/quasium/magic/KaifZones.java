package net.sorukoma.quasium.magic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.sorukoma.quasium.Config;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.compat.SableCompat;
import net.sorukoma.quasium.ecology.EcologyTicker;
import net.sorukoma.quasium.golem.WorkerGolem;
import net.sorukoma.quasium.particle.ModParticles;

public final class KaifZones {
    public static final int TICK_INTERVAL = 10;
    public static final TagKey<Block> LOUNGE_FURNITURE =
            TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "lounge_furniture"));
    public static final ResourceKey<JukeboxSong> LOFI_SONG =
            ResourceKey.create(Registries.JUKEBOX_SONG, ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "lofi_kaif_beats"));
    public static final int NIRVANA_SMOKERS = 3;
    public static final int TIER_HOLD_TICKS = 200;
    private static final int BOUNDARY_PARTICLES = 8;

    private KaifZones() {
    }

    public static int tierFor(int smokers, boolean modular, int furniture) {
        if (smokers <= 0) return 0;
        if (smokers >= NIRVANA_SMOKERS && furniture >= Config.NIRVANA_FURNITURE.getAsInt()) return PlayerZones.NIRVANA;
        return smokers >= 2 || modular ? 2 : 1;
    }

    public static int radiusFor(int tier) {
        return switch (tier) {
            case 1 -> Config.ZONE_RADIUS_T1.getAsInt();
            case 2 -> Config.ZONE_RADIUS_T2.getAsInt();
            default -> Config.ZONE_RADIUS_T3.getAsInt();
        };
    }

    public static void spread(ServerLevel level, BlockPos center, int tier, int earned, int radius, String flavor, int color) {
        if (tier <= 0) return;
        EcologyTicker.zoneTick(level, center, TICK_INTERVAL);
        long now = level.getGameTime();
        Vec3 origin = Vec3.atCenterOf(center);
        double reachSqr = (double) radius * radius;
        for (ServerPlayer player : level.players()) {
            if (!player.isSpectator() && SableCompat.distanceSqr(player, origin) <= reachSqr) {
                PlayerZones.offer(player, tier, earned, flavor, color, now);
            }
        }
        for (WorkerGolem golem : level.getEntitiesOfClass(WorkerGolem.class, new AABB(center).inflate(radius),
                golem -> golem.distanceToSqr(origin) <= reachSqr)) {
            golem.vibe(now);
        }
        for (int i = 0; i < BOUNDARY_PARTICLES; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            level.sendParticles(ModParticles.noteSmoke(color), origin.x + Math.cos(angle) * radius, origin.y + level.random.nextDouble(),
                    origin.z + Math.sin(angle) * radius, 1, 0.05, 0.05, 0.05, 0.0);
        }
    }

    public static int countFurniture(ServerLevel level, BlockPos center, int radius) {
        int found = 0;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -2, -radius), center.offset(radius, 2, radius))) {
            if (pos.distSqr(center) <= (double) radius * radius && level.getBlockState(pos).is(LOUNGE_FURNITURE)) found++;
        }
        return found;
    }

    public static boolean lofiPlayingNear(ServerLevel level, BlockPos center, int radius) {
        ChunkPos min = new ChunkPos(center.offset(-radius, 0, -radius));
        ChunkPos max = new ChunkPos(center.offset(radius, 0, radius));
        for (int cx = min.x; cx <= max.x; cx++) {
            for (int cz = min.z; cz <= max.z; cz++) {
                if (!level.hasChunk(cx, cz)) continue;
                for (BlockEntity entity : level.getChunk(cx, cz).getBlockEntities().values()) {
                    if (entity instanceof JukeboxBlockEntity jukebox && entity.getBlockPos().distSqr(center) <= (double) radius * radius
                            && playsLofi(level, jukebox)) return true;
                }
            }
        }
        return false;
    }

    private static boolean playsLofi(ServerLevel level, JukeboxBlockEntity jukebox) {
        if (!jukebox.getSongPlayer().isPlaying() || jukebox.getSongPlayer().getSong() == null) return false;
        return level.registryAccess().registryOrThrow(Registries.JUKEBOX_SONG)
                .getResourceKey(jukebox.getSongPlayer().getSong()).filter(LOFI_SONG::equals).isPresent();
    }
}
