package net.sorukoma.quasium.smokelands.storm;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.api.SmokeCastEvent;
import net.sorukoma.quasium.smokelands.SmokelandsConfig;
import net.sorukoma.quasium.smokelands.SmokelandsDimension;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID)
public final class StormEvents {
    private static final float MIN_MISS_DEGREES = 25f;
    private static final float EXTRA_MISS_DEGREES = 20f;
    private static final Map<UUID, Float> DEFLECTED = new HashMap<>();

    private StormEvents() {
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level && SmokelandsDimension.contains(level)) CinderStorm.of(level).tick(level);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) sync(player);
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) sync(player);
    }

    private static void sync(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new StormStatePacket(CinderStorm.raging(player.serverLevel())));
    }

    @SubscribeEvent
    public static void onCast(SmokeCastEvent event) {
        ServerPlayer player = event.player();
        if (!CinderStorm.raging(player.serverLevel())) return;
        if (player.getRandom().nextInt(100) < SmokelandsConfig.STORM_CAST_MISS_PCT.get()) deflect(player);
    }

    public static void deflect(ServerPlayer player) {
        float miss = MIN_MISS_DEGREES + player.getRandom().nextFloat() * EXTRA_MISS_DEGREES;
        DEFLECTED.putIfAbsent(player.getUUID(), player.getYRot());
        player.setYRot(player.getYRot() + (player.getRandom().nextBoolean() ? miss : -miss));
        player.displayClientMessage(Component.translatable("quasium.smokelands.storm_miss"), true);
    }

    public static void restore(ServerPlayer player) {
        Float aimed = DEFLECTED.remove(player.getUUID());
        if (aimed != null) player.setYRot(aimed);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) restore(player);
    }
}
