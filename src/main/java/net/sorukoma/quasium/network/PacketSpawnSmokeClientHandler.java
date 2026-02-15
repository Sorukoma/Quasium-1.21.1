package net.sorukoma.quasium.network;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class PacketSpawnSmokeClientHandler {
    public static void handle(final PacketSpawnSmoke payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            Player player = minecraft.player;

            if (player != null) {
                Vec3 look = payload.lookDirection();
                // Спавним частицы дыма от головы игрока
                // Координаты спавна частиц: x, y, z
                // Скорость частиц: dx, dy, dz
                // Количество частиц
                for (int i = 0; i < 20; i++) { // Спавним 20 частиц для начала
                    double x = player.getX();
                    double y = player.getY() + player.getEyeHeight(); // От головы игрока
                    double z = player.getZ();

                    // Небольшой случайный разброс для начальной позиции
                    double offsetX = (player.getRandom().nextDouble() - 0.5) * 0.2;
                    double offsetY = (player.getRandom().nextDouble() - 0.5) * 0.2;
                    double offsetZ = (player.getRandom().nextDouble() - 0.5) * 0.2;

                    // Скорость частиц в направлении взгляда игрока
                    // Увеличиваем скорость, чтобы частицы двигались дальше
                    double speedMultiplier = 0.2;
                    double dx = look.x * speedMultiplier + offsetX * 0.5;
                    double dy = look.y * speedMultiplier + offsetY * 0.5;
                    double dz = look.z * speedMultiplier + offsetZ * 0.5;

                    player.level().addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, x + offsetX, y + offsetY, z + offsetZ, dx, dy, dz);
                }
            }
        });
    }
}
