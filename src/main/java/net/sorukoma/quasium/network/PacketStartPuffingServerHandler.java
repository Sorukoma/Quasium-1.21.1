package net.sorukoma.quasium.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.sorukoma.quasium.block.entity.HookahBlockEntity;

public class PacketStartPuffingServerHandler {
    public static void handle(final PacketStartPuffing payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            BlockPos blockPos = payload.blockPos();

            if (player.level().isLoaded(blockPos)) {
                BlockEntity blockEntity = player.level().getBlockEntity(blockPos);
                if (blockEntity instanceof HookahBlockEntity hookahBlockEntity) {
                    hookahBlockEntity.startPuffing(player.getUUID());
                } else {
                    player.sendSystemMessage(Component.literal("Error: Hookah Block Entity not found at " + blockPos));
                }
            } else {
                player.sendSystemMessage(Component.literal("Error: Block not loaded at " + blockPos));
            }
        });
    }
}
