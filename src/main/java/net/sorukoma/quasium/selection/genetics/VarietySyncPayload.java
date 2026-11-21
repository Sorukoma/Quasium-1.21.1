package net.sorukoma.quasium.selection.genetics;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.item.TobaccoVarieties;

import java.util.Map;

public record VarietySyncPayload(Map<String, VarietyDefinition> varieties) implements CustomPacketPayload {
    public static final Type<VarietySyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "tobacco_varieties"));
    public static final StreamCodec<ByteBuf, VarietySyncPayload> STREAM_CODEC =
            ByteBufCodecs.fromCodec(Codec.unboundedMap(Codec.STRING, VarietyDefinition.CODEC))
                    .map(VarietySyncPayload::new, VarietySyncPayload::varieties);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(VarietySyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> TobaccoVarieties.replace(payload.varieties()));
    }
}
