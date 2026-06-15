package net.sorukoma.quasium.network;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.sorukoma.quasium.data.BackpackHookahState;
import net.sorukoma.quasium.item.ModItems;
import top.theillusivec4.curios.api.CuriosApi;

public class PacketDetachPipeBackpackHandler {
    public static void handle(final PacketDetachPipeBackpack payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            boolean hasBackpackHookah = CuriosApi.getCuriosHelper().findFirstCurio(player, ModItems.BACKPACK_HOOKAH.get()).isPresent();
            if (!hasBackpackHookah) {
                PacketDistributor.sendToPlayer(player, new PacketSyncBackpackPipeConnection(false));
                return;
            }
            BackpackHookahState state = BackpackHookahState.getOrCreate(player);
            if (state.isPuffing()) {
                state.stopPuffing();
            }
            PacketDistributor.sendToPlayer(player, new PacketSyncBackpackPipeConnection(false));
            player.sendSystemMessage(Component.translatable("quasium.message.pipe_detached_backpack"));
        });
    }
}