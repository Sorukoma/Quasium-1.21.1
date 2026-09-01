package net.sorukoma.quasium.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.client.ClientTobaccoData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public record TobaccoDataSyncPacket(Set<String> triedFlavors, Set<Float> triedSelfMixedStrengths,
                                     Map<String, Boolean> flavorPreferences) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<TobaccoDataSyncPacket> ID =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "tobacco_data_sync"));
    public static final StreamCodec<FriendlyByteBuf, TobaccoDataSyncPacket> STREAM_CODEC =
            StreamCodec.of(TobaccoDataSyncPacket::encode, TobaccoDataSyncPacket::decode);

    public static TobaccoDataSyncPacket decode(FriendlyByteBuf buf) {
        int flavorCount = buf.readVarInt();
        Set<String> flavors = new HashSet<>();
        for (int i = 0; i < flavorCount; i++) flavors.add(buf.readUtf());
        int strengthCount = buf.readVarInt();
        Set<Float> strengths = new HashSet<>();
        for (int i = 0; i < strengthCount; i++) strengths.add(buf.readFloat());
        int preferenceCount = buf.readVarInt();
        Map<String, Boolean> preferences = new HashMap<>();
        for (int i = 0; i < preferenceCount; i++) preferences.put(buf.readUtf(), buf.readBoolean());
        return new TobaccoDataSyncPacket(flavors, strengths, preferences);
    }

    public static void encode(FriendlyByteBuf buf, TobaccoDataSyncPacket packet) {
        buf.writeVarInt(packet.triedFlavors().size());
        for (String flavor : packet.triedFlavors()) buf.writeUtf(flavor);
        buf.writeVarInt(packet.triedSelfMixedStrengths().size());
        for (Float strength : packet.triedSelfMixedStrengths()) buf.writeFloat(strength);
        buf.writeVarInt(packet.flavorPreferences().size());
        for (Map.Entry<String, Boolean> entry : packet.flavorPreferences().entrySet()) {
            buf.writeUtf(entry.getKey());
            buf.writeBoolean(entry.getValue());
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static void handle(TobaccoDataSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.flow().isClientbound()) handleClient(packet);
        });
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(TobaccoDataSyncPacket packet) {
        ClientTobaccoData.setTriedFlavors(packet.triedFlavors());
        ClientTobaccoData.setTriedSelfMixedStrengths(packet.triedSelfMixedStrengths());
        ClientTobaccoData.setFlavorPreferences(packet.flavorPreferences());
    }
}
