package net.sorukoma.quasium.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.component.ModDataComponents;

import javax.annotation.Nullable;

public record PacketBackpackHookahPuffData(
        float totalHeat,
        int totalTobaccoWeight,
        @Nullable ModDataComponents.HookahBowlContents bowlContents
) implements CustomPacketPayload {
    public static final Type<PacketBackpackHookahPuffData> ID =
            new Type<>(ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "backpack_hookah_puff_data"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PacketBackpackHookahPuffData> STREAM_CODEC = StreamCodec.of(
            PacketBackpackHookahPuffData::encode,
            PacketBackpackHookahPuffData::decode
    );

    private static void encode(RegistryFriendlyByteBuf buf, PacketBackpackHookahPuffData pkt) {
        buf.writeFloat(pkt.totalHeat);
        buf.writeVarInt(pkt.totalTobaccoWeight);
        buf.writeBoolean(pkt.bowlContents != null);
        if (pkt.bowlContents != null) {
            ModDataComponents.HookahBowlContents.STREAM_CODEC.encode(buf, pkt.bowlContents);
        }
    }

    private static PacketBackpackHookahPuffData decode(RegistryFriendlyByteBuf buf) {
        float totalHeat = buf.readFloat();
        int totalTobaccoWeight = buf.readVarInt();
        boolean hasContents = buf.readBoolean();
        ModDataComponents.HookahBowlContents bowlContents = hasContents ? ModDataComponents.HookahBowlContents.STREAM_CODEC.decode(buf) : null;
        return new PacketBackpackHookahPuffData(totalHeat, totalTobaccoWeight, bowlContents);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return ID; }
}
