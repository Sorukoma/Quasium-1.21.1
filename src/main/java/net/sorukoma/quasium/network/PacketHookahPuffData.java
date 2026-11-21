package net.sorukoma.quasium.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.component.ModDataComponents;
import org.jetbrains.annotations.Nullable;

public record PacketHookahPuffData(BlockPos blockPos, float totalHeat,
                                   int totalTobaccoWeight, int warmup, @Nullable ModDataComponents.HookahBowlContents bowlContents) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PacketHookahPuffData> ID =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "hookah_puff_data"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketHookahPuffData> STREAM_CODEC = StreamCodec.of(
            PacketHookahPuffData::encode, PacketHookahPuffData::decode
    );

    private static void encode(RegistryFriendlyByteBuf buf, PacketHookahPuffData pkt) {
        buf.writeBlockPos(pkt.blockPos);
        buf.writeFloat(pkt.totalHeat);
        buf.writeInt(pkt.totalTobaccoWeight);
        buf.writeVarInt(pkt.warmup);
        buf.writeBoolean(pkt.bowlContents != null);
        if (pkt.bowlContents != null) {
            ModDataComponents.HookahBowlContents.STREAM_CODEC.encode(buf, pkt.bowlContents);
        }
    }

    private static PacketHookahPuffData decode(RegistryFriendlyByteBuf buf) {
        BlockPos blockPos = buf.readBlockPos();
        float totalHeat = buf.readFloat();
        int totalTobaccoWeight = buf.readInt();
        int warmup = buf.readVarInt();
        boolean hasContents = buf.readBoolean();
        ModDataComponents.HookahBowlContents bowlContents = hasContents ? ModDataComponents.HookahBowlContents.STREAM_CODEC.decode(buf) : null;
        return new PacketHookahPuffData(blockPos, totalHeat, totalTobaccoWeight, warmup, bowlContents);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
