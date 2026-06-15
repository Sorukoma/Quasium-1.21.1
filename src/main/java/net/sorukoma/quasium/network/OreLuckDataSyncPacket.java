package net.sorukoma.quasium.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.client.ClientOreLuckData;

import java.util.HashSet;
import java.util.Set;

public record OreLuckDataSyncPacket(Set<Integer> achievedFortuneLevels) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OreLuckDataSyncPacket> ID =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "ore_luck_data_sync"));
    public static final StreamCodec<FriendlyByteBuf, OreLuckDataSyncPacket> STREAM_CODEC = StreamCodec.of(OreLuckDataSyncPacket::encode, OreLuckDataSyncPacket::decode);

    public static OreLuckDataSyncPacket decode(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        Set<Integer> levels = new HashSet<>();
        for (int i = 0; i < count; i++) {
            levels.add(buf.readVarInt());
        }
        return new OreLuckDataSyncPacket(levels);
    }

    public static void encode(FriendlyByteBuf buf, OreLuckDataSyncPacket packet) {
        buf.writeVarInt(packet.achievedFortuneLevels().size());
        for (Integer level : packet.achievedFortuneLevels()) {
            buf.writeVarInt(level);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static void handle(OreLuckDataSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.flow().isClientbound()) {
                handleClient(packet);
            }
        });
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(OreLuckDataSyncPacket packet) {
        ClientOreLuckData.setAchievedFortuneLevels(packet.achievedFortuneLevels());
    }
}