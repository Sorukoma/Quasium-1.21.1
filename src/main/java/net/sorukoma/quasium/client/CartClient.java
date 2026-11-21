package net.sorukoma.quasium.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.cart.HookahCarts;
import net.sorukoma.quasium.client.renderer.entity.HookahMinecartRenderer;
import net.sorukoma.quasium.screen.CartHookahScreen;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID, value = Dist.CLIENT)
public final class CartClient {
    private static int connectedCart = -1;

    private CartClient() {
    }

    public static int connectedCart() {
        return connectedCart;
    }

    public static void setConnectedCart(int cartId) {
        connectedCart = cartId;
        if (cartId < 0) return;
        ClientHookahInputHandler.setConnectedHookahPos(null);
        ClientHookahInputHandler.setConnectedToBackpack(false);
    }

    @SubscribeEvent
    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(HookahCarts.HOOKAH_MINECART_MENU.get(), CartHookahScreen::new);
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(HookahCarts.HOOKAH_MINECART.get(), HookahMinecartRenderer::new);
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        connectedCart = -1;
    }
}
