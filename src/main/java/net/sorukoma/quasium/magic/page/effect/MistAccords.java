package net.sorukoma.quasium.magic.page.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

final class MistAccords {
    private MistAccords() {
    }

    static void balm(AccordHit hit, LivingEntity target) {
        int ticks = hit.ticks("regen_seconds", 4f);
        for (LivingEntity ally : Strikes.around(hit.level(), target.position(), hit.radius("radius", 4f), hit::isAlly)) {
            ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, ticks, 0));
            hit.puff(ally, ParticleTypes.HEART, 2);
        }
    }

    static void ferment(AccordHit hit, LivingEntity target) {
        if (!hit.isFoe(target)) return;
        int ticks = hit.ticks("effect_seconds", 6f);
        target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, ticks, 0));
        target.addEffect(new MobEffectInstance(MobEffects.HUNGER, ticks, 0));
        hit.puff(target, ParticleTypes.SNEEZE, 10);
    }

    static void fog(AccordHit hit, LivingEntity target) {
        if (!hit.isFoe(target)) return;
        float radius = hit.radius("radius", 4f);
        int ticks = hit.ticks("fog_seconds", 8f);
        for (LivingEntity lost : Strikes.around(hit.level(), target.position(), radius, hit::isFoe)) {
            if (!(lost instanceof Mob mob)) continue;
            mob.addEffect(new MobEffectInstance(HeartRegistry.FOGGED, ticks, 0));
            if (mob.getTarget() != null && !HeartStatuses.seesThroughFog(mob, mob.getTarget())) Strikes.forgetTarget(mob);
        }
        hit.level().sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, target.getX(), target.getY(0.5), target.getZ(),
                Strikes.silent(hit.page()) ? 4 : 30, radius / 2, 0.6, radius / 2, 0.01);
    }
}
