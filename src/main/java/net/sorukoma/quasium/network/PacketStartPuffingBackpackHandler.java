package net.sorukoma.quasium.network;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.sorukoma.quasium.data.BackpackHookahState;
import net.sorukoma.quasium.hookah.HookahSignals;
import net.sorukoma.quasium.item.HookahPipeItem;
import net.sorukoma.quasium.item.ModItems;
import top.theillusivec4.curios.api.CuriosApi;

public class PacketStartPuffingBackpackHandler {
    public static void handle(final PacketStartPuffingBackpack payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            boolean hasBackpackHookah = CuriosApi.getCuriosHelper().findFirstCurio(player, ModItems.BACKPACK_HOOKAH.get()).isPresent();
            if (!hasBackpackHookah) {
                player.sendSystemMessage(Component.translatable("quasium.message.no_backpack_hookah"));
                return;
            }
            if (!HookahPipeItem.holdsPipe(player) || HookahPipeItem.coolingDown(player)) {
                return;
            }
            BackpackHookahState state = BackpackHookahState.getOrCreate(player);
            if (state.isPuffing()) return;
            String cold = state.coldReason();
            if (cold != null) HookahSignals.refuse(player, cold);
            else state.startPuffing();
        });
    }
}