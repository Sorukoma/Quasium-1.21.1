package net.sorukoma.quasium.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.sorukoma.quasium.QuasiumMod;

public record PacketPuffDuration(BlockPos blockPos, int durationHeldTicks) implements CustomPacketPayload {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "puff_duration");
    public static final StreamCodec<FriendlyByteBuf, PacketPuffDuration> STREAM_CODEC = CustomPacketPayload.codec(PacketPuffDuration::write, PacketPuffDuration::new);

    public PacketPuffDuration(FriendlyByteBuf buffer) {
        this(buffer.readBlockPos(), buffer.readInt());
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(blockPos);
        buffer.writeInt(durationHeldTicks);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return new Type<>(ID);
    }
}
