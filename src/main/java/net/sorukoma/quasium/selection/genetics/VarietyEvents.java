package net.sorukoma.quasium.selection.genetics;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.item.TobaccoVarieties;
import net.sorukoma.quasium.magic.CodecDataLoader;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID)
public final class VarietyEvents {
    private VarietyEvents() {
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new CodecDataLoader<>("tobacco_variety", VarietyDefinition.CODEC, TobaccoVarieties::load));
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        VarietySyncPayload payload = new VarietySyncPayload(TobaccoVarieties.all());
        event.getRelevantPlayers().forEach(player -> PacketDistributor.sendToPlayer(player, payload));
    }

    @SubscribeEvent
    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(QuasiumMod.MOD_ID);
        registrar.playToClient(VarietySyncPayload.TYPE, VarietySyncPayload.STREAM_CODEC, VarietySyncPayload::handle);
        registrar.playToClient(HybridCollectionPayload.TYPE, HybridCollectionPayload.STREAM_CODEC, HybridCollectionPayload::handle);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) HybridCollection.sync(player);
    }
}
