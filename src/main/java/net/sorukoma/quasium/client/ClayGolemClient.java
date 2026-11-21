package net.sorukoma.quasium.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.client.renderer.entity.ClayGolemModel;
import net.sorukoma.quasium.client.renderer.entity.ClayGolemRenderer;
import net.sorukoma.quasium.golem.ClayGolems;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID, value = Dist.CLIENT)
public final class ClayGolemClient {
    private ClayGolemClient() {
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ClayGolemModel.LAYER, ClayGolemModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ClayGolems.CLAY_GOLEM.get(), ClayGolemRenderer::new);
    }
}
