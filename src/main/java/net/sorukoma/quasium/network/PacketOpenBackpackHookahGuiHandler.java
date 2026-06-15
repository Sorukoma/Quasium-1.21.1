package net.sorukoma.quasium.network;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.sorukoma.quasium.data.BackpackHookahState;
import net.sorukoma.quasium.item.ModItems;
import top.theillusivec4.curios.api.CuriosApi;

public class PacketOpenBackpackHookahGuiHandler {

    public static void handle(final PacketOpenBackpackHookahGui payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            boolean hasBackpackHookah = CuriosApi.getCuriosHelper().findFirstCurio(player, ModItems.BACKPACK_HOOKAH.get()).isPresent();
            if (!hasBackpackHookah) {
                player.sendSystemMessage(Component.translatable("quasium.message.no_backpack_hookah"));
                return;
            }

            BackpackHookahState state = BackpackHookahState.getOrCreate(player);
            player.openMenu(new BackpackHookahMenuProvider(state));
        });
    }
}