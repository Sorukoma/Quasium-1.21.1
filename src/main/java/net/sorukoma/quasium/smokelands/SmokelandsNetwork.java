package net.sorukoma.quasium.smokelands;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.smokelands.storm.StormStatePacket;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID)
public final class SmokelandsNetwork {
    private static volatile boolean storm;

    private SmokelandsNetwork() {
    }

    public static boolean stormSeenByClient() {
        return storm;
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(StormStatePacket.TYPE, StormStatePacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> storm = packet.raging()));
    }
}
