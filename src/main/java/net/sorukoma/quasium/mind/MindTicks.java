package net.sorukoma.quasium.mind;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.advancements.triggers.SmokecraftTrigger;
import net.sorukoma.quasium.data.PlayerKaifData;
import net.sorukoma.quasium.magic.PlayerZones;
import net.sorukoma.quasium.mind.dream.DreamCompass;
import net.sorukoma.quasium.mind.ghost.GhostVisits;
import net.sorukoma.quasium.mind.overdose.PhantomSpawner;
import net.sorukoma.quasium.mind.research.ResearchRules;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID)
public final class MindTicks {
    public static final String SEES_RUINS = "quasium.sees_ruins";
    public static final String MIRROR_OPEN = "quasium.mirror_open";
    private static final Map<UUID, Integer> ZONE_SECONDS = new HashMap<>();

    private MindTicks() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.isAlive()) return;
        if (player.tickCount % 10 == 0) DreamCompass.tick(player);
        if (player.tickCount % 20 != 0) return;
        Tolerance.decay(player);
        Insight.tick(player);
        PhantomSpawner.tick(player);
        perceive(player);
        withdraw(player);
        countZone(player);
        if (player.tickCount % 100 != 0) return;
        Titles.tick(player);
        GhostVisits.visit(player);
    }

    private static void perceive(ServerPlayer player) {
        int kaif = PlayerKaifData.getKaifValue(player);
        mark(player, SEES_RUINS, kaif >= MindConfig.PERCEPTION_RUINS.getAsInt());
        boolean mirror = kaif <= MindConfig.PERCEPTION_MIRROR.getAsInt();
        mark(player, MIRROR_OPEN, mirror);
        if (mirror && MindData.raise(player, ResearchRules.MIRROR_SEEN)) {
            player.displayClientMessage(Component.translatable("quasium.research.mirror_opened").withStyle(ChatFormatting.DARK_PURPLE), false);
            SmokecraftTrigger.fire(player, "mirror");
        }
    }

    private static void mark(ServerPlayer player, String tag, boolean on) {
        if (on) player.addTag(tag);
        else player.removeTag(tag);
    }

    private static void withdraw(ServerPlayer player) {
        if (PlayerKaifData.getKaifValue(player) < MindConfig.WITHDRAWAL_FATIGUE_KAIF.getAsInt()) {
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 60, 0, true, false, true));
        }
    }

    private static void countZone(ServerPlayer player) {
        if (PlayerZones.tier(player) == 0) return;
        if (ZONE_SECONDS.merge(player.getUUID(), 1, Integer::sum) < 60) return;
        ZONE_SECONDS.remove(player.getUUID());
        MindStats.award(player, MindRegistry.ZONE_MINUTES, 1);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID player = event.getEntity().getUUID();
        ZONE_SECONDS.remove(player);
        Insight.forget(player);
        PhantomSpawner.forget(player);
        DreamCompass.forget(player);
    }
}
