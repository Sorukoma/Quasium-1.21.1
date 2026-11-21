package net.sorukoma.quasium.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.configuration.ServerConfigurationPacketListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ConfigurationTask;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.network.configuration.ICustomConfigurationTask;
import net.neoforged.neoforge.network.event.RegisterConfigurationTasksEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.progression.ConfigArgs;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID)
public record ConfigValuesPacket(Map<String, String> values) implements CustomPacketPayload {
    public static final Type<ConfigValuesPacket> ID =
            new Type<>(ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "config_values"));
    private static final int VALUE_CHARS = 1 << 18;
    public static final StreamCodec<ByteBuf, ConfigValuesPacket> STREAM_CODEC = ByteBufCodecs
            .<ByteBuf, String, String, Map<String, String>>map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.stringUtf8(VALUE_CHARS))
            .map(ConfigValuesPacket::new, ConfigValuesPacket::values);

    public static ConfigValuesPacket current() {
        return new ConfigValuesPacket(ConfigArgs.localValues());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static void handle(ConfigValuesPacket packet, IPayloadContext context) {
        boolean ownServer = context.connection().isMemoryConnection();
        context.enqueueWork(() -> ConfigArgs.useServerValues(ownServer ? Map.of() : packet.values));
    }

    @SubscribeEvent
    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").commonToClient(ID, STREAM_CODEC, ConfigValuesPacket::handle);
    }

    @SubscribeEvent
    public static void onConfigReloaded(ModConfigEvent.Reloading event) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || event.getConfig().getType() != ModConfig.Type.COMMON) return;
        server.execute(() -> server.getPlayerList().broadcastAll(new ClientboundCustomPayloadPacket(current())));
    }

    @SubscribeEvent
    public static void onRegisterConfigurationTasks(RegisterConfigurationTasksEvent event) {
        event.register(new Task(event.getListener()));
    }

    public record Task(ServerConfigurationPacketListener listener) implements ICustomConfigurationTask {
        public static final ConfigurationTask.Type TYPE =
                new ConfigurationTask.Type(ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "config_values"));

        @Override
        public void run(Consumer<CustomPacketPayload> sender) {
            sender.accept(current());
            listener.finishCurrentTask(TYPE);
        }

        @Override
        public ConfigurationTask.Type type() {
            return TYPE;
        }
    }
}
