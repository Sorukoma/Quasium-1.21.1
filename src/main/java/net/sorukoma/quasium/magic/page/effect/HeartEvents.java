package net.sorukoma.quasium.magic.page.effect;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.effect.ModEffects;
import net.sorukoma.quasium.magic.page.PageRegistry;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID)
public final class HeartEvents {
    private HeartEvents() {
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        boolean ambered = event.getEntity().hasEffect(HeartRegistry.AMBER);
        boolean calmAttacker = event.getSource().getEntity() instanceof LivingEntity attacker && attacker.hasEffect(HeartRegistry.CALMED);
        if (ambered || calmAttacker) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        LivingEntity next = event.getNewAboutToBeSetTarget();
        if (next != null && HeartStatuses.blocksTarget(event.getEntity(), next)) event.setNewAboutToBeSetTarget(null);
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        FireAccords.smolderBurst(event.getEntity());
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        LivingEntity faller = event.getEntity();
        if (!faller.hasEffect(PageRegistry.GROUNDED) || faller.hasEffect(ModEffects.HEAVY)) return;
        event.setDamageMultiplier(event.getDamageMultiplier() * Accords.param("collapse", "fall_multiplier", 1.5f));
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof LivingEntity living) NoteMarks.tick(living);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        LingeringGround.tick(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        LingeringGround.clear();
    }
}
