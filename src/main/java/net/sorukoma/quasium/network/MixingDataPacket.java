package net.sorukoma.quasium.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.configuration.ServerConfigurationPacketListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.network.ConfigurationTask;
import net.neoforged.neoforge.network.configuration.ICustomConfigurationTask;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.recipe.MixingRecipe;
import net.sorukoma.quasium.recipe.MixingRecipes;

import java.util.Map;
import java.util.function.Consumer;

public record MixingDataPacket(Map<ResourceLocation, MixingRecipe> recipes) implements CustomPacketPayload {
    public static final Type<MixingDataPacket> ID =
            new Type<>(ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "mixing_data"));
    public static final StreamCodec<ByteBuf, MixingDataPacket> STREAM_CODEC =
            ByteBufCodecs.fromCodec(MixingRecipes.MAP_CODEC).map(MixingDataPacket::new, MixingDataPacket::recipes);

    public static MixingDataPacket current() {
        return new MixingDataPacket(MixingRecipes.all());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static void handle(MixingDataPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> MixingRecipes.set(packet.recipes));
    }

    public record Task(ServerConfigurationPacketListener listener) implements ICustomConfigurationTask {
        public static final ConfigurationTask.Type TYPE =
                new ConfigurationTask.Type(ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "mixing_data"));

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
