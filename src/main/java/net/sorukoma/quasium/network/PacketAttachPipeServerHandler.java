package net.sorukoma.quasium.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.sorukoma.quasium.block.entity.HookahBlockEntity;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public class PacketAttachPipeServerHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void handle(final PacketAttachPipe payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            BlockPos blockPos = payload.blockPos();

            LOGGER.debug("Server: Received PacketAttachPipe from player {} for block at {}", player.getName().getString(), blockPos);

            if (player.level().isLoaded(blockPos)) {
                BlockEntity blockEntity = player.level().getBlockEntity(blockPos);
                if (blockEntity instanceof HookahBlockEntity hookahBlockEntity) {
                    if (!hookahBlockEntity.isPipeAttached()) {
                        hookahBlockEntity.attachPipe(player.getUUID(), player.getName().getString());
                        player.sendSystemMessage(Component.literal("Hookah pipe attached successfully!"));
                        LOGGER.debug("Server: Hookah pipe attached by {} to block at {}", player.getName().getString(), blockPos);
                    } else {
                        player.sendSystemMessage(Component.literal("This hookah already has a pipe attached."));
                        LOGGER.warn("Server: Attempted to attach pipe to already attached hookah at {} by {}", blockPos, player.getName().getString());
                    }
                } else {
                    player.sendSystemMessage(Component.literal("Error: Hookah Block Entity not found at " + blockPos));
                    LOGGER.error("Server: Hookah Block Entity not found at {} when handling PacketAttachPipe.", blockPos);
                }
            } else {
                player.sendSystemMessage(Component.literal("Error: Block not loaded at " + blockPos));
                LOGGER.error("Server: Block not loaded at {} when handling PacketAttachPipe.", blockPos);
            }
        });
    }
}
