package net.sorukoma.quasium.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.particle.ModParticles;
import net.sorukoma.quasium.sound.ModSoundEvents;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID, value = Dist.CLIENT)
public final class ClientPipeFlow {
    private static final int FLOW_TTL_TICKS = 30;
    private static final double VIEW_DISTANCE_SQR = 32.0 * 32.0;
    private static final double HISS_DISTANCE_SQR = 12.0 * 12.0;
    private static final Map<BlockPos, Flow> FLOWS = new HashMap<>();

    private record Flow(int color, float load, long until) {
    }

    private ClientPipeFlow() {
    }

    public static void update(List<BlockPos> pipes, int color, float load) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        long until = minecraft.level.getGameTime() + FLOW_TTL_TICKS;
        for (BlockPos pipe : pipes) FLOWS.put(pipe, new Flow(color, load, until));
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || minecraft.isPaused() || FLOWS.isEmpty()) return;
        long now = minecraft.level.getGameTime();
        RandomSource random = minecraft.level.random;
        if (now % 20 == 0) hiss(minecraft);
        for (Iterator<Map.Entry<BlockPos, Flow>> it = FLOWS.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<BlockPos, Flow> entry = it.next();
            Flow flow = entry.getValue();
            if (flow.until() < now) {
                it.remove();
                continue;
            }
            BlockPos pipe = entry.getKey();
            if (minecraft.player.distanceToSqr(pipe.getCenter()) > VIEW_DISTANCE_SQR || random.nextFloat() > flow.load() * 0.5f) continue;
            minecraft.level.addParticle(ModParticles.noteSmoke(flow.color()),
                    pipe.getX() + 0.4 + random.nextDouble() * 0.2, pipe.getY() + 0.4 + random.nextDouble() * 0.2,
                    pipe.getZ() + 0.4 + random.nextDouble() * 0.2,
                    (random.nextDouble() - 0.5) * 0.02, 0.0, (random.nextDouble() - 0.5) * 0.02);
        }
    }

    private static void hiss(Minecraft minecraft) {
        BlockPos nearest = null;
        double best = HISS_DISTANCE_SQR;
        for (BlockPos pipe : FLOWS.keySet()) {
            double distance = minecraft.player.distanceToSqr(pipe.getCenter());
            if (distance < best) {
                best = distance;
                nearest = pipe;
            }
        }
        if (nearest == null) return;
        float volume = 0.12f + 0.3f * FLOWS.get(nearest).load();
        minecraft.level.playLocalSound(nearest, ModSoundEvents.PIPE_FLOW.get(), SoundSource.BLOCKS, volume, 1f, false);
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        FLOWS.clear();
    }
}
