package net.sorukoma.quasium.mind.client;

import net.minecraft.client.model.PhantomModel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.mind.MindRegistry;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID, value = Dist.CLIENT)
public final class MindClient {
    private MindClient() {
    }

    @SubscribeEvent
    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(MindRegistry.RESEARCH_TABLE_MENU.get(), ResearchTableScreen::new);
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(MindRegistry.SMOKE_PHANTOM.get(), SmokePhantomRenderer::new);
        event.registerEntityRenderer(MindRegistry.HOOKAH_GHOST.get(), HookahGhostRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(SmokePhantomRenderer.LAYER, PhantomModel::createBodyLayer);
    }
}
