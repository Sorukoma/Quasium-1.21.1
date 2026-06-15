package net.sorukoma.quasium.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.sorukoma.quasium.QuasiumMod;

public record PacketAttachPipeBackpack() implements CustomPacketPayload {
    public static final Type<PacketAttachPipeBackpack> ID =
            new Type<>(ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "attach_pipe_backpack"));
    public static final StreamCodec<FriendlyByteBuf, PacketAttachPipeBackpack> STREAM_CODEC =
            StreamCodec.unit(new PacketAttachPipeBackpack());

    @Override
    public Type<? extends CustomPacketPayload> type() { return ID; }
}