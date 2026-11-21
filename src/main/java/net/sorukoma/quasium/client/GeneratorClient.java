package net.sorukoma.quasium.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.draft.GeneratorBlocks;
import net.sorukoma.quasium.screen.SmokeBoilerScreen;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID, value = Dist.CLIENT)
public final class GeneratorClient {
    private GeneratorClient() {
    }

    @SubscribeEvent
    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(GeneratorBlocks.SMOKE_BOILER_MENU.get(), SmokeBoilerScreen::new);
    }
}
