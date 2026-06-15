package net.sorukoma.quasium.event;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.data.BackpackHookahState;
import net.sorukoma.quasium.item.ModItems;
import top.theillusivec4.curios.api.CuriosApi;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class ServerTickHandler {

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer() == null) {
            return;
        }

        // Tick backpack hookah state for all online players who have one
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            boolean hasBackpackHookah = CuriosApi.getCuriosHelper().findFirstCurio(player, ModItems.BACKPACK_HOOKAH.get()).isPresent();
            if (hasBackpackHookah) {
                BackpackHookahState state = BackpackHookahState.getOrCreate(player);
                state.tick();
                // Auto-save periodically (every 5 seconds = 100 ticks)
                if (event.getServer().getTickCount() % 100 == 0) {
                    state.saveToPlayer();
                }
            } else {
                // Player doesn't have backpack hookah - clean up state if exists
                BackpackHookahState existingState = BackpackHookahState.getState(player.getUUID());
                if (existingState != null) {
                    if (existingState.isPuffing()) {
                        existingState.stopPuffing();
                    }
                    existingState.drops();
                    existingState.resetState();
                    BackpackHookahState.removeState(player.getUUID());
                }
            }
        }
    }
}