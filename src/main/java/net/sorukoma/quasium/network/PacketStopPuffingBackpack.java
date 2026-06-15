package net.sorukoma.quasium.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.sorukoma.quasium.QuasiumMod;

public record PacketStopPuffingBackpack() implements CustomPacketPayload {
    public static final Type<PacketStopPuffingBackpack> ID =
            new Type<>(ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "stop_puffing_backpack"));
    public static final StreamCodec<FriendlyByteBuf, PacketStopPuffingBackpack> STREAM_CODEC =
            StreamCodec.unit(new PacketStopPuffingBackpack());

    @Override
    public Type<? extends CustomPacketPayload> type() { return ID; }
}