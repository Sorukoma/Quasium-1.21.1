package net.sorukoma.quasium.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.client.ClientMagicData;

import java.util.List;

public record OreGlowPacket(List<BlockPos> positions, int durationTicks) implements CustomPacketPayload {
    public static final int MAX_POSITIONS = 512;
    public static final CustomPacketPayload.Type<OreGlowPacket> ID =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "ore_glow"));
    public static final StreamCodec<ByteBuf, OreGlowPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_POSITIONS)), OreGlowPacket::positions,
            ByteBufCodecs.VAR_INT, OreGlowPacket::durationTicks,
            OreGlowPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static void handle(OreGlowPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.flow().isClientbound()) handleClient(packet);
        });
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(OreGlowPacket packet) {
        if (Minecraft.getInstance().level == null) return;
        ClientMagicData.setGlowingOres(packet.positions, Minecraft.getInstance().level.getGameTime() + packet.durationTicks);
    }
}
