package net.sorukoma.quasium.network;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.sorukoma.quasium.block.ModularHookahBlock;
import net.sorukoma.quasium.block.entity.ModularHookahBlockEntity;

/**
 * Серверный обработчик shift+ПКМ по модульному кальяну. Ванильный конвейер не
 * вызывает Block.useItemOn при shift+предмет, поэтому клиент шлёт этот пакет,
 * а сервер запускает ту же логику сборки, что и {@link ModularHookahBlock#handleInteraction}.
 */
public class PacketModularHookahInteractHandler {
    private static final double MAX_DISTANCE_SQ = 64.0;

    public static void handle(final PacketModularHookahInteract payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            BlockPos pos = payload.blockPos();

            if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > MAX_DISTANCE_SQ) {
                return;
            }
            if (!player.level().isLoaded(pos)) return;
            if (!(player.level().getBlockEntity(pos) instanceof ModularHookahBlockEntity be)) return;

            InteractionHand hand = payload.mainHand() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            ItemStack stack = player.getItemInHand(hand);
            ModularHookahBlock.handleInteraction(be, stack, player.level(), pos, player, hand);
        });
    }
}
