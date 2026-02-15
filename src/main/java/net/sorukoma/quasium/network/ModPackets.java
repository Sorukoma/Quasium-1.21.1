package net.sorukoma.quasium.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.sorukoma.quasium.QuasiumMod;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import com.mojang.logging.LogUtils; // Import LogUtils
import org.slf4j.Logger; // Import Logger

public class ModPackets {
    private static final Logger LOGGER = LogUtils.getLogger(); // Initialize Logger

    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(QuasiumMod.MOD_ID);

        // Регистрируем наш пакет KaifUpdatePacket
        registrar.playToClient(new CustomPacketPayload.Type<>(KaifUpdatePacket.ID), KaifUpdatePacket.STREAM_CODEC, KaifUpdatePacket::handle);
        // Регистрируем наш пакет PacketSpawnSmoke
        registrar.playToClient(new CustomPacketPayload.Type<>(PacketSpawnSmoke.ID), PacketSpawnSmoke.STREAM_CODEC, PacketSpawnSmokeClientHandler::handle);
        // Регистрируем наш пакет PacketHookahPuffData
        registrar.playToClient(new CustomPacketPayload.Type<>(PacketHookahPuffData.ID), PacketHookahPuffData.STREAM_CODEC, PacketHookahPuffDataClientHandler::handle);
        // Регистрируем наш пакет PacketPuffDuration (Client-to-Server)
        registrar.playToServer(new CustomPacketPayload.Type<>(PacketPuffDuration.ID), PacketPuffDuration.STREAM_CODEC, PacketPuffDurationServerHandler::handle);
        // Регистрируем новые пакеты для начала/остановки затяжки
        registrar.playToServer(new CustomPacketPayload.Type<>(PacketStartPuffing.ID), PacketStartPuffing.STREAM_CODEC, PacketStartPuffingServerHandler::handle);
        registrar.playToServer(new CustomPacketPayload.Type<>(PacketStopPuffing.ID), PacketStopPuffing.STREAM_CODEC, PacketStopPuffingServerHandler::handle);
        // Регистрируем новый пакет для прикрепления трубки
        registrar.playToServer(PacketAttachPipe.ID, PacketAttachPipe.STREAM_CODEC, PacketAttachPipeServerHandler::handle);
        // Удаляем регистрацию PacketDetachPipe
        // registrar.playToServer(PacketDetachPipe.TYPE, PacketDetachPipe.STREAM_CODEC, PacketDetachPipe::handle);
    }

    public static <T extends CustomPacketPayload> void sendToClientsTrackingBlock(T payload, BlockEntity blockEntity) {
        if (blockEntity.getLevel() instanceof ServerLevel serverLevel && !blockEntity.getLevel().isClientSide()) {
            LOGGER.debug("Server: Sending packet {} to clients tracking block at {}", payload.type().id(), blockEntity.getBlockPos()); // ADDED LOG
            PacketDistributor.sendToPlayersTrackingChunk(serverLevel, new ChunkPos(blockEntity.getBlockPos()), payload);
        }
    }
}
