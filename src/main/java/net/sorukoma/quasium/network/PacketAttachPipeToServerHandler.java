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
import net.sorukoma.quasium.data.BackpackHookahState;
import net.sorukoma.quasium.cart.HookahMinecart;

public class PacketAttachPipeToServerHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void handle(final PacketAttachPipeToServer payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) attach(player, payload.blockPos());
        });
    }

    public static void attach(ServerPlayer player, BlockPos blockPos) {
        // Distance validation (sub-level aware so hookahs on moving Sable contraptions still work)
        if (net.sorukoma.quasium.compat.SableCompat.distanceSqrToBlockCenter(player, blockPos) > HookahConnectionEntity.PIPE_DETACH_DISTANCE_SQ) {
            player.displayClientMessage(Component.translatable("quasium.message.pipe_too_far"), true);
            return;
        }

        boolean holdingPipe = player.getMainHandItem().is(net.sorukoma.quasium.item.ModItems.HOOKAH_PIPE.get())
                || player.getOffhandItem().is(net.sorukoma.quasium.item.ModItems.HOOKAH_PIPE.get());
        if (!holdingPipe) {
            return;
        }

        if (player.level().isLoaded(blockPos)) {
            BlockEntity blockEntity = player.level().getBlockEntity(blockPos);
            if (blockEntity instanceof HookahBlockEntity hookahBlockEntity) {

                // Detach from any other standing hookahs first
                List<HookahConnectionEntity> allPlayerConnections = player.level().getEntitiesOfClass(
                        HookahConnectionEntity.class,
                        new AABB(player.blockPosition()).inflate(64.0),
                        e -> e.getAttachedPlayerUUID() != null &&
                             e.getAttachedPlayerUUID().equals(player.getUUID())
                );
                for (HookahConnectionEntity oldConn : allPlayerConnections) {
                    if (blockPos.equals(oldConn.getHookahBlockPos())) continue;
                    BlockEntity oldBe = player.level().getBlockEntity(oldConn.getHookahBlockPos());
                    if (oldBe instanceof HookahBlockEntity oldHookahBe) {
                        oldConn.stopPuffing(oldHookahBe, player);
                    }
                    oldConn.discard();
                }

                // Detach from backpack hookah if connected
                BackpackHookahState backpackState = BackpackHookahState.getState(player.getUUID());
                if (backpackState != null && backpackState.isPuffing()) {
                    backpackState.stopPuffing();
                    PacketDistributor.sendToPlayer(player, new PacketSyncBackpackPipeConnection(false));
                }
                HookahMinecart.unhookAll(player);

                // Check if already attached to THIS specific hookah
                List<HookahConnectionEntity> existingConnections = player.level().getEntitiesOfClass(
                        HookahConnectionEntity.class,
                        new AABB(blockPos).inflate(HookahConnectionEntity.PIPE_DETACH_DISTANCE_SQ + 1.0),
                        hookahConnection -> hookahConnection.getAttachedPlayerUUID() != null &&
                                            hookahConnection.getAttachedPlayerUUID().equals(player.getUUID()) &&
                                            hookahConnection.getHookahBlockPos() != null &&
                                            hookahConnection.getHookahBlockPos().equals(blockPos)
                );

                if (existingConnections.isEmpty()) {
                    HookahConnectionEntity connectionEntity = new HookahConnectionEntity(player.level(), player.getUUID(), blockPos);
                    player.level().addFreshEntity(connectionEntity);
                    player.sendSystemMessage(Component.translatable("quasium.message.pipe_attached"));
                    PacketDistributor.sendToPlayer(player, new PacketSyncHookahConnection(Optional.of(blockPos)));
                } else {
                    player.sendSystemMessage(Component.translatable("quasium.message.pipe_already_attached"));
                    PacketDistributor.sendToPlayer(player, new PacketSyncHookahConnection(Optional.of(blockPos)));
                }
            } else {
                player.sendSystemMessage(Component.translatable("quasium.message.hookah_missing"));
                PacketDistributor.sendToPlayer(player, new PacketSyncHookahConnection(Optional.empty()));
                LOGGER.debug("Server: Hookah Block Entity not found at {} when handling PacketAttachPipeToServer.", blockPos);
            }
        } else {
            player.sendSystemMessage(Component.translatable("quasium.message.hookah_not_loaded"));
            PacketDistributor.sendToPlayer(player, new PacketSyncHookahConnection(Optional.empty()));
            LOGGER.debug("Server: Block not loaded at {} when handling PacketAttachPipeToServer.", blockPos);
        }
    }
}