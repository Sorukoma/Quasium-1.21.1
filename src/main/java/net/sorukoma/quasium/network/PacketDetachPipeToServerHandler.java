package net.sorukoma.quasium.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.sorukoma.quasium.block.entity.HookahBlockEntity;
import net.sorukoma.quasium.entity.HookahConnectionEntity;
import net.minecraft.world.phys.AABB;
import java.util.List;
import java.util.Optional;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public class PacketDetachPipeToServerHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final double MAX_DISTANCE_SQ = 64.0; // 8 blocks

    public static void handle(final PacketDetachPipeToServer payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            BlockPos blockPos = payload.blockPos();

            LOGGER.debug("Server: Received PacketDetachPipeToServer from player {} for block at {}", player.getName().getString(), blockPos);

            // Distance validation
            if (player.blockPosition().distSqr(blockPos) > MAX_DISTANCE_SQ) {
                LOGGER.warn("Server: Player {} tried to detach pipe from hookah at {} but is too far away.", player.getName().getString(), blockPos);
                return;
            }

            if (player.level().isLoaded(blockPos)) {
                BlockEntity blockEntity = player.level().getBlockEntity(blockPos);
                if (blockEntity instanceof HookahBlockEntity hookahBlockEntity) {
                    List<HookahConnectionEntity> existingConnections = player.level().getEntitiesOfClass(
                            HookahConnectionEntity.class,
                            new AABB(blockPos).inflate(HookahConnectionEntity.PIPE_DETACH_DISTANCE_SQ + 1.0),
                            hookahConnection -> hookahConnection.getAttachedPlayerUUID() != null &&
                                                hookahConnection.getAttachedPlayerUUID().equals(player.getUUID()) &&
                                                hookahConnection.getHookahBlockPos() != null &&
                                                hookahConnection.getHookahBlockPos().equals(blockPos)
                    );

                    if (existingConnections.isEmpty()) {
                        player.sendSystemMessage(Component.literal("No hookah pipe is attached by you to this hookah."));
                        PacketDistributor.sendToPlayer(player, new PacketSyncHookahConnection(Optional.empty()));
                        LOGGER.debug("Server: No pipe attached by player {}.", player.getUUID());
                    } else {
                        HookahConnectionEntity connectionEntity = existingConnections.get(0);
                        connectionEntity.stopPuffing(hookahBlockEntity, player);
                        connectionEntity.discard();
                        player.sendSystemMessage(Component.literal("You detached the hookah pipe."));
                        PacketDistributor.sendToPlayer(player, new PacketSyncHookahConnection(Optional.empty()));
                        LOGGER.debug("Server: HookahConnectionEntity for player {} at {} discarded.", player.getUUID(), blockPos);
                    }
                } else {
                    player.sendSystemMessage(Component.literal("Error: Hookah Block Entity not found at " + blockPos));
                    PacketDistributor.sendToPlayer(player, new PacketSyncHookahConnection(Optional.empty()));
                    LOGGER.error("Server: Hookah Block Entity not found at {} when handling PacketDetachPipeToServer.", blockPos);
                }
            } else {
                player.sendSystemMessage(Component.literal("Error: Block not loaded at " + blockPos));
                PacketDistributor.sendToPlayer(player, new PacketSyncHookahConnection(Optional.empty()));
                LOGGER.error("Server: Block not loaded at {} when handling PacketDetachPipeToServer.", blockPos);
            }
        });
    }
}