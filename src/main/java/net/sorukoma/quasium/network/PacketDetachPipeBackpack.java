package net.sorukoma.quasium.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.sorukoma.quasium.QuasiumMod;

public record PacketDetachPipeBackpack() implements CustomPacketPayload {
    public static final Type<PacketDetachPipeBackpack> ID =
            new Type<>(ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "detach_pipe_backpack"));
    public static final StreamCodec<FriendlyByteBuf, PacketDetachPipeBackpack> STREAM_CODEC =
            StreamCodec.unit(new PacketDetachPipeBackpack());

    @Override
    public Type<? extends CustomPacketPayload> type() { return ID; }
}