package net.sorukoma.quasium.mind.overdose;

import com.mojang.logging.LogUtils;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.advancements.triggers.SmokecraftTrigger;
import net.sorukoma.quasium.api.KaifComaEvent;
import net.sorukoma.quasium.data.PlayerKaifData;
import net.sorukoma.quasium.data.PlayerOverdoseData;
import net.sorukoma.quasium.mind.MindConfig;
import net.sorukoma.quasium.mind.MindPackets;
import net.sorukoma.quasium.mind.MindRegistry;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID)
public final class KaifComa {
    public static final ResourceKey<DamageType> SMOKERS_LUNG =
            ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "smokers_lung"));
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation STILL = ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "kaif_coma");
    private static final List<Holder<Attribute>> FROZEN = List.of(Attributes.MOVEMENT_SPEED, Attributes.JUMP_STRENGTH);
    private static final Map<UUID, Long> WAKE_AT = new HashMap<>();

    private KaifComa() {
    }

    public static void enter(ServerPlayer player) {
        if (MindConfig.REALISM_DEATH.get()) {
            player.hurt(player.damageSources().source(SMOKERS_LUNG), Float.MAX_VALUE);
            return;
        }
        int ticks = MindConfig.COMA_SECONDS.getAsInt() * 20;
        NeoForge.EVENT_BUS.post(new KaifComaEvent(player, ticks));
        WAKE_AT.put(player.getUUID(), player.level().getGameTime() + ticks);
        freeze(player, true);
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, ticks + 20, 0, false, false));
        player.level().playSound(null, player.blockPosition(), MindRegistry.COMA_FADE.get(), SoundSource.PLAYERS, 1f, 1f);
        MindPackets.cue(player, "coma", ticks);
        SmokecraftTrigger.fire(player, "kaif_coma");
        LOGGER.info("{} fell into a kaif coma for {} s", player.getGameProfile().getName(), ticks / 20);
    }

    public static boolean inComa(Player player) {
        return WAKE_AT.containsKey(player.getUUID());
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) tick(player);
    }

    public static void tick(ServerPlayer player) {
        Long wakeAt = WAKE_AT.get(player.getUUID());
        if (wakeAt == null) return;
        if (player.level().getGameTime() >= wakeAt) {
            wake(player);
        } else {
            player.setDeltaMovement(0, Math.min(0, player.getDeltaMovement().y), 0);
        }
    }

    private static void wake(ServerPlayer player) {
        WAKE_AT.remove(player.getUUID());
        freeze(player, false);
        player.removeEffect(MobEffects.BLINDNESS);
        player.getFoodData().setFoodLevel(Math.max(0, player.getFoodData().getFoodLevel() - MindConfig.COMA_FOOD_LOSS.getAsInt()));
        PlayerOverdoseData.clearWindow(player);
        PlayerKaifData.setKaifValue(player, MindConfig.COMA_WAKE_KAIF.getAsInt());
        MindPackets.cue(player, "coma", 0);
        LOGGER.info("{} woke from the kaif coma, kaif {}", player.getGameProfile().getName(), PlayerKaifData.getKaifValue(player));
    }

    private static void freeze(ServerPlayer player, boolean still) {
        for (Holder<Attribute> attribute : FROZEN) {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance == null) continue;
            if (still) {
                instance.addOrUpdateTransientModifier(new AttributeModifier(STILL, -1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            } else {
                instance.removeModifier(STILL);
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && inComa(player)) wake(player);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (WAKE_AT.remove(event.getEntity().getUUID()) != null && event.getEntity() instanceof ServerPlayer player) {
            MindPackets.cue(player, "coma", 0);
        }
    }
}
