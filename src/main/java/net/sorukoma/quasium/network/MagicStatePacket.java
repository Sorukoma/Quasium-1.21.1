package net.sorukoma.quasium.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.client.ClientMagicData;

public record MagicStatePacket(float lungs, int capacity, String spell, int cost, float charge,
                               int holdTicks, int coughTicks, String label, String missing) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<MagicStatePacket> ID =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "magic_state"));
    public static final StreamCodec<FriendlyByteBuf, MagicStatePacket> STREAM_CODEC =
            StreamCodec.of(MagicStatePacket::encode, MagicStatePacket::decode);

    private static void encode(FriendlyByteBuf buf, MagicStatePacket packet) {
        buf.writeFloat(packet.lungs);
        buf.writeVarInt(packet.capacity);
        buf.writeUtf(packet.spell);
        buf.writeVarInt(packet.cost);
        buf.writeFloat(packet.charge);
        buf.writeVarInt(packet.holdTicks);
        buf.writeVarInt(packet.coughTicks);
        buf.writeUtf(packet.label);
        buf.writeUtf(packet.missing);
    }

    private static MagicStatePacket decode(FriendlyByteBuf buf) {
        return new MagicStatePacket(buf.readFloat(), buf.readVarInt(), buf.readUtf(), buf.readVarInt(),
                buf.readFloat(), buf.readVarInt(), buf.readVarInt(), buf.readUtf(), buf.readUtf());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static void handle(MagicStatePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.flow().isClientbound()) handleClient(packet);
        });
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(MagicStatePacket packet) {
        ClientMagicData.setCastState(packet.lungs, packet.capacity,
                packet.spell.isEmpty() ? null : ResourceLocation.tryParse(packet.spell),
                packet.cost, packet.charge, packet.holdTicks, packet.coughTicks);
        ClientMagicData.setPageState(packet.label, packet.missing);
    }
}
