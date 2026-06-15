package net.sorukoma.quasium.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.sorukoma.quasium.QuasiumMod;

import java.util.Optional;

public record PacketSyncHookahConnection(Optional<BlockPos> connectedHookahPos) implements CustomPacketPayload {
    public static final Type<PacketSyncHookahConnection> ID =
            new Type<>(ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "sync_hookah_connection"));

    public static final StreamCodec<FriendlyByteBuf, PacketSyncHookahConnection> STREAM_CODEC = CustomPacketPayload.codec(PacketSyncHookahConnection::write, PacketSyncHookahConnection::new);

    public PacketSyncHookahConnection(FriendlyByteBuf buffer) {
        this(buffer.readOptional(buf -> buf.readBlockPos()));
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeOptional(connectedHookahPos, (buf, pos) -> buf.writeBlockPos(pos));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
