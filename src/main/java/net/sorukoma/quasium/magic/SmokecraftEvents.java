package net.sorukoma.quasium.magic;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.player.PlayerSpawnPhantomsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.sorukoma.quasium.Config;
import net.sorukoma.quasium.effect.ModEffects;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterConfigurationTasksEvent;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.data.PlayerMagicData;
import net.sorukoma.quasium.network.SmokecraftDataPacket;
import net.sorukoma.quasium.network.SmokecraftDataTask;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID)
public final class SmokecraftEvents {
    private SmokecraftEvents() {
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new CodecDataLoader<>("note_profile", NoteProfile.CODEC, NoteProfiles::set));
        event.addListener(new CodecDataLoader<>("note_rule", NoteRule.FILE_CODEC, NoteRules::set));
        event.addListener(new CodecDataLoader<>("spell", Spell.CODEC, Spells::set));
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        NoteDerivation.refresh(event.getServer());
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() == null) NoteDerivation.refresh(event.getPlayerList().getServer());
        SmokecraftDataPacket packet = SmokecraftDataPacket.current();
        if (event.getPlayer() != null) {
            PacketDistributor.sendToPlayer(event.getPlayer(), packet);
        } else {
            PacketDistributor.sendToAllPlayers(packet);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) PlayerMagicData.sync(player);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) PlayerMagicData.sync(player);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SmokeCasting.forget(event.getEntity().getUUID());
        PlayerZones.forget(event.getEntity().getUUID());
        AromaEffects.forget(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        LivingEntity target = event.getNewAboutToBeSetTarget();
        if (target == null) return;
        if (SpellActions.isPlayerInScreen(target) || (target instanceof Player player && AromaEffects.hidesFromMobs(player))) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        PlayerZones.tick(player);
        CozyEvening.tick(player);
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (event.getEntity().hasEffect(ModEffects.HEAVY)) {
            event.setDamageMultiplier(event.getDamageMultiplier() * Config.HEAVY_FALL_DAMAGE_PCT.getAsInt() / 100f);
        }
    }

    @SubscribeEvent
    public static void onPhantoms(PlayerSpawnPhantomsEvent event) {
        if (PlayerZones.tier(event.getEntity()) > 0) event.setResult(PlayerSpawnPhantomsEvent.Result.DENY);
    }

    @SubscribeEvent
    public static void onVisibility(LivingEvent.LivingVisibilityEvent event) {
        if (event.getEntity() instanceof Player player && PlayerZones.tier(player) > 0) {
            event.modifyVisibility(1.0 - Config.ZONE_AGGRO_REDUCTION_PCT.getAsInt() / 100.0);
        }
    }

    public static void onRegisterConfigurationTasks(RegisterConfigurationTasksEvent event) {
        event.register(new SmokecraftDataTask(event.getListener()));
    }
}
