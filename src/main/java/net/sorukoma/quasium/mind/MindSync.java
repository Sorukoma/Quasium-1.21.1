package net.sorukoma.quasium.mind;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.data.PlayerFlavorPreferenceData;
import net.sorukoma.quasium.data.PlayerOverdoseData;
import net.sorukoma.quasium.data.PlayerTobaccoData;
import net.sorukoma.quasium.mind.research.Perk;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID)
public final class MindSync {
    private static final int FLUSH_TICKS = 10;
    private static final Set<UUID> DIRTY = ConcurrentHashMap.newKeySet();

    private MindSync() {
    }

    public static void markDirty(ServerPlayer player) {
        DIRTY.add(player.getUUID());
    }

    public static void send(ServerPlayer player) {
        DIRTY.remove(player.getUUID());
        PacketDistributor.sendToPlayer(player, new MindPackets.Sync(snapshot(player)));
    }

    private static CompoundTag snapshot(ServerPlayer player) {
        CompoundTag tag = MindData.root(player).copy();
        tag.put("Tolerance", PlayerTobaccoData.toleranceTag(player));
        tag.put("Stats", MindStats.snapshot(player));
        tag.putInt("Stage", PlayerOverdoseData.stage(player));
        tag.putFloat("Grams", PlayerOverdoseData.windowGrams(player));
        tag.putInt("Tried", PlayerTobaccoData.getTriedFlavors(player).size());
        if (Perk.SOMMELIER.ownedBy(player)) {
            CompoundTag preferences = new CompoundTag();
            PlayerFlavorPreferenceData.getAllPreferences(player).forEach(preferences::putInt);
            tag.put("Preferences", preferences);
        }
        return tag;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % FLUSH_TICKS == 0 && DIRTY.contains(player.getUUID())) {
            send(player);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) send(player);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) send(player);
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) send(player);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        DIRTY.remove(event.getEntity().getUUID());
    }
}
