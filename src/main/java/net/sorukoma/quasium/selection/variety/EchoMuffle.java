package net.sorukoma.quasium.selection.variety;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.warden.Warden;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.VanillaGameEvent;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.advancements.triggers.SmokecraftTrigger;
import net.sorukoma.quasium.effect.ModEffects;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID)
public final class EchoMuffle {
    public static final String WARDEN_EVENT = "echo_warden";
    private static final String WARDEN_FOOLED_AT = "quasium_echo_warden_at";
    private static final int WARDEN_RADIUS = 16;
    private static final int WARDEN_THROTTLE_TICKS = 20;

    private EchoMuffle() {
    }

    @SubscribeEvent
    public static void onGameEvent(VanillaGameEvent event) {
        if (!(event.getCause() instanceof LivingEntity source) || !source.hasEffect(ModEffects.ECHO_MUFFLE)) return;
        event.setCanceled(true);
        if (source instanceof ServerPlayer player) noteFooledWarden(player);
    }

    private static void noteFooledWarden(ServerPlayer player) {
        long now = player.level().getGameTime();
        CompoundTag data = player.getPersistentData();
        if (now - data.getLong(WARDEN_FOOLED_AT) < WARDEN_THROTTLE_TICKS) return;
        if (player.level().getEntitiesOfClass(Warden.class, player.getBoundingBox().inflate(WARDEN_RADIUS)).isEmpty()) return;
        data.putLong(WARDEN_FOOLED_AT, now);
        SmokecraftTrigger.fire(player, WARDEN_EVENT);
    }
}
