package net.sorukoma.quasium.ecology.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.sorukoma.quasium.QuasiumMod;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID, value = Dist.CLIENT)
public final class ClientSmog {
    private static final Map<Long, Air> CHUNKS = new ConcurrentHashMap<>();

    private record Air(int density, int smog) {
    }

    private ClientSmog() {
    }

    public static void update(long chunk, int density, int smog) {
        Air before = density == 0 && smog == 0 ? CHUNKS.remove(chunk) : CHUNKS.put(chunk, new Air(density, smog));
        if ((before == null ? 0 : before.smog()) != smog) redraw(chunk);
    }

    public static int smog(BlockPos pos) {
        Air air = CHUNKS.get(ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4));
        return air == null ? 0 : air.smog();
    }

    public static int density(BlockPos pos) {
        Air air = CHUNKS.get(ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4));
        return air == null ? 0 : air.density();
    }

    private static void redraw(long chunk) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        int minX = ChunkPos.getX(chunk) << 4;
        int minZ = ChunkPos.getZ(chunk) << 4;
        Minecraft.getInstance().levelRenderer.setBlocksDirty(minX, level.getMinBuildHeight(), minZ, minX + 15, level.getMaxBuildHeight() - 1, minZ + 15);
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel().isClientSide()) CHUNKS.remove(event.getChunk().getPos().toLong());
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        CHUNKS.clear();
    }
}
