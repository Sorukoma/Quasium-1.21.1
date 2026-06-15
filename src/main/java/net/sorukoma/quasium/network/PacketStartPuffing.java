package net.sorukoma.quasium.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.sorukoma.quasium.QuasiumMod;

public record PacketStartPuffing(BlockPos blockPos) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PacketStartPuffing> ID =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "start_puffing")); // Corrected type and initialization
    public static final StreamCodec<FriendlyByteBuf, PacketStartPuffing> STREAM_CODEC = CustomPacketPayload.codec(PacketStartPuffing::write, PacketStartPuffing::new);

    public PacketStartPuffing(FriendlyByteBuf buffer) {
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