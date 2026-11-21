package net.sorukoma.quasium.client;

import net.minecraft.client.renderer.entity.NoopRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.block.LoungeBlocks;
import net.sorukoma.quasium.client.renderer.blockentity.LowTableRenderer;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID, value = Dist.CLIENT)
public final class LoungeClient {
    private LoungeClient() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(LoungeBlocks.LOW_TABLE_BE.get(), LowTableRenderer::new);
        event.registerEntityRenderer(LoungeBlocks.SEAT.get(), NoopRenderer::new);
    }
}
