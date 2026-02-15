package net.sorukoma.quasium.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.sorukoma.quasium.QuasiumMod;

public record PacketAttachPipe(BlockPos blockPos) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PacketAttachPipe> ID = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "attach_pipe"));

    // Исправленная реализация STREAM_CODEC
    public static final StreamCodec<FriendlyByteBuf, PacketAttachPipe> STREAM_CODEC = CustomPacketPayload.codec(PacketAttachPipe::write, PacketAttachPipe::read);

    // Метод для записи данных пакета в буфер
    private void write(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(this.blockPos);
    }

    // Метод для чтения данных пакета из буфера
    private static PacketAttachPipe read(FriendlyByteBuf buffer) {
        return new PacketAttachPipe(buffer.readBlockPos());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
