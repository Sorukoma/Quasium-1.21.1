package net.sorukoma.quasium.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.client.ClientPipeFlow;

import java.util.List;

public record PipeFlowPacket(List<BlockPos> pipes, int color, float load) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PipeFlowPacket> ID =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "pipe_flow"));
    public static final StreamCodec<ByteBuf, PipeFlowPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list(512)), PipeFlowPacket::pipes,
            ByteBufCodecs.INT, PipeFlowPacket::color,
            ByteBufCodecs.FLOAT, PipeFlowPacket::load,
            PipeFlowPacket::new);

    public PipeFlowPacket {
        pipes = pipes.size() > 512 ? List.copyOf(pipes.subList(0, 512)) : List.copyOf(pipes);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static void handle(PipeFlowPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.flow().isClientbound()) handleClient(packet);
        });
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(PipeFlowPacket packet) {
        ClientPipeFlow.update(packet.pipes(), packet.color(), packet.load());
    }
}
