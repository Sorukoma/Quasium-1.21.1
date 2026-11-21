package net.sorukoma.quasium.selection.genetics;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.client.ClientHybrids;

import java.util.List;

public record HybridCollectionPayload(List<String> keys) implements CustomPacketPayload {
    public static final Type<HybridCollectionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "hybrid_collection"));
    public static final StreamCodec<ByteBuf, HybridCollectionPayload> STREAM_CODEC =
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).map(HybridCollectionPayload::new, HybridCollectionPayload::keys);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(HybridCollectionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientHybrids.set(payload.keys()));
    }
}
