package net.sorukoma.quasium.network;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class PacketSpawnSmokeClientHandler {
    public static void handle(final PacketSpawnSmoke payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level == null) {
                return;
            }

            // Находим именно курящего игрока по ID сущности (а не локального игрока),
            // чтобы дым шёл из головы того, кто реально затянулся.
            Entity smoker = minecraft.level.getEntity(payload.smokerEntityId());
            if (smoker == null) {
                return;
            }

            Vec3 look = payload.lookDirection();
            // Спавним частицы дыма от головы курящего игрока
            for (int i = 0; i < 20; i++) {
                double x = smoker.getX();
                double y = smoker.getY() + smoker.getEyeHeight(); // От головы
                double z = smoker.getZ();

                // Небольшой случайный разброс для начальной позиции
                double offsetX = (smoker.getRandom().nextDouble() - 0.5) * 0.2;
                double offsetY = (smoker.getRandom().nextDouble() - 0.5) * 0.2;
                double offsetZ = (smoker.getRandom().nextDouble() - 0.5) * 0.2;

                // Скорость частиц в направлении взгляда курящего
                double speedMultiplier = 0.2;
                double dx = look.x * speedMultiplier + offsetX * 0.5;
                double dy = look.y * speedMultiplier + offsetY * 0.5;
                double dz = look.z * speedMultiplier + offsetZ * 0.5;

                smoker.level().addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, x + offsetX, y + offsetY, z + offsetZ, dx, dy, dz);
            }
        });
    }
}
