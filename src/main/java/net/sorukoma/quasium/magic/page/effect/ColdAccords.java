package net.sorukoma.quasium.magic.page.effect;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.sorukoma.quasium.magic.ScatterTeleport;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

final class ColdAccords {
    private static final int ICE_DEPTH = 2;

    private ColdAccords() {
    }

    static void spark(AccordHit hit, LivingEntity target) {
        if (!hit.isFoe(target)) return;
        float reach = hit.param("reach", 5f);
        Set<LivingEntity> struck = new HashSet<>();
        struck.add(target);
        LivingEntity from = target;
        for (int jump = 0; jump < Math.round(hit.param("jumps", 2f)); jump++) {
            LivingEntity last = from;
            LivingEntity next = Strikes.around(hit.level(), last.position(), reach, entity -> !struck.contains(entity) && hit.isFoe(entity))
                    .stream()
                    .min(Comparator.comparingDouble(entity -> entity.distanceToSqr(last)))
                    .orElse(null);
            if (next == null) return;
            float damage = hit.param("damage", 2f);
            if (Strikes.isWet(next)) damage *= hit.param("wet_multiplier", 2f);
            hit.damage(next, damage);
            arc(hit.level(), last.getEyePosition(), next.getEyePosition());
            struck.add(next);
            from = next;
        }
    }

    private static void arc(ServerLevel level, Vec3 from, Vec3 to) {
        Vec3 step = to.subtract(from);
        int points = Math.max(2, (int) (step.length() * 3));
        for (int i = 0; i <= points; i++) {
            Vec3 point = from.add(step.scale(i / (double) points));
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, point.x, point.y, point.z, 1, 0.05, 0.05, 0.05, 0.0);
        }
    }

    static void lightness(AccordHit hit, LivingEntity target) {
        if (hit.isAlly(target)) {
            int ticks = hit.ticks("effect_seconds", 6f);
            target.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, ticks, 0));
            target.addEffect(new MobEffectInstance(MobEffects.JUMP, ticks, 0));
            hit.puff(target, ParticleTypes.CLOUD, 8);
            return;
        }
        Vec3 motion = target.getDeltaMovement();
        target.setDeltaMovement(motion.x, Math.max(motion.y, hit.param("lift", 0.8f) * hit.power()), motion.z);
        target.hurtMarked = true;
        hit.puff(target, ParticleTypes.CLOUD, 12);
    }

    static void rime(AccordHit hit, LivingEntity target) {
        if (!hit.isFoe(target)) return;
        freezeWater(hit, target);
        if (!Strikes.isWet(target)) return;
        int ticks = Strikes.controlTicks(target, hit.ticks("freeze_seconds", 1.5f));
        target.addEffect(new MobEffectInstance(HeartRegistry.ROOTED, ticks, 0));
        if (target.canFreeze()) target.setTicksFrozen(Math.max(target.getTicksFrozen(), target.getTicksRequiredToFreeze() + ticks));
        hit.puff(target, ParticleTypes.SNOWFLAKE, 16);
    }

    private static void freezeWater(AccordHit hit, LivingEntity target) {
        ServerLevel level = hit.level();
        BlockPos feet = target.blockPosition();
        int reach = Math.round(hit.radius("ice_radius", 2f));
        int ticks = hit.flatTicks("ice_seconds", 10f);
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                if (dx * dx + dz * dz > reach * reach) continue;
                BlockPos surface = waterSurface(level, feet.offset(dx, 0, dz));
                if (surface == null || !level.getEntitiesOfClass(LivingEntity.class, new AABB(surface)).isEmpty()) continue;
                level.setBlockAndUpdate(surface, Blocks.FROSTED_ICE.defaultBlockState());
                LingeringGround.place(level, surface, Blocks.WATER.defaultBlockState(), ticks);
            }
        }
    }

    @Nullable
    private static BlockPos waterSurface(ServerLevel level, BlockPos column) {
        for (int lift = 1; lift >= -ICE_DEPTH; lift--) {
            BlockPos pos = column.above(lift);
            if (level.getBlockState(pos).is(Blocks.WATER) && level.getFluidState(pos).isSource() && level.isEmptyBlock(pos.above())) return pos;
        }
        return null;
    }

    static void glaze(AccordHit hit, LivingEntity target) {
        if (!hit.isFoe(target)) return;
        float radius = hit.radius("radius", 3f);
        LingeringGround.slick(hit.level(), target.position(), radius, hit.flatTicks("effect_seconds", 6f), hit.ctx().caster());
        hit.level().sendParticles(ParticleTypes.SNOWFLAKE, target.getX(), target.getY() + 0.1, target.getZ(),
                Strikes.silent(hit.page()) ? 4 : 24, radius / 2, 0.05, radius / 2, 0.01);
    }

    static void oblivion(AccordHit hit, LivingEntity target) {
        if (!hit.isFoe(target)) return;
        hit.puff(target, ParticleTypes.PORTAL, 20);
        ScatterTeleport.scatter(hit.level(), target, hit.param("scatter", 8f) * hit.power());
    }
}
