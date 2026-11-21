package net.sorukoma.quasium.tea;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.api.KaifChangeEvent;
import net.sorukoma.quasium.api.SmokeCastEvent;
import net.sorukoma.quasium.api.TobaccoConsumptionEvent;
import net.sorukoma.quasium.ecology.EcologyConfig;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID)
public final class SobrietySeals {
    private static final Map<ResourceKey<Level>, Set<BlockPos>> SEALS = new HashMap<>();

    private SobrietySeals() {
    }

    static void add(Level level, BlockPos pos) {
        SEALS.computeIfAbsent(level.dimension(), key -> new HashSet<>()).add(pos.immutable());
    }

    static void remove(Level level, BlockPos pos) {
        Set<BlockPos> seals = SEALS.get(level.dimension());
        if (seals != null) seals.remove(pos);
    }

    public static boolean covers(Player player) {
        if (player.level().isClientSide) return false;
        Set<BlockPos> seals = SEALS.get(player.level().dimension());
        if (seals == null || seals.isEmpty()) return false;
        double radius = EcologyConfig.SEAL_RADIUS.getAsInt();
        Vec3 at = player.position();
        for (BlockPos seal : seals) {
            if (Vec3.atCenterOf(seal).distanceToSqr(at) <= radius * radius) return true;
        }
        return false;
    }

    @SubscribeEvent
    public static void onCast(SmokeCastEvent event) {
        if (!EcologyConfig.SEAL_ALLOWS_CASTS.getAsBoolean() && covers(event.player())) {
            event.refuse(Component.translatable("quasium.seal.no_casts"));
        }
    }

    @SubscribeEvent
    public static void onKaifChange(KaifChangeEvent event) {
        if (covers(event.player())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onTobacco(TobaccoConsumptionEvent event) {
        if (covers(event.player())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof Level level && !level.isClientSide) SEALS.remove(level.dimension());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        SEALS.clear();
    }
}
