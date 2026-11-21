package net.sorukoma.quasium.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.block.MixingBlocks;
import net.sorukoma.quasium.screen.MixingTableScreen;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID, value = Dist.CLIENT)
public final class MixingClientEvents {
    private MixingClientEvents() {
    }

    @SubscribeEvent
    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(MixingBlocks.MIXING_TABLE_MENU.get(), MixingTableScreen::new);
    }
}
