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

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "kaif_update");
    public static final StreamCodec<FriendlyByteBuf, KaifUpdatePacket> STREAM_CODEC = StreamCodec.of(KaifUpdatePacket::encode, KaifUpdatePacket::decode);

    public static KaifUpdatePacket decode(FriendlyByteBuf buf) {
        return new KaifUpdatePacket(buf.readInt());
    }

    public static void encode(FriendlyByteBuf buf, KaifUpdatePacket packet) {
        buf.writeInt(packet.kaifValue());
    }

    // Метод id() не является частью интерфейса CustomPacketPayload, но может быть полезен для других целей.
    // Если он вызывает ошибку, его можно удалить.
    public ResourceLocation id() {
        return ID;
    }

    @Override // Добавлена реализация метода type() из CustomPacketPayload
    public Type<? extends CustomPacketPayload> type() {
        return new Type<>(ID);
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
