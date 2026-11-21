package net.sorukoma.quasium.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.client.renderer.entity.CalloudGolemModel;
import net.sorukoma.quasium.client.renderer.entity.CalloudGolemRenderer;
import net.sorukoma.quasium.golem.CalloudGolems;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID, value = Dist.CLIENT)
public final class CalloudGolemClient {
    private CalloudGolemClient() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(CalloudGolems.CALLOUD_GOLEM.get(), CalloudGolemRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(CalloudGolemModel.LAYER, CalloudGolemModel::createBodyLayer);
    }
}
