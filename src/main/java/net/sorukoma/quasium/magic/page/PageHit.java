package net.sorukoma.quasium.magic.page;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.sorukoma.quasium.magic.CastContext;
import net.sorukoma.quasium.magic.MagicMath;

public record PageHit(CastContext ctx, CompiledPage page, PageEffect effect, float strength) {
    public ServerLevel level() {
        return ctx.level();
    }

    public float power() {
        return ctx.power() * strength;
    }

    public float param(String key, float fallback) {
        float value = effect.param(key, fallback);
        return key.endsWith("_seconds") ? value * page.param(PageCompiler.SECONDS, 1f) : value;
    }

    public int ticks(String key, float fallbackSeconds) {
        return Math.round(param(key, fallbackSeconds) * MagicMath.TICKS_PER_SECOND * power());
    }

    public int flatTicks(String key, float fallbackSeconds) {
        return Math.round(param(key, fallbackSeconds) * MagicMath.TICKS_PER_SECOND);
    }

    public void damage(LivingEntity target, float amount) {
        ctx.withPower(power()).damage(target, amount);
    }

    public boolean isAlly(LivingEntity entity) {
        return PageTargets.isAlly(ctx.caster(), entity);
    }
}
