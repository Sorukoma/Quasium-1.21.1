package net.sorukoma.quasium.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.sorukoma.quasium.QuasiumMod;

public record PacketAttachPipeToServer(BlockPos blockPos) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PacketAttachPipeToServer> ID =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "attach_pipe_to_server"));
    public static final StreamCodec<FriendlyByteBuf, PacketAttachPipeToServer> STREAM_CODEC = CustomPacketPayload.codec(PacketAttachPipeToServer::write, PacketAttachPipeToServer::new);

    public PacketAttachPipeToServer(FriendlyByteBuf buffer) {
        this(buffer.readBlockPos());
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(blockPos);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}