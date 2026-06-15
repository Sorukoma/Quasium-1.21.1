package net.sorukoma.quasium.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.sorukoma.quasium.QuasiumMod;

public record PacketStartPuffingBackpack() implements CustomPacketPayload {
    public static final Type<PacketStartPuffingBackpack> ID =
            new Type<>(ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "start_puffing_backpack"));
    public static final StreamCodec<FriendlyByteBuf, PacketStartPuffingBackpack> STREAM_CODEC =
            StreamCodec.unit(new PacketStartPuffingBackpack());

    @Override
    public Type<? extends CustomPacketPayload> type() { return ID; }
}