package net.sorukoma.quasium.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.magic.SmokeCasting;

public record ExhalePacket(int ringOffsetMs) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ExhalePacket> ID =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "ring_exhale"));
    public static final StreamCodec<ByteBuf, ExhalePacket> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(ExhalePacket::new, ExhalePacket::ringOffsetMs);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static void handle(ExhalePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) SmokeCasting.markRing(player, packet.ringOffsetMs());
        });
    }
}
