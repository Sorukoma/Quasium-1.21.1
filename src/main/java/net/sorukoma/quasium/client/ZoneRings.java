package net.sorukoma.quasium.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.particle.ModParticles;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID, value = Dist.CLIENT)
public final class ZoneRings {
    private static final int ROLL_TICKS = 12;
    private static final double PUFF_SPACING = 0.7;
    private static final double DRIFT = 0.25;
    private static final List<Ring> RINGS = new ArrayList<>();

    private static final class Ring {
        private final Vec3 center;
        private final int radius;
        private final int color;
        private int age;

        private Ring(Vec3 center, int radius, int color) {
            this.center = center;
            this.radius = radius;
            this.color = color;
        }
    }

    private ZoneRings() {
    }

    public static void start(BlockPos hookah, int radius, int color) {
        RINGS.add(new Ring(new Vec3(hookah.getX() + 0.5, hookah.getY() + 0.3, hookah.getZ() + 0.5), radius, color));
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.isPaused() || RINGS.isEmpty()) return;
        for (Iterator<Ring> it = RINGS.iterator(); it.hasNext(); ) {
            Ring ring = it.next();
            ring.age++;
            roll(minecraft.level, ring);
            if (ring.age >= ROLL_TICKS) it.remove();
        }
    }

    private static void roll(ClientLevel level, Ring ring) {
        double reach = ring.radius * ring.age / (double) ROLL_TICKS;
        double speed = DRIFT * ring.radius / ROLL_TICKS;
        int puffs = Math.max(8, (int) Math.ceil(2 * Math.PI * reach / PUFF_SPACING));
        ColorParticleOption smoke = ModParticles.noteSmoke(ring.color);
        RandomSource random = level.random;
        for (int puff = 0; puff < puffs; puff++) {
            double angle = (puff + random.nextDouble()) * 2 * Math.PI / puffs;
            double dx = Math.cos(angle);
            double dz = Math.sin(angle);
            level.addParticle(smoke, ring.center.x + dx * reach, ring.center.y + random.nextDouble() * 0.3, ring.center.z + dz * reach,
                    dx * speed, 0.0, dz * speed);
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        RINGS.clear();
    }
}
