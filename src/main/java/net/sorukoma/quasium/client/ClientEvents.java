package net.sorukoma.quasium.client;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.sorukoma.quasium.client.gui.ProgressionBookScreen;
import net.sorukoma.quasium.item.ModItems;
import net.sorukoma.quasium.network.PacketHookahPuffDataClientHandler;
import net.sorukoma.quasium.progression.ConfigArgs;

import java.util.Map;

@EventBusSubscriber(modid = "quasium", value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity().level().isClientSide() && event.getItemStack().getItem() == ModItems.PROGRESSION_BOOK.get()) {
            Minecraft.getInstance().setScreen(new ProgressionBookScreen());
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        OverdoseHallucinationHandler.clientTick();
        RecipeGate.tick();
        RingTiming.tick(Minecraft.getInstance());
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        OverdoseHallucinationHandler.renderPhantoms(event);
    }

    @SubscribeEvent
    public static void onClientLoggedOut(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        resetClientState();
    }

    @SubscribeEvent
    public static void onClientRespawn(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.Clone event) {
        // Срабатывает при респауне / смене измерения на клиенте
        ClientKaifData.setKaifValue(0);
        ClientHookahInputHandler.setConnectedHookahPos(null);
        ClientHookahInputHandler.setConnectedToBackpack(false);
        ClientOverdoseData.reset();
        RingTiming.forget();
    }

    private static void resetClientState() {
        ClientKaifData.setKaifValue(0);
        ClientHookahInputHandler.setConnectedHookahPos(null);
        ClientHookahInputHandler.setConnectedToBackpack(false);
        PacketHookahPuffDataClientHandler.clearAllData();
        net.sorukoma.quasium.network.PacketBackpackHookahPuffDataClientHandler.clearData();
        ClientOverdoseData.reset();
        RingTiming.forget();
        ConfigArgs.useServerValues(Map.of());
    }
}
