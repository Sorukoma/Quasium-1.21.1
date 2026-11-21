package net.sorukoma.quasium.ecology;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.sorukoma.quasium.QuasiumMod;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID)
public final class EcologyNetwork {
    private EcologyNetwork() {
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(ChunkSmogPacket.ID, ChunkSmogPacket.STREAM_CODEC, ChunkSmogPacket::handle);
    }

    public static void sendChunk(LevelChunk chunk) {
        if (chunk.getLevel() instanceof ServerLevel level) {
            PacketDistributor.sendToPlayersTrackingChunk(level, chunk.getPos(), packet(chunk));
        }
    }

    public static void sendChunk(ServerPlayer player, LevelChunk chunk) {
        PacketDistributor.sendToPlayer(player, packet(chunk));
    }

    private static ChunkSmogPacket packet(LevelChunk chunk) {
        return new ChunkSmogPacket(chunk.getPos().toLong(), SmokeDensity.density(chunk), SmokeDensity.smog(chunk).level());
    }
}
