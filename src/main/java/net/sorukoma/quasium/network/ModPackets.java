package net.sorukoma.quasium.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.sorukoma.quasium.QuasiumMod;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import net.sorukoma.quasium.network.TobaccoDataSyncPacket;

public class ModPackets {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(QuasiumMod.MOD_ID);

        // Регистрируем наш пакет KaifUpdatePacket
        registrar.playToClient(KaifUpdatePacket.ID, KaifUpdatePacket.STREAM_CODEC, KaifUpdatePacket::handle);
        // Регистрируем наш пакет PacketSpawnSmoke
        registrar.playToClient(PacketSpawnSmoke.ID, PacketSpawnSmoke.STREAM_CODEC, PacketSpawnSmokeClientHandler::handle);
        // Регистрируем наш пакет PacketHookahPuffData
        registrar.playToClient(PacketHookahPuffData.ID, PacketHookahPuffData.STREAM_CODEC, PacketHookahPuffDataClientHandler::handle);
        // Регистрируем наш пакет PacketStopHookahPuffSoundClient
        registrar.playToClient(PacketStopHookahPuffSoundClient.ID, PacketStopHookahPuffSoundClient.STREAM_CODEC, PacketStopHookahPuffSoundClient::handle);
        // Регистрируем наш пакет PacketSyncHookahConnection (Server-to-Client)
        registrar.playToClient(PacketSyncHookahConnection.ID, PacketSyncHookahConnection.STREAM_CODEC, PacketSyncHookahConnectionHandler::handle);
        // Регистрируем пакет TobaccoDataSyncPacket (Server-to-Client)
        registrar.playToClient(TobaccoDataSyncPacket.ID, TobaccoDataSyncPacket.STREAM_CODEC, TobaccoDataSyncPacket::handle);
        // Регистрируем пакет OreLuckDataSyncPacket (Server-to-Client)
        registrar.playToClient(OreLuckDataSyncPacket.ID, OreLuckDataSyncPacket.STREAM_CODEC, OreLuckDataSyncPacket::handle);
        // Регистрируем пакет OverdoseStatusPacket (Server-to-Client)
        registrar.playToClient(OverdoseStatusPacket.ID, OverdoseStatusPacket.STREAM_CODEC, OverdoseStatusPacket::handle);
        // Регистрируем новые пакеты для начала/остановки затяжки
        registrar.playToServer(PacketStartPuffing.ID, PacketStartPuffing.STREAM_CODEC, PacketStartPuffingServerHandler::handle);
        registrar.playToServer(PacketStopPuffing.ID, PacketStopPuffing.STREAM_CODEC, PacketStopPuffingServerHandler::handle);
        // Регистрируем пакет для прикрепления трубки
        registrar.playToServer(PacketAttachPipeToServer.ID, PacketAttachPipeToServer.STREAM_CODEC, PacketAttachPipeToServerHandler::handle);
        // Регистрируем пакет для открепления трубки
        registrar.playToServer(PacketDetachPipeToServer.ID, PacketDetachPipeToServer.STREAM_CODEC, PacketDetachPipeToServerHandler::handle);
        // Регистрируем пакет для смешивания табака
        registrar.playToServer(PacketMixTobacco.TYPE, PacketMixTobacco.STREAM_CODEC, PacketMixTobacco::handle);
        // Регистрируем пакеты для наспинного кальяна
        registrar.playToServer(PacketOpenBackpackHookahGui.ID, PacketOpenBackpackHookahGui.STREAM_CODEC, PacketOpenBackpackHookahGuiHandler::handle);
        registrar.playToServer(PacketAttachPipeBackpack.ID, PacketAttachPipeBackpack.STREAM_CODEC, PacketAttachPipeBackpackHandler::handle);
        registrar.playToServer(PacketDetachPipeBackpack.ID, PacketDetachPipeBackpack.STREAM_CODEC, PacketDetachPipeBackpackHandler::handle);
        registrar.playToServer(PacketStartPuffingBackpack.ID, PacketStartPuffingBackpack.STREAM_CODEC, PacketStartPuffingBackpackHandler::handle);
        registrar.playToServer(PacketStopPuffingBackpack.ID, PacketStopPuffingBackpack.STREAM_CODEC, PacketStopPuffingBackpackHandler::handle);
        registrar.playToClient(PacketBackpackHookahPuffData.ID, PacketBackpackHookahPuffData.STREAM_CODEC, PacketBackpackHookahPuffDataClientHandler::handle);
        registrar.playToClient(PacketSyncBackpackPipeConnection.ID, PacketSyncBackpackPipeConnection.STREAM_CODEC, PacketSyncBackpackPipeConnectionHandler::handle);
    }

    public static <T extends CustomPacketPayload> void sendToClientsTrackingBlock(T payload, BlockEntity blockEntity) {
        if (blockEntity.getLevel() instanceof ServerLevel serverLevel && !blockEntity.getLevel().isClientSide()) {
            PacketDistributor.sendToPlayersTrackingChunk(serverLevel, new ChunkPos(blockEntity.getBlockPos()), payload);
        }
    }

    /**
     * Отправляет пакет всем клиентам, которые видят данную сущность, включая самого игрока.
     * Используется для эффектов, привязанных к конкретной сущности (например, дым из головы курящего),
     * чтобы каждый клиент отрисовал эффект на правильной сущности.
     */
    public static <T extends CustomPacketPayload> void sendToClientsTrackingEntityAndSelf(T payload, net.minecraft.world.entity.Entity entity) {
        if (entity.level() instanceof ServerLevel && !entity.level().isClientSide()) {
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity, payload);
        }
    }
}