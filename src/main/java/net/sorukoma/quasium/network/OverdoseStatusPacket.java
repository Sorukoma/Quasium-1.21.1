package net.sorukoma.quasium.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.client.ClientOverdoseData;

public record OverdoseStatusPacket(boolean isOverdosed) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OverdoseStatusPacket> ID =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "overdose_status"));

    public static final StreamCodec<FriendlyByteBuf, OverdoseStatusPacket> STREAM_CODEC = StreamCodec.of(
            OverdoseStatusPacket::encode,
            OverdoseStatusPacket::decode
    );

    public static OverdoseStatusPacket decode(FriendlyByteBuf buf) {
        return new OverdoseStatusPacket(buf.readBoolean());
    }

    public static void encode(FriendlyByteBuf buf, OverdoseStatusPacket packet) {
        buf.writeBoolean(packet.isOverdosed());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static void handle(OverdoseStatusPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.flow().isClientbound()) {
                handleClient(packet);
            }
        });
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(OverdoseStatusPacket packet) {
        ClientOverdoseData.setOverdosed(packet.isOverdosed());
    }
}
