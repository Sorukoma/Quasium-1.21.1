package net.sorukoma.quasium.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.WanderingTraderRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.selection.SelectionRegistry;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID, value = Dist.CLIENT)
public final class SmokeCaravanRenderer extends WanderingTraderRenderer {
    private static final ResourceLocation SKIN = ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "textures/entity/smoke_caravan.png");

    public SmokeCaravanRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(WanderingTrader trader) {
        return SKIN;
    }

    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(SelectionRegistry.SMOKE_CARAVAN.get(), SmokeCaravanRenderer::new);
    }
}
