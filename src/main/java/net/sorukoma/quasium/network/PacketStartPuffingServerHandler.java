package net.sorukoma.quasium.network;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.sorukoma.quasium.block.entity.HookahBlockEntity;
import net.sorukoma.quasium.entity.HookahConnectionEntity;
import net.sorukoma.quasium.hookah.HookahSignals;
import net.sorukoma.quasium.item.HookahPipeItem;
import net.minecraft.world.phys.AABB;
import java.util.List;

public class PacketStartPuffingServerHandler {

    private static final double MAX_DISTANCE_SQ = 64.0; // 8 блоков

    public static void handle(final PacketStartPuffing payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) start(player, payload.blockPos());
        });
    }

    public static void start(ServerPlayer player, BlockPos blockPos) {
        // Валидация дистанции: не доверяем произвольной позиции от клиента.
        // Через SableCompat, иначе кальян на движущемся корабле (Create Aeronautics)
        // оказался бы "слишком далеко" из-за координат sub-level.
        if (net.sorukoma.quasium.compat.SableCompat.distanceSqrToBlockCenter(player, blockPos) > MAX_DISTANCE_SQ) {
            return;
        }

        if (!HookahPipeItem.holdsPipe(player) || HookahPipeItem.coolingDown(player)) {
            return;
        }

        if (player.level().isLoaded(blockPos)) {
            BlockEntity blockEntity = player.level().getBlockEntity(blockPos);
            if (blockEntity instanceof HookahBlockEntity hookahBlockEntity) {
                AABB searchBox = new AABB(blockPos).inflate(HookahConnectionEntity.PIPE_DETACH_DISTANCE_SQ + 1.0);

                List<HookahConnectionEntity> connections = player.level().getEntitiesOfClass(
                        HookahConnectionEntity.class,
                        searchBox,
                        connection -> {
                            boolean isPlayerMatch = connection.getAttachedPlayerUUID() != null && connection.getAttachedPlayerUUID().equals(player.getUUID());
                            boolean isHookahMatch = connection.getHookahBlockPos() != null && connection.getHookahBlockPos().equals(blockPos);
                            return isPlayerMatch && isHookahMatch;
                        }
                );

                if (connections.isEmpty()) {
                    HookahSignals.refuse(player, "not_connected");
                    return;
                }
                String cold = hookahBlockEntity.coldReason();
                if (cold != null) {
                    HookahSignals.refuse(player, cold);
                    return;
                }
                connections.get(0).startPuffing(hookahBlockEntity);
            }
        }
    }
}
