package net.sorukoma.quasium.ecology;

import it.unimi.dsi.fastutil.HashCommon;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;

public final class SmogSpread {
    private static final long TICKS_PER_DAY = 24000L;

    private SmogSpread() {
    }

    public static long day(ServerLevel level) {
        return level.getDayTime() / TICKS_PER_DAY;
    }

    public static Direction wind(ServerLevel level) {
        return Direction.from2DDataValue(Math.floorMod((int) HashCommon.murmurHash3(level.getSeed() + day(level)), 4));
    }

    public static Component describe(ServerLevel level) {
        return Component.translatable("quasium.scan.wind", Component.translatable("quasium.wind." + wind(level).getName()));
    }

    public static void drift(ServerLevel level, long[] chunks) {
        Direction wind = wind(level);
        int pctPerLevel = EcologyConfig.SMOG_SPREAD_PCT_PER_LEVEL.getAsInt();
        for (long key : chunks) {
            LevelChunk source = EcologyTicker.loaded(level, key);
            if (source == null) continue;
            int smog = SmokeDensity.smog(source).level();
            if (smog == 0 || level.random.nextInt(100) >= SmogMath.spreadChancePct(smog, pctPerLevel)) continue;
            int targets = SmogMath.spreadTargets(smog, EcologyConfig.SMOG_SPREAD_NEIGHBOURS_AT_MAX.getAsInt());
            Direction side = level.random.nextBoolean() ? wind.getClockWise() : wind.getCounterClockWise();
            Direction[] order = {wind, side, side.getOpposite(), wind.getOpposite()};
            for (int i = 0; i < targets && i < order.length; i++) push(level, source, order[i]);
        }
    }

    private static void push(ServerLevel level, LevelChunk source, Direction toward) {
        ChunkPos from = source.getPos();
        LevelChunk target = EcologyTicker.loaded(level, ChunkPos.asLong(from.x + toward.getStepX(), from.z + toward.getStepZ()));
        if (target == null || SmokeDensity.smog(target).level() >= SmokeDensity.smog(source).level()) return;
        SmokeDensity.addSmog(source, -1);
        SmokeDensity.addSmog(target, 1);
    }
}
