package net.sorukoma.quasium.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.sorukoma.quasium.block.entity.HookahBlockEntity;
import net.sorukoma.quasium.component.ModDataComponents;
import net.sorukoma.quasium.entity.HookahConnectionEntity;
import net.sorukoma.quasium.item.ModItems;
import net.minecraft.world.phys.AABB;
import java.util.List;

public class PacketStartPuffingServerHandler {

    private static final double MAX_DISTANCE_SQ = 64.0; // 8 блоков

    public static void handle(final PacketStartPuffing payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            BlockPos blockPos = payload.blockPos();

            // Валидация дистанции: не доверяем произвольной позиции от клиента.
            // Через SableCompat — иначе кальян на движущемся корабле (Create Aeronautics)
            // оказался бы «слишком далеко» из-за координат sub-level.
            if (net.sorukoma.quasium.compat.SableCompat.distanceSqrToBlockCenter(player, blockPos) > MAX_DISTANCE_SQ) {
                return;
            }

            // Проверяем кулдаун трубки — если активен, не начинаем затяжку
            ItemStack pipeItem = player.getMainHandItem().is(ModItems.HOOKAH_PIPE.get()) ?
                                 player.getMainHandItem() :
                                 player.getOffhandItem();
            if (!pipeItem.is(ModItems.HOOKAH_PIPE.get())) {
                return;
            }
            Long cooldownEnd = pipeItem.get(ModDataComponents.HOOKAH_PIPE_COOLDOWN_END.get());
            if (cooldownEnd != null && player.level().getGameTime() < cooldownEnd) {
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

                    if (!connections.isEmpty()) {
                        HookahConnectionEntity connectionEntity = connections.get(0);
                        connectionEntity.startPuffing(hookahBlockEntity);
                    } else {
                        player.connection.send(new ClientboundSetActionBarTextPacket(
                                Component.translatable("quasium.message.not_connected")));
                    }
                }
            }
        });
    }
}
