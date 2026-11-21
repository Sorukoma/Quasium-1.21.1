package net.sorukoma.quasium.magic.page.effect;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;
import net.sorukoma.quasium.magic.CastContext;
import net.sorukoma.quasium.magic.page.CompiledPage;
import net.sorukoma.quasium.magic.page.CoreHearts;
import net.sorukoma.quasium.magic.page.PageCatalog;
import net.sorukoma.quasium.magic.page.PageConfig;
import net.sorukoma.quasium.magic.page.PageMath;
import net.sorukoma.quasium.magic.page.PageTop;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

final class Strikes {
    private static final int QUIET_PARTICLE_SHARE = 4;

    private Strikes() {
    }

    static boolean isWet(LivingEntity entity) {
        return CoreHearts.isWet(entity);
    }

    static int controlTicks(LivingEntity target, int ticks) {
        return controlTicks(target, ticks, PageConfig.PLAYER_CONTROL_PCT.getAsInt() / 100f);
    }

    static int controlTicks(LivingEntity target, int ticks, float playerShare) {
        int length = target instanceof Player ? Math.round(ticks * playerShare) : ticks;
        if (target.getType().is(Tags.EntityTypes.BOSSES)) length = Math.min(length, PageConfig.BOSS_CONTROL_TICKS.getAsInt());
        return length;
    }

    static float widened(CompiledPage page, String key) {
        PageCatalog catalog = PageCatalog.get();
        float scale = 1f;
        for (Map.Entry<String, Integer> top : page.tops().entrySet()) {
            PageTop definition = catalog.top(top.getKey());
            Float step = definition == null ? null : definition.scale().get(key);
            if (step != null) scale *= PageMath.stackedScale(step, top.getValue());
        }
        return scale;
    }

    static boolean silent(CompiledPage page) {
        return page.param("silent", 0f) > 0f;
    }

    static void puff(ServerLevel level, CompiledPage page, LivingEntity target, ParticleOptions particle, int count) {
        int shown = silent(page) ? Math.max(1, count / QUIET_PARTICLE_SHARE) : count;
        level.sendParticles(particle, target.getX(), target.getY(0.6), target.getZ(),
                shown, target.getBbWidth() / 2, target.getBbHeight() / 3, target.getBbWidth() / 2, 0.02);
    }

    static Vec3 origin(CastContext ctx) {
        if (ctx.carrier() != null) return ctx.carrier().position();
        return ctx.caster() == null ? Vec3.ZERO : ctx.caster().position();
    }

    static void push(LivingEntity target, Vec3 from, double strength) {
        target.knockback(strength, from.x - target.getX(), from.z - target.getZ());
        target.hurtMarked = true;
    }

    static void forgetTarget(Mob mob) {
        mob.setTarget(null);
        mob.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
        if (mob instanceof NeutralMob angry) angry.stopBeingAngry();
    }

    static List<LivingEntity> around(ServerLevel level, Vec3 center, double radius, Predicate<LivingEntity> wanted) {
        return level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius),
                entity -> entity.isAlive() && !entity.isSpectator() && entity.distanceToSqr(center) <= radius * radius && wanted.test(entity));
    }
}
