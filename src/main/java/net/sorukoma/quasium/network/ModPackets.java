package net.sorukoma.quasium.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.golem.GolemBindPacket;
import net.sorukoma.quasium.golem.GolemFeedPacket;
import net.sorukoma.quasium.golem.GolemProfile;
import net.sorukoma.quasium.screen.ProgramEditPacket;
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
        // Регистрируем пакет ProgressionArticlesSyncPacket (Server-to-Client)
        registrar.playToClient(ProgressionArticlesSyncPacket.ID, ProgressionArticlesSyncPacket.STREAM_CODEC, ProgressionArticlesSyncPacket::handle);
        // Регистрируем новые пакеты для начала/остановки затяжки
        registrar.playToServer(PacketStartPuffing.ID, PacketStartPuffing.STREAM_CODEC, PacketStartPuffingServerHandler::handle);
        registrar.playToServer(PacketStopPuffing.ID, PacketStopPuffing.STREAM_CODEC, PacketStopPuffingServerHandler::handle);
        // Регистрируем пакет для прикрепления трубки
        registrar.playToServer(PacketAttachPipeToServer.ID, PacketAttachPipeToServer.STREAM_CODEC, PacketAttachPipeToServerHandler::handle);
        // Регистрируем пакет для открепления трубки
        registrar.playToServer(PacketDetachPipeToServer.ID, PacketDetachPipeToServer.STREAM_CODEC, PacketDetachPipeToServerHandler::handle);
        // Модульный кальян: сборка/вода/GUI (shift+ПКМ с предметом ванилла не доводит до useItemOn)
        registrar.playToServer(PacketModularHookahInteract.ID, PacketModularHookahInteract.STREAM_CODEC, PacketModularHookahInteractHandler::handle);
        // Регистрируем пакеты для наспинного кальяна
        registrar.playToServer(PacketOpenBackpackHookahGui.ID, PacketOpenBackpackHookahGui.STREAM_CODEC, PacketOpenBackpackHookahGuiHandler::handle);
        registrar.playToServer(PacketAttachPipeBackpack.ID, PacketAttachPipeBackpack.STREAM_CODEC, PacketAttachPipeBackpackHandler::handle);
        registrar.playToServer(PacketDetachPipeBackpack.ID, PacketDetachPipeBackpack.STREAM_CODEC, PacketDetachPipeBackpackHandler::handle);
        registrar.playToServer(PacketStartPuffingBackpack.ID, PacketStartPuffingBackpack.STREAM_CODEC, PacketStartPuffingBackpackHandler::handle);
        registrar.playToServer(PacketStopPuffingBackpack.ID, PacketStopPuffingBackpack.STREAM_CODEC, PacketStopPuffingBackpackHandler::handle);
        registrar.playToClient(PacketBackpackHookahPuffData.ID, PacketBackpackHookahPuffData.STREAM_CODEC, PacketBackpackHookahPuffDataClientHandler::handle);
        registrar.playToClient(PacketSyncBackpackPipeConnection.ID, PacketSyncBackpackPipeConnection.STREAM_CODEC, PacketSyncBackpackPipeConnectionHandler::handle);
        registrar.playToClient(MagicStatePacket.ID, MagicStatePacket.STREAM_CODEC, MagicStatePacket::handle);
        registrar.playToClient(ScanDataSyncPacket.ID, ScanDataSyncPacket.STREAM_CODEC, ScanDataSyncPacket::handle);
        registrar.playToClient(OreGlowPacket.ID, OreGlowPacket.STREAM_CODEC, OreGlowPacket::handle);
        registrar.commonToClient(SmokecraftDataPacket.ID, SmokecraftDataPacket.STREAM_CODEC, SmokecraftDataPacket::handle);
        registrar.playToClient(ZoneStatePacket.ID, ZoneStatePacket.STREAM_CODEC, ZoneStatePacket::handle);
        registrar.playToClient(ZoneRingPacket.ID, ZoneRingPacket.STREAM_CODEC, ZoneRingPacket::handle);
        registrar.playToClient(PipeFlowPacket.ID, PipeFlowPacket.STREAM_CODEC, PipeFlowPacket::handle);
        registrar.playToServer(ExhalePacket.ID, ExhalePacket.STREAM_CODEC, ExhalePacket::handle);
        registrar.playToServer(GolemFeedPacket.ID, GolemFeedPacket.STREAM_CODEC, GolemFeedPacket::handle);
        registrar.playToServer(GolemBindPacket.ID, GolemBindPacket.STREAM_CODEC, GolemBindPacket::handle);
        registrar.playToServer(ProgramEditPacket.ID, ProgramEditPacket.STREAM_CODEC, ProgramEditPacket::handle);
        registrar.playToClient(GolemProfile.ID, GolemProfile.STREAM_CODEC, GolemProfile::handle);
        registrar.playToServer(ClaimBookRewardPacket.ID, ClaimBookRewardPacket.STREAM_CODEC, ClaimBookRewardPacket::handle);
        registrar.playToClient(ClaimedRewardsPacket.ID, ClaimedRewardsPacket.STREAM_CODEC, ClaimedRewardsPacket::handle);
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