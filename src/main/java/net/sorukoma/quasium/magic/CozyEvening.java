package net.sorukoma.quasium.magic;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.level.biome.Biome;
import net.sorukoma.quasium.advancements.triggers.SmokecraftTrigger;
import net.sorukoma.quasium.entity.SeatEntity;

public final class CozyEvening {
    public static final String EVENT = "cozy_evening";
    private static final int CHECK_TICKS = 20;
    private static final double CAT_REACH = 8.0;
    private static final int DISC_REACH = 16;

    private CozyEvening() {
    }

    public static void tick(ServerPlayer player) {
        if (player.level().getGameTime() % CHECK_TICKS == 0) check(player);
    }

    public static void check(ServerPlayer player) {
        if (holds(player)) SmokecraftTrigger.fire(player, EVENT);
    }

    public static boolean holds(ServerPlayer player) {
        if (!SeatEntity.isSeated(player) || PlayerZones.tier(player) == 0 || !player.level().isRaining()) return false;
        ServerLevel level = player.serverLevel();
        BlockPos pos = player.blockPosition();
        return level.getBiome(pos).value().getPrecipitationAt(pos) == Biome.Precipitation.RAIN
                && !level.getEntitiesOfClass(Cat.class, player.getBoundingBox().inflate(CAT_REACH), Cat::isTame).isEmpty()
                && KaifZones.lofiPlayingNear(level, pos, DISC_REACH);
    }
}
