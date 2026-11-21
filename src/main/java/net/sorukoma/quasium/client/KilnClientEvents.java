package net.sorukoma.quasium.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.kiln.KilnBlocks;
import net.sorukoma.quasium.screen.SmokeKilnScreen;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID, value = Dist.CLIENT)
public final class KilnClientEvents {
    private KilnClientEvents() {
    }

    @SubscribeEvent
    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(KilnBlocks.SMOKE_KILN_MENU.get(), SmokeKilnScreen::new);
    }
}
