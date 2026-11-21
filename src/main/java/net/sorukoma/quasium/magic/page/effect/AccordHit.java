package net.sorukoma.quasium.magic.page.effect;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.sorukoma.quasium.magic.CastContext;
import net.sorukoma.quasium.magic.MagicMath;
import net.sorukoma.quasium.magic.page.Accord;
import net.sorukoma.quasium.magic.page.CompiledPage;
import net.sorukoma.quasium.magic.page.PageCompiler;
import net.sorukoma.quasium.magic.page.PageTarget;
import net.sorukoma.quasium.magic.page.PageTargets;

record AccordHit(CastContext ctx, CompiledPage page, Accord accord, float strength) {
    ServerLevel level() {
        return ctx.level();
    }

    float power() {
        return ctx.power() * strength;
    }

    float param(String key, float fallback) {
        float value = accord.param(key, fallback);
        return key.endsWith("_seconds") ? value * page.param(PageCompiler.SECONDS, 1f) : value;
    }

    float radius(String key, float fallback) {
        return param(key, fallback) * Strikes.widened(page, "radius");
    }

    int ticks(String key, float fallbackSeconds) {
        return Math.round(param(key, fallbackSeconds) * MagicMath.TICKS_PER_SECOND * power());
    }

    int flatTicks(String key, float fallbackSeconds) {
        return Math.round(param(key, fallbackSeconds) * MagicMath.TICKS_PER_SECOND);
    }

    void damage(LivingEntity target, float amount) {
        ctx.withPower(power()).damage(target, amount);
    }

    void damage(LivingEntity target, float amount, ResourceKey<DamageType> type) {
        ctx.withPower(power()).damage(target, amount, type);
    }

    boolean isFoe(LivingEntity entity) {
        return PageTargets.accepts(PageTarget.FOE, ctx.caster(), entity);
    }

    boolean isAlly(LivingEntity entity) {
        return PageTargets.isAlly(ctx.caster(), entity);
    }

    void puff(LivingEntity target, ParticleOptions particle, int count) {
        Strikes.puff(level(), page, target, particle, count);
    }
}
