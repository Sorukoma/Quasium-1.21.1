package net.sorukoma.quasium.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.client.ClientMagicData;

import java.util.LinkedHashSet;
import java.util.Set;

public record ScanDataSyncPacket(Set<String> scanned, int insight) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ScanDataSyncPacket> ID =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "scan_data_sync"));
    public static final StreamCodec<FriendlyByteBuf, ScanDataSyncPacket> STREAM_CODEC =
            StreamCodec.of(ScanDataSyncPacket::encode, ScanDataSyncPacket::decode);

    private static void encode(FriendlyByteBuf buf, ScanDataSyncPacket packet) {
        buf.writeVarInt(packet.scanned.size());
        packet.scanned.forEach(buf::writeUtf);
        buf.writeVarInt(packet.insight);
    }

    private static ScanDataSyncPacket decode(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        Set<String> scanned = new LinkedHashSet<>();
        for (int i = 0; i < count; i++) scanned.add(buf.readUtf());
        return new ScanDataSyncPacket(scanned, buf.readVarInt());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static void handle(ScanDataSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.flow().isClientbound()) handleClient(packet);
        });
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(ScanDataSyncPacket packet) {
        ClientMagicData.setScanData(packet.scanned, packet.insight);
    }
}
