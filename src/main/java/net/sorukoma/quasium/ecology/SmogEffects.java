package net.sorukoma.quasium.ecology;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.chunk.LevelChunk;
import net.sorukoma.quasium.advancements.triggers.SmokecraftTrigger;

public final class SmogEffects {
    public static final int CHECK_TICKS = 100;
    private static final int LINGER_TICKS = CHECK_TICKS + 40;
    private static final int NAUSEA_FROM = 2;
    private static final int FATIGUE_FROM = 3;
    private static final int POISON_FROM = 4;

    private SmogEffects() {
    }

    public static void tick(ServerLevel level, long now) {
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator()) continue;
            LevelChunk chunk = level.getChunkAt(player.blockPosition());
            int smog = SmokeDensity.smog(chunk).level();
            if (SmokeDensity.density(chunk) >= EcologyConfig.BONUS_THRESHOLD.getAsInt()) SmokecraftTrigger.fire(player, "density_40");
            if (smog > 0) SmokecraftTrigger.fire(player, "smog");
            if (!player.isCreative()) breathe(player, smog, now % SmogMath.TICKS_PER_MINUTE < CHECK_TICKS);
        }
    }

    public static void breathe(ServerPlayer player, int smog, boolean minuteStarts) {
        if (smog >= SmogMath.MAX_LEVEL) {
            give(player, MobEffects.CONFUSION, LINGER_TICKS);
        } else if (smog >= NAUSEA_FROM && minuteStarts) {
            int seconds = EcologyConfig.SMOG_NAUSEA_SECONDS.getAsInt();
            if (seconds > 0) give(player, MobEffects.CONFUSION, seconds * 20);
        }
        if (smog >= FATIGUE_FROM) give(player, MobEffects.DIG_SLOWDOWN, LINGER_TICKS);
        if (smog >= POISON_FROM) give(player, MobEffects.POISON, LINGER_TICKS);
    }

    private static void give(ServerPlayer player, Holder<MobEffect> effect, int ticks) {
        player.addEffect(new MobEffectInstance(effect, ticks, 0, true, true, true));
    }
}
