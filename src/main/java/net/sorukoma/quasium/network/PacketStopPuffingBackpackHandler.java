package net.sorukoma.quasium.network;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.sorukoma.quasium.data.BackpackHookahState;
import net.sorukoma.quasium.item.ModItems;
import top.theillusivec4.curios.api.CuriosApi;

public class PacketStopPuffingBackpackHandler {
    public static void handle(final PacketStopPuffingBackpack payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            boolean hasBackpackHookah = CuriosApi.getCuriosHelper().findFirstCurio(player, ModItems.BACKPACK_HOOKAH.get()).isPresent();
            if (!hasBackpackHookah) return;
            BackpackHookahState state = BackpackHookahState.getOrCreate(player);
            if (state.isPuffing()) {
                net.sorukoma.quasium.magic.SmokeCasting.release(player, state);
                state.stopPuffing();
            }
        });
    }
}