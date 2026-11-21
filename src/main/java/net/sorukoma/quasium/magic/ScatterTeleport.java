package net.sorukoma.quasium.magic;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;

public final class ScatterTeleport {
    private static final int TRIES = 16;

    private ScatterTeleport() {
    }

    public static boolean scatter(ServerLevel level, LivingEntity target, double scatter) {
        if (target.getType().is(Tags.EntityTypes.BOSSES)) return false;
        RandomSource random = target.getRandom();
        Vec3 from = target.position();
        for (int attempt = 0; attempt < TRIES; attempt++) {
            double x = from.x + (random.nextDouble() - 0.5) * 2 * scatter;
            double y = Mth.clamp(from.y + random.nextInt(17) - 8, level.getMinBuildHeight(), level.getMaxBuildHeight() - 1);
            double z = from.z + (random.nextDouble() - 0.5) * 2 * scatter;
            if (target.isPassenger()) target.stopRiding();
            if (!target.randomTeleport(x, y, z, true)) continue;
            level.gameEvent(GameEvent.TELEPORT, from, GameEvent.Context.of(target));
            level.playSound(null, from.x, from.y, from.z, SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.HOSTILE, 1f, 0.7f);
            if (target instanceof Mob mob) mob.setTarget(null);
            return true;
        }
        return false;
    }
}
