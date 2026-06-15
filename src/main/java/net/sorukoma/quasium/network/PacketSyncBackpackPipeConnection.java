package net.sorukoma.quasium.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.sorukoma.quasium.QuasiumMod;

public record PacketSyncBackpackPipeConnection(boolean connected) implements CustomPacketPayload {
    public static final Type<PacketSyncBackpackPipeConnection> ID =
            new Type<>(ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "sync_backpack_pipe_connection"));
    public static final StreamCodec<FriendlyByteBuf, PacketSyncBackpackPipeConnection> STREAM_CODEC =
            StreamCodec.of(
                    (buf, pkt) -> buf.writeBoolean(pkt.connected()),
                    buf -> new PacketSyncBackpackPipeConnection(buf.readBoolean())
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return ID; }
}