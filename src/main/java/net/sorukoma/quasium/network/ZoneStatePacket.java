package net.sorukoma.quasium.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.client.ClientZoneData;

public record ZoneStatePacket(int tier, int color) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ZoneStatePacket> ID =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "zone_state"));
    public static final StreamCodec<ByteBuf, ZoneStatePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ZoneStatePacket::tier,
            ByteBufCodecs.INT, ZoneStatePacket::color,
            ZoneStatePacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static void handle(ZoneStatePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.flow().isClientbound()) handleClient(packet);
        });
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(ZoneStatePacket packet) {
        ClientZoneData.update(packet.tier(), packet.color());
    }
}
