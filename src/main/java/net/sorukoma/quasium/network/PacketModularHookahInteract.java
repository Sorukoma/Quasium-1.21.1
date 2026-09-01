package net.sorukoma.quasium.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.sorukoma.quasium.QuasiumMod;

/**
 * Клиент → сервер: игрок ПКМ по модульному кальяну (сборка/вода/GUI).
 * Нужен потому, что при shift+ПКМ с предметом ванилла не вызывает Block.useItemOn,
 * поэтому серверную логику сборки запускаем явным пакетом (как у трубки).
 * mainHand=true — предмет в основной руке, иначе в offhand.
 */
public record PacketModularHookahInteract(BlockPos blockPos, boolean mainHand) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PacketModularHookahInteract> ID =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "modular_hookah_interact"));
    public static final StreamCodec<FriendlyByteBuf, PacketModularHookahInteract> STREAM_CODEC =
            CustomPacketPayload.codec(PacketModularHookahInteract::write, PacketModularHookahInteract::new);

    public PacketModularHookahInteract(FriendlyByteBuf buffer) {
        this(buffer.readBlockPos(), buffer.readBoolean());
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(blockPos);
        buffer.writeBoolean(mainHand);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
