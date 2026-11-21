package net.sorukoma.quasium.smokelands.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.progression.ConfigArgs;
import net.sorukoma.quasium.smokelands.SmokelandsConfig;
import net.sorukoma.quasium.smokelands.SmokelandsDimension;
import net.sorukoma.quasium.smokelands.SmokelandsNetwork;
import net.sorukoma.quasium.smokelands.SmokelandsSounds;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID, value = Dist.CLIENT)
public final class StormClient {
    private static final int ASH_PER_TICK = 8;
    private static final double ASH_REACH = 12.0;
    private static StormSound sound;

    private StormClient() {
    }

    static boolean raging() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.level != null && SmokelandsDimension.contains(minecraft.level) && SmokelandsNetwork.stormSeenByClient();
    }

    @SubscribeEvent
    public static void onFog(ViewportEvent.RenderFog event) {
        if (!raging()) return;
        float keep = 1f - ConfigArgs.synced(SmokelandsConfig.STORM_VISIBILITY_CUT_PCT) / 100f;
        event.setFarPlaneDistance(event.getFarPlaneDistance() * keep);
        event.setNearPlaneDistance(Math.min(event.getNearPlaneDistance(), event.getFarPlaneDistance() * 0.2f));
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        if (!raging()) return;
        event.setRed(event.getRed() * 0.6f + 0.12f);
        event.setGreen(event.getGreen() * 0.6f + 0.1f);
        event.setBlue(event.getBlue() * 0.6f + 0.08f);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.isPaused() || minecraft.level == null || !raging()) return;
        Vec3 eye = minecraft.gameRenderer.getMainCamera().getPosition();
        RandomSource random = minecraft.level.getRandom();
        int ash = switch (minecraft.options.particles().get()) {
            case ALL -> ASH_PER_TICK;
            case DECREASED -> ASH_PER_TICK / 3;
            case MINIMAL -> 0;
        };
        for (int i = 0; i < ash; i++) {
            double x = eye.x + (random.nextDouble() - 0.5) * 2 * ASH_REACH;
            double y = eye.y + (random.nextDouble() - 0.3) * ASH_REACH;
            double z = eye.z + (random.nextDouble() - 0.5) * 2 * ASH_REACH;
            minecraft.level.addParticle(random.nextBoolean() ? ParticleTypes.ASH : ParticleTypes.WHITE_ASH, x, y, z, 0.3, -0.02, 0.12);
        }
        if (sound == null || sound.isStopped()) {
            sound = new StormSound();
            minecraft.getSoundManager().play(sound);
        }
    }

    private static final class StormSound extends AbstractTickableSoundInstance {
        private static final float FULL = 0.7f;
        private static final int FADE_TICKS = 40;
        private static final float FADE_STEP = FULL / FADE_TICKS;

        private StormSound() {
            super(SmokelandsSounds.CINDER_STORM.get(), SoundSource.WEATHER, SoundInstance.createUnseededRandom());
            looping = true;
            delay = 0;
            volume = 0f;
            relative = true;
            attenuation = Attenuation.NONE;
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }

        @Override
        public void tick() {
            volume = Math.min(FULL, volume + (raging() ? FADE_STEP : -FADE_STEP));
            if (volume <= 0f) stop();
        }
    }
}
