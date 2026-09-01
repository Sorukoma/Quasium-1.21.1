package net.sorukoma.quasium.network;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.sorukoma.quasium.util.SmokeColorUtil;
import org.joml.Vector3f;

public class PacketSpawnSmokeClientHandler {
    public static void handle(final PacketSpawnSmoke payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level == null) {
                return;
            }

            Entity smoker = minecraft.level.getEntity(payload.smokerEntityId());
            if (smoker == null) {
                return;
            }

            Vec3 look = payload.lookDirection();
            int rgb = payload.particleColor();
            float[] norm = SmokeColorUtil.toNormalizedRGB(rgb);
            Vector3f dustColor = new Vector3f(norm[0], norm[1], norm[2]);

            int count = Math.max(1, payload.particleCount());
            for (int i = 0; i < count; i++) {
                double x = smoker.getX();
                double y = smoker.getY() + smoker.getEyeHeight();
                double z = smoker.getZ();

                double offsetX = (smoker.getRandom().nextDouble() - 0.5) * 0.2;
                double offsetY = (smoker.getRandom().nextDouble() - 0.5) * 0.2;
                double offsetZ = (smoker.getRandom().nextDouble() - 0.5) * 0.2;

                double speedMultiplier = 0.2;
                double dx = look.x * speedMultiplier + offsetX * 0.5;
                double dy = look.y * speedMultiplier + offsetY * 0.5;
                double dz = look.z * speedMultiplier + offsetZ * 0.5;

                // Базовый белый/серый дым (объём)
                smoker.level().addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, x + offsetX, y + offsetY, z + offsetZ, dx, dy, dz);
                // Цветной DUST-оверлей по типу табака
                DustParticleOptions dust = new DustParticleOptions(dustColor, 0.6f);
                smoker.level().addParticle(dust, x + offsetX, y + offsetY, z + offsetZ, dx, dy * 0.5, dz);
            }
        });
    }
}
