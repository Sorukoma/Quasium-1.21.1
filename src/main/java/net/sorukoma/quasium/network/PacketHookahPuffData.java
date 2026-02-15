package net.sorukoma.quasium.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.sorukoma.quasium.QuasiumMod;
import org.jetbrains.annotations.Nullable; // Импортируем Nullable
import java.util.UUID; // Импортируем UUID

public record PacketHookahPuffData(BlockPos blockPos, int puffsRemainingPotion, int puffsRemainingCharcoal, @Nullable UUID attachedPlayerUUID, @Nullable String attachedPlayerName) implements CustomPacketPayload {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "hookah_puff_data");
    public static final StreamCodec<FriendlyByteBuf, PacketHookahPuffData> STREAM_CODEC = CustomPacketPayload.codec(PacketHookahPuffData::write, PacketHookahPuffData::new);

    public PacketHookahPuffData(FriendlyByteBuf buffer) {
        this(buffer.readBlockPos(), buffer.readInt(), buffer.readInt(), buffer.readBoolean() ? buffer.readUUID() : null, buffer.readBoolean() ? buffer.readUtf() : null);
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(blockPos);
        buffer.writeInt(puffsRemainingPotion);
        buffer.writeInt(puffsRemainingCharcoal);
        buffer.writeBoolean(attachedPlayerUUID != null);
        if (attachedPlayerUUID != null) {
            buffer.writeUUID(attachedPlayerUUID);
        }
        buffer.writeBoolean(attachedPlayerName != null);
        if (attachedPlayerName != null) {
            buffer.writeUtf(attachedPlayerName);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return new Type<>(ID);
    }
}