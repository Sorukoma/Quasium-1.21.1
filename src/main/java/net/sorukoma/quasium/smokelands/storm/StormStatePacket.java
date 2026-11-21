package net.sorukoma.quasium.smokelands.storm;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.sorukoma.quasium.QuasiumMod;

public record StormStatePacket(boolean raging) implements CustomPacketPayload {
    public static final Type<StormStatePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "cinder_storm"));
    public static final StreamCodec<ByteBuf, StormStatePacket> STREAM_CODEC =
            ByteBufCodecs.BOOL.map(StormStatePacket::new, StormStatePacket::raging);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
