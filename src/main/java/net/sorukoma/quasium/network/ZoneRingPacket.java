package net.sorukoma.quasium.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.client.ZoneRings;

public record ZoneRingPacket(BlockPos hookah, int radius, int color) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ZoneRingPacket> ID =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "zone_ring"));
    public static final StreamCodec<ByteBuf, ZoneRingPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ZoneRingPacket::hookah,
            ByteBufCodecs.VAR_INT, ZoneRingPacket::radius,
            ByteBufCodecs.INT, ZoneRingPacket::color,
            ZoneRingPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static void handle(ZoneRingPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.flow().isClientbound()) handleClient(packet);
        });
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(ZoneRingPacket packet) {
        ZoneRings.start(packet.hookah(), packet.radius(), packet.color());
    }
}
