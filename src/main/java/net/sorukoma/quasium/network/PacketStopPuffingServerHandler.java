package net.sorukoma.quasium.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.sorukoma.quasium.block.entity.HookahBlockEntity;
import net.sorukoma.quasium.entity.HookahConnectionEntity; // Импортируем HookahConnectionEntity
import net.minecraft.world.phys.AABB; // Импортируем AABB
import java.util.List; // Импортируем List

public class PacketStopPuffingServerHandler {
    public static void handle(final PacketStopPuffing payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            BlockPos blockPos = payload.blockPos();

            if (player.level().isLoaded(blockPos)) {
                BlockEntity blockEntity = player.level().getBlockEntity(blockPos);
                if (blockEntity instanceof HookahBlockEntity hookahBlockEntity) {
                    // Ищем HookahConnectionEntity, привязанную к этому игроку и этому кальяну
                    List<HookahConnectionEntity> connections = player.level().getEntitiesOfClass(
                            HookahConnectionEntity.class,
                            new AABB(blockPos).inflate(HookahConnectionEntity.PIPE_DETACH_DISTANCE_SQ), // Ищем в радиусе отсоединения
                            connection -> connection.getAttachedPlayerUUID() != null &&
                                          connection.getAttachedPlayerUUID().equals(player.getUUID()) &&
                                          connection.getHookahBlockPos() != null &&
                                          connection.getHookahBlockPos().equals(blockPos)
                    );

                    if (!connections.isEmpty()) {
                        HookahConnectionEntity connectionEntity = connections.get(0); // Берем первое найденное соединение
                        connectionEntity.stopPuffing(hookahBlockEntity, player); // Передаем player
                    } else {
                        player.sendSystemMessage(Component.translatable("quasium.message.not_connected"));
                    }
                } else {
                    player.sendSystemMessage(Component.translatable("quasium.message.hookah_missing"));
                }
            } else {
                player.sendSystemMessage(Component.translatable("quasium.message.hookah_not_loaded"));
            }
        });
    }
}