package net.sorukoma.quasium.hookah;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.sorukoma.quasium.component.ModDataComponents.HookahBowlContents;
import net.sorukoma.quasium.data.PlayerOverdoseData;
import net.sorukoma.quasium.magic.SmokeTricks;
import net.sorukoma.quasium.mind.MindConfig;
import net.sorukoma.quasium.network.ModPackets;
import net.sorukoma.quasium.network.PacketSpawnSmoke;
import net.sorukoma.quasium.sound.ModSoundEvents;
import net.sorukoma.quasium.tea.SobrietySeals;
import net.sorukoma.quasium.util.SmokeColorUtil;
import org.jetbrains.annotations.Nullable;

public final class ExhaleSmoke {
    private ExhaleSmoke() {
    }

    public static void exhale(ServerPlayer smoker, @Nullable HookahBowlContents row) {
        if (!SmokeTricks.holdsExhale(smoker)) {
            int density = Math.round(SmokeColorUtil.smokeDensity(row) * (1f + sickness(smoker)));
            ModPackets.sendToClientsTrackingEntityAndSelf(new PacketSpawnSmoke(smoker.getId(), smoker.getLookAngle(), color(smoker, row), density),
                    smoker);
        }
        if (coughs(smoker)) dryCough(smoker);
    }

    public static int color(ServerPlayer smoker, @Nullable HookahBowlContents row) {
        return SmokeColorUtil.blend(SmokeColorUtil.colorForContents(row), SmokeColorUtil.SICK, sickness(smoker));
    }

    private static float sickness(ServerPlayer smoker) {
        if (SobrietySeals.covers(smoker)) return 0f;
        return SmokeColorUtil.sickness(PlayerOverdoseData.windowShare(smoker), MindConfig.SICK_SMOKE_FROM_PCT.getAsInt());
    }

    public static boolean coughs(ServerPlayer smoker) {
        return !SobrietySeals.covers(smoker) && PlayerOverdoseData.windowShare(smoker) * 100f >= MindConfig.SICK_COUGH_FROM_PCT.getAsInt();
    }

    private static void dryCough(ServerPlayer smoker) {
        smoker.level().playSound(null, smoker.getX(), smoker.getEyeY(), smoker.getZ(), ModSoundEvents.DRY_COUGH.get(), SoundSource.PLAYERS,
                0.8f, 0.95f + smoker.getRandom().nextFloat() * 0.1f);
        Vec3 mouth = smoker.getEyePosition().add(smoker.getLookAngle().scale(0.4));
        smoker.serverLevel().sendParticles(ParticleTypes.SMOKE, mouth.x, mouth.y, mouth.z, 3, 0.05, 0.05, 0.05, 0.01);
    }
}
