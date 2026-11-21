package net.sorukoma.quasium.ecology;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.ecology.client.ClientSmog;

public record ChunkSmogPacket(long chunk, int density, int smog) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ChunkSmogPacket> ID =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "chunk_smog"));
    public static final StreamCodec<ByteBuf, ChunkSmogPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, ChunkSmogPacket::chunk,
            ByteBufCodecs.VAR_INT, ChunkSmogPacket::density,
            ByteBufCodecs.VAR_INT, ChunkSmogPacket::smog,
            ChunkSmogPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static void handle(ChunkSmogPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.flow().isClientbound()) handleClient(packet);
        });
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(ChunkSmogPacket packet) {
        ClientSmog.update(packet.chunk(), packet.density(), packet.smog());
    }
}
