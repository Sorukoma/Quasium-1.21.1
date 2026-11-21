package net.sorukoma.quasium.magic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.sorukoma.quasium.Config;
import net.sorukoma.quasium.mind.BlockFinder;
import net.sorukoma.quasium.network.OreGlowPacket;

import java.util.List;

public final class OreWhisper {
    private OreWhisper() {
    }

    public static List<BlockPos> reveal(ServerLevel level, ServerPlayer viewer, Vec3 center, int radius, int glowTicks) {
        List<BlockPos> ores = BlockFinder.near(level, BlockPos.containing(center), radius,
                BlockFinder.matching(Config.WHISPER_ORES.get()), OreGlowPacket.MAX_POSITIONS);
        PacketDistributor.sendToPlayer(viewer, new OreGlowPacket(ores, glowTicks));
        level.sendParticles(ParticleTypes.FLAME, center.x, center.y + 1.0, center.z, 24, 0.8, 0.4, 0.8, 0.02);
        return ores;
    }
}
