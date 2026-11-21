package net.sorukoma.quasium.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.client.RingTiming;

public record PacketStopHookahPuffSoundClient() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<PacketStopHookahPuffSoundClient> ID =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "stop_hookah_puff_sound_client"));

    public static final StreamCodec<FriendlyByteBuf, PacketStopHookahPuffSoundClient> STREAM_CODEC =
            StreamCodec.unit(new PacketStopHookahPuffSoundClient());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static void handle(PacketStopHookahPuffSoundClient payload, IPayloadContext context) {
        context.enqueueWork(RingTiming::stoppedByServer);
    }
}
