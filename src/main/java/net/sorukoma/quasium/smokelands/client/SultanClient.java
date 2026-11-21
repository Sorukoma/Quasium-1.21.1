package net.sorukoma.quasium.smokelands.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.sounds.Music;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.SelectMusicEvent;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.smokelands.SmokelandsDimension;
import net.sorukoma.quasium.smokelands.SmokelandsSounds;
import net.sorukoma.quasium.smokelands.sultan.Sultans;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID, value = Dist.CLIENT)
public final class SultanClient {
    private static Music theme;

    private SultanClient() {
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(SultanModel.LAYER, SultanModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(Sultans.SULTAN.get(), SultanRenderer::new);
        event.registerEntityRenderer(Sultans.RING.get(), NoopRenderer::new);
    }

    @SubscribeEvent
    public static void onSelectMusic(SelectMusicEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !SmokelandsDimension.contains(minecraft.level) || !minecraft.gui.getBossOverlay().shouldPlayMusic()) return;
        if (theme == null) theme = new Music(SmokelandsSounds.SULTAN_THEME, 0, 0, true);
        event.overrideMusic(theme);
    }
}
