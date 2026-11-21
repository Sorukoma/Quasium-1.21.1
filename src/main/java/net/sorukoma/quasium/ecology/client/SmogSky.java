package net.sorukoma.quasium.ecology.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.ecology.EcologyConfig;
import net.sorukoma.quasium.ecology.EcologyRegistry;
import net.sorukoma.quasium.progression.ConfigArgs;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID, value = Dist.CLIENT)
public final class SmogSky {
    private static final float HAZE = 0.25f;
    private static final float PER_LEVEL = 0.15f;
    private static final float EASE = 0.04f;
    private static final float TINT = 0.75f;
    private static final float THINNEST_FOG = 20f;
    private static final float SHADE = 0.55f;
    private static final int AMBIENCE_TICKS = 80;
    private static final int AMBIENCE_FROM = 2;
    private static final int PARTICLE_SPREAD = 24;

    private static float shown;

    private SmogSky() {
    }

    public static float thickness(int density, int smog) {
        float air = smog > 0 ? Math.min(1f, HAZE + smog * PER_LEVEL) : density >= ConfigArgs.synced(EcologyConfig.HAZE_THRESHOLD) ? HAZE : 0f;
        return air * ConfigArgs.synced(EcologyConfig.SMOG_TINT_PCT) / 100f;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            shown = 0f;
            return;
        }
        if (minecraft.isPaused()) return;
        BlockPos pos = player.blockPosition();
        int smog = ClientSmog.smog(pos);
        shown += (thickness(ClientSmog.density(pos), smog) - shown) * EASE;
        if (smog <= 0) return;
        RandomSource random = minecraft.level.random;
        for (int i = 0; i < smog; i++) {
            minecraft.level.addParticle(smog >= 3 ? ParticleTypes.ASH : ParticleTypes.WHITE_ASH,
                    player.getX() + (random.nextDouble() - 0.5) * PARTICLE_SPREAD, player.getY() + random.nextDouble() * 8 - 2,
                    player.getZ() + (random.nextDouble() - 0.5) * PARTICLE_SPREAD, 0, 0, 0);
        }
        if (smog >= AMBIENCE_FROM && player.tickCount % AMBIENCE_TICKS == 0) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forLocalAmbience(EcologyRegistry.SMOG_AMBIENCE.get(), 0.2f + 0.1f * smog, 1f));
        }
    }

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        if (shown <= 0.01f) return;
        float amount = shown * TINT;
        float ash = ashen(event.getRed(), event.getGreen(), event.getBlue());
        event.setRed(Mth.lerp(amount, event.getRed(), ash));
        event.setGreen(Mth.lerp(amount, event.getGreen(), ash));
        event.setBlue(Mth.lerp(amount, event.getBlue(), ash));
    }

    private static float ashen(float red, float green, float blue) {
        return (0.299f * red + 0.587f * green + 0.114f * blue) * SHADE;
    }

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        if (shown <= 0.01f || event.getType() != FogType.NONE) return;
        float far = Mth.lerp(shown, event.getFarPlaneDistance(), Math.min(event.getFarPlaneDistance(), THINNEST_FOG));
        event.setFarPlaneDistance(far);
        event.setNearPlaneDistance(Math.min(event.getNearPlaneDistance(), far * (1f - shown)));
        event.setCanceled(true);
    }
}
