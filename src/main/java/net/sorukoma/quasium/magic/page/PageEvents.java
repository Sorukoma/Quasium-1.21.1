package net.sorukoma.quasium.magic.page;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.magic.CodecDataLoader;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID)
public final class PageEvents {
    private PageEvents() {
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new CodecDataLoader<>("page_form", PageForm.CODEC, PageCatalog::setForms));
        event.addListener(new CodecDataLoader<>("page_effect", PageEffect.CODEC, PageCatalog::setEffects));
        event.addListener(new CodecDataLoader<>("page_top", PageTop.CODEC, PageCatalog::setTops));
        event.addListener(new CodecDataLoader<>("accord", Accord.CODEC, PageCatalog::setAccords));
        event.addListener(new CodecDataLoader<>("essence_notes", EssenceNotes.CODEC, PageCatalog::setEssences));
    }

    @SubscribeEvent
    public static void onHeal(LivingHealEvent event) {
        if (event.getEntity().hasEffect(PageRegistry.CHOKING)) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        PageCasting.forget(event.getEntity().getUUID());
    }
}
