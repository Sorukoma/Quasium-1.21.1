package net.sorukoma.quasium.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.sorukoma.quasium.QuasiumMod;

public record PacketStopPuffing(BlockPos blockPos) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PacketStopPuffing> ID =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "stop_puffing")); // Corrected type and initialization
    public static final StreamCodec<FriendlyByteBuf, PacketStopPuffing> STREAM_CODEC = CustomPacketPayload.codec(PacketStopPuffing::write, PacketStopPuffing::new);

    public PacketStopPuffing(FriendlyByteBuf buffer) {
        this(buffer.readBlockPos());
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(blockPos);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID; // Simply return the static ID
    }
}