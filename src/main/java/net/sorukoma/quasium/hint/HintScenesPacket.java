package net.sorukoma.quasium.hint;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.client.hint.Hints;

import java.util.Map;

public record HintScenesPacket(Map<ResourceLocation, HintScene> scenes) implements CustomPacketPayload {
    public static final Type<HintScenesPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "hint_scenes"));
    public static final StreamCodec<ByteBuf, HintScenesPacket> STREAM_CODEC =
            ByteBufCodecs.fromCodec(HintScene.MAP_CODEC).map(HintScenesPacket::new, HintScenesPacket::scenes);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(HintScenesPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> Hints.set(packet.scenes()));
    }
}
