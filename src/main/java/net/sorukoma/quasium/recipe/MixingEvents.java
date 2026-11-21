package net.sorukoma.quasium.recipe;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterConfigurationTasksEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.magic.CodecDataLoader;
import net.sorukoma.quasium.network.MixingDataPacket;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID)
public final class MixingEvents {
    private MixingEvents() {
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new CodecDataLoader<>("mixing", MixingRecipe.CODEC, MixingRecipes::set));
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        MixingDataPacket packet = MixingDataPacket.current();
        event.getRelevantPlayers().forEach(player -> PacketDistributor.sendToPlayer(player, packet));
    }

    @SubscribeEvent
    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").commonToClient(MixingDataPacket.ID, MixingDataPacket.STREAM_CODEC, MixingDataPacket::handle);
    }

    @SubscribeEvent
    public static void onRegisterConfigurationTasks(RegisterConfigurationTasksEvent event) {
        event.register(new MixingDataPacket.Task(event.getListener()));
    }
}
