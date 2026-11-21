package net.sorukoma.quasium.magic.page.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.sorukoma.quasium.magic.page.Grounding;

final class BindingAccords {
    private static final float SMALL_MOB_HEIGHT = 1.0f;

    private BindingAccords() {
    }

    static void amber(AccordHit hit, LivingEntity target) {
        if (!hit.isFoe(target)) return;
        target.addEffect(new MobEffectInstance(HeartRegistry.AMBER, Strikes.controlTicks(target, hit.ticks("hold_seconds", 2f)), 0));
        if (target instanceof Mob mob) mob.getNavigation().stop();
        hit.puff(target, ParticleTypes.WAX_ON, 16);
    }

    static void quagmire(AccordHit hit, LivingEntity target) {
        if (!hit.isFoe(target)) return;
        int ticks = hit.ticks("effect_seconds", 4f);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, Math.round(hit.param("slow_level", 4f)) - 1));
        if (target instanceof Mob && target.getBbHeight() <= SMALL_MOB_HEIGHT) {
            target.addEffect(new MobEffectInstance(HeartRegistry.ROOTED, Strikes.controlTicks(target, ticks), 0));
        }
        hit.puff(target, ParticleTypes.FALLING_DRIPSTONE_WATER, 12);
    }

    static void collapse(AccordHit hit, LivingEntity target) {
        if (!hit.isFoe(target)) return;
        Grounding.ground(target, hit.param("pull_down", 1.5f), hit.ticks("effect_seconds", 4f));
        hit.puff(target, ParticleTypes.LARGE_SMOKE, 12);
    }
}
