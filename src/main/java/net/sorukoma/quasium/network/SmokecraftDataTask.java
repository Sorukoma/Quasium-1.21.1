package net.sorukoma.quasium.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.network.ConfigurationTask;
import net.minecraft.network.protocol.configuration.ServerConfigurationPacketListener;
import net.neoforged.neoforge.network.configuration.ICustomConfigurationTask;
import net.sorukoma.quasium.QuasiumMod;

import java.util.function.Consumer;

public record SmokecraftDataTask(ServerConfigurationPacketListener listener) implements ICustomConfigurationTask {
    public static final ConfigurationTask.Type TYPE =
            new ConfigurationTask.Type(ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "smokecraft_data"));

    @Override
    public void run(Consumer<CustomPacketPayload> sender) {
        sender.accept(SmokecraftDataPacket.current());
        listener.finishCurrentTask(TYPE);
    }

    @Override
    public ConfigurationTask.Type type() {
        return TYPE;
    }
}
