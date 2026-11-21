package net.sorukoma.quasium.hint;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.magic.CodecDataLoader;

import java.util.Map;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID)
public final class HintEvents {
    private static volatile Map<ResourceLocation, HintScene> scenes = Map.of();

    private HintEvents() {
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new CodecDataLoader<>("hints", HintScene.CODEC, loaded -> scenes = Map.copyOf(loaded)));
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        HintScenesPacket packet = new HintScenesPacket(scenes);
        event.getRelevantPlayers().forEach(player -> PacketDistributor.sendToPlayer(player, packet));
    }

    @SubscribeEvent
    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar(QuasiumMod.MOD_ID).playToClient(HintScenesPacket.TYPE, HintScenesPacket.STREAM_CODEC, HintScenesPacket::handle);
    }
}
