package net.sorukoma.quasium.magic.page.spirit.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.magic.page.spirit.SpiritRegistry;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID, value = Dist.CLIENT)
public final class SpiritClient {
    private SpiritClient() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(SpiritRegistry.HOOKAH_SPIRIT.get(), HookahSpiritRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(HookahSpiritModel.LAYER, HookahSpiritModel::createBodyLayer);
        event.registerLayerDefinition(SpiritFaces.LAYER, SpiritFaces::createFaceLayer);
    }
}
