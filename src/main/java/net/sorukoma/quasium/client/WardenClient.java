package net.sorukoma.quasium.client;

import net.minecraft.client.renderer.entity.NoopRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.client.renderer.entity.SmokeWardenModel;
import net.sorukoma.quasium.client.renderer.entity.SmokeWardenRenderer;
import net.sorukoma.quasium.golem.WardenGolems;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID, value = Dist.CLIENT)
public final class WardenClient {
    private WardenClient() {
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(SmokeWardenModel.LAYER, SmokeWardenModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(WardenGolems.SMOKE_WARDEN.get(), SmokeWardenRenderer::new);
        event.registerEntityRenderer(WardenGolems.WARDEN_SHOT.get(), NoopRenderer::new);
    }
}
