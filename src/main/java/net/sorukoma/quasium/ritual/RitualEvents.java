package net.sorukoma.quasium.ritual;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.configuration.ServerConfigurationPacketListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.network.ConfigurationTask;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.configuration.ICustomConfigurationTask;
import net.neoforged.neoforge.network.event.RegisterConfigurationTasksEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.magic.CodecDataLoader;
import net.sorukoma.quasium.ritual.armor.Smokestep;
import net.sorukoma.quasium.ritual.brazier.RitualPressPacket;

import java.util.function.Consumer;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID)
public final class RitualEvents {
    public static final String FOLDER = "ritual";

    private RitualEvents() {
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new CodecDataLoader<>(FOLDER, RitualRecipe.CODEC, RitualRecipes::set));
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        RitualRecipesPacket packet = RitualRecipesPacket.current();
        event.getRelevantPlayers().forEach(player -> PacketDistributor.sendToPlayer(player, packet));
    }

    @SubscribeEvent
    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").commonToClient(RitualRecipesPacket.TYPE, RitualRecipesPacket.STREAM_CODEC, RitualRecipesPacket::handle)
                .playToServer(Smokestep.DashPacket.TYPE, Smokestep.DashPacket.STREAM_CODEC, Smokestep.DashPacket::handle)
                .playToServer(RitualPressPacket.TYPE, RitualPressPacket.STREAM_CODEC, RitualPressPacket::handle)
                .playToClient(Smokestep.ReadyPacket.TYPE, Smokestep.ReadyPacket.STREAM_CODEC, Smokestep.ReadyPacket::handle);
    }

    @SubscribeEvent
    public static void onConfigurationTasks(RegisterConfigurationTasksEvent event) {
        event.register(new SyncTask(event.getListener()));
    }

    private record SyncTask(ServerConfigurationPacketListener listener) implements ICustomConfigurationTask {
        private static final ConfigurationTask.Type TYPE =
                new ConfigurationTask.Type(ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "ritual_recipes"));

        @Override
        public void run(Consumer<CustomPacketPayload> sender) {
            sender.accept(RitualRecipesPacket.current());
            listener.finishCurrentTask(TYPE);
        }

        @Override
        public ConfigurationTask.Type type() {
            return TYPE;
        }
    }
}
