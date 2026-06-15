package net.sorukoma.quasium.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.sorukoma.quasium.QuasiumMod;

public record PacketOpenBackpackHookahGui() implements CustomPacketPayload {
    public static final Type<PacketOpenBackpackHookahGui> ID =
            new Type<>(ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "open_backpack_hookah_gui"));
    public static final StreamCodec<FriendlyByteBuf, PacketOpenBackpackHookahGui> STREAM_CODEC =
            StreamCodec.unit(new PacketOpenBackpackHookahGui());

    @Override
    public Type<? extends CustomPacketPayload> type() { return ID; }
}