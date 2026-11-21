package net.sorukoma.quasium.client.geode;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.sorukoma.quasium.client.ClientKaifData;
import net.sorukoma.quasium.progression.ConfigArgs;
import net.sorukoma.quasium.selection.SelectionConfig;
import net.sorukoma.quasium.selection.geode.GeodeReveal;
import net.sorukoma.quasium.sound.ModSoundEvents;
import org.joml.Vector3f;

public final class GeodeAmbience {
    private static final int HUM_INTERVAL_TICKS = 40;
    private static final double FACE_OFFSET = 0.5625;
    private static final DustParticleOptions SHIMMER = new DustParticleOptions(new Vector3f(0.72F, 0.42F, 1.0F), 0.8F);
    private static long lastHum = -HUM_INTERVAL_TICKS;

    private GeodeAmbience() {
    }

    public static void animate(Level level, BlockPos pos, RandomSource random) {
        if (!GeodeReveal.revealed(ClientKaifData.getKaifValue())) return;
        shimmer(level, pos, random);
        hum(level, pos, random);
    }

    private static void shimmer(Level level, BlockPos pos, RandomSource random) {
        Direction face = Direction.getRandom(random);
        BlockPos side = pos.relative(face);
        if (level.getBlockState(side).isSolidRender(level, side)) return;
        level.addParticle(SHIMMER, pos.getX() + along(face, Direction.Axis.X, random), pos.getY() + along(face, Direction.Axis.Y, random),
                pos.getZ() + along(face, Direction.Axis.Z, random), 0, 0, 0);
    }

    private static double along(Direction face, Direction.Axis axis, RandomSource random) {
        return face.getAxis() == axis ? 0.5 + FACE_OFFSET * face.getAxisDirection().getStep() : random.nextFloat();
    }

    private static void hum(Level level, BlockPos pos, RandomSource random) {
        Player player = Minecraft.getInstance().player;
        long now = level.getGameTime();
        if (player == null || Math.abs(now - lastHum) < HUM_INTERVAL_TICKS) return;
        if (!pos.closerToCenterThan(player.position(), ConfigArgs.synced(SelectionConfig.GEODE_HUM_RADIUS))) return;
        lastHum = now;
        level.playLocalSound(pos, ModSoundEvents.GEODE_HUM.get(), SoundSource.BLOCKS, 0.7F, 0.9F + random.nextFloat() * 0.2F, false);
    }
}
