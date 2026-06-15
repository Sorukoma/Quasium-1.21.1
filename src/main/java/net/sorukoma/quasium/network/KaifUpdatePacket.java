package net.sorukoma.quasium.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.client.ClientKaifData;

public record KaifUpdatePacket(int kaifValue) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<KaifUpdatePacket> ID =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "kaif_update")); // Corrected type and initialization
    public static final StreamCodec<FriendlyByteBuf, KaifUpdatePacket> STREAM_CODEC = StreamCodec.of(KaifUpdatePacket::encode, KaifUpdatePacket::decode);

    public static KaifUpdatePacket decode(FriendlyByteBuf buf) {
        return new KaifUpdatePacket(buf.readInt());
    }

    public static void encode(FriendlyByteBuf buf, KaifUpdatePacket packet) {
        buf.writeInt(packet.kaifValue());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID; // Simply return the static ID
    }

    public static void handle(KaifUpdatePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            // Убедимся, что это клиентская сторона
            if (context.flow().isClientbound()) {
                handleClient(packet);
            }
        });
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(KaifUpdatePacket packet) {
        ClientKaifData.setKaifValue(packet.kaifValue());
    }
}