package net.sorukoma.quasium.magic.page;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.level.block.BaseFireBlock;
import net.sorukoma.quasium.effect.ModEffects;

public final class CoreHearts {
    private CoreHearts() {
    }

    static void registerAll() {
        HeartEffects.register("frost", new Frost());
        HeartEffects.register("heat", new Heat());
        HeartEffects.register("healing", new Healing());
        HeartEffects.register("soak", new Soak());
        HeartEffects.register("heaviness", new Heaviness());
        HeartEffects.register("choke", new Choke());
    }

    static boolean breathes(LivingEntity entity) {
        return !entity.getType().is(EntityTypeTags.UNDEAD) && !(entity instanceof AbstractGolem);
    }

    public static boolean isWet(LivingEntity entity) {
        return entity.hasEffect(PageRegistry.SOAKED) || entity.isInWaterRainOrBubble();
    }

    private static void puff(PageHit hit, LivingEntity target, net.minecraft.core.particles.ParticleOptions particle, int count) {
        hit.level().sendParticles(particle, target.getX(), target.getY(0.6), target.getZ(),
                count, target.getBbWidth() / 2, target.getBbHeight() / 3, target.getBbWidth() / 2, 0.02);
    }

    private static final class Frost implements HeartBehavior {
        @Override
        public void hitEntity(PageHit hit, LivingEntity target) {
            float damage = hit.param("damage", 2f);
            if (target.fireImmune() || target.getType().is(EntityTypeTags.FREEZE_HURTS_EXTRA_TYPES)) {
                damage *= hit.param("fire_mob_multiplier", 2f);
            }
            hit.damage(target, damage);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, hit.ticks("slow_seconds", 4f),
                    Math.round(hit.param("slow_level", 3f)) - 1));
            if (target.canFreeze()) {
                int frozen = target.getTicksRequiredToFreeze() + hit.ticks("freeze_seconds", 3f) * 2;
                target.setTicksFrozen(Math.max(target.getTicksFrozen(), frozen));
            }
            puff(hit, target, ParticleTypes.SNOWFLAKE, 10);
        }
    }

    private static final class Heat implements HeartBehavior {
        @Override
        public void hitEntity(PageHit hit, LivingEntity target) {
            float damage = hit.param("damage", 3f);
            if (isWet(target)) damage *= hit.param("wet_multiplier", 0.5f);
            hit.damage(target, damage);
            if (!isWet(target)) target.igniteForTicks(hit.ticks("burn_seconds", 4f));
        }

        @Override
        public void hitBlock(PageHit hit, BlockPos pos) {
            if (!hit.level().isEmptyBlock(pos) || !BaseFireBlock.canBePlacedAt(hit.level(), pos, Direction.UP)) return;
            for (Direction side : Direction.values()) {
                BlockPos neighbour = pos.relative(side);
                if (hit.level().getBlockState(neighbour).isFlammable(hit.level(), neighbour, side.getOpposite())) return;
            }
            hit.level().setBlockAndUpdate(pos, BaseFireBlock.getState(hit.level(), pos));
        }
    }

    private static final class Healing implements HeartBehavior {
        @Override
        public void hitEntity(PageHit hit, LivingEntity target) {
            float amount = hit.param("heal", 4f);
            if (target.getType().is(EntityTypeTags.UNDEAD)) {
                hit.damage(target, amount);
                return;
            }
            target.heal(amount * hit.power());
            puff(hit, target, ParticleTypes.HEART, 3);
        }
    }

    private static final class Soak implements HeartBehavior {
        @Override
        public void hitEntity(PageHit hit, LivingEntity target) {
            target.clearFire();
            target.addEffect(new MobEffectInstance(PageRegistry.SOAKED, hit.ticks("wet_seconds", 6f), 0));
            puff(hit, target, ParticleTypes.SPLASH, 12);
        }
    }

    private static final class Heaviness implements HeartBehavior {
        @Override
        public void hitEntity(PageHit hit, LivingEntity target) {
            int ticks = hit.ticks("effect_seconds", 3f);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, Math.round(hit.param("slow_level", 2f)) - 1));
            target.addEffect(new MobEffectInstance(ModEffects.HEAVY, ticks, 0));
            Grounding.pull(target, hit.param("pull_down", 0.8f));
        }
    }

    private static final class Choke implements HeartBehavior {
        @Override
        public void hitEntity(PageHit hit, LivingEntity target) {
            if (!breathes(target)) return;
            int amplifier = Math.round(hit.param("damage_per_second", 1f) * hit.power()) - 1;
            if (target.isUnderWater()) amplifier = amplifier * 2 + 1;
            target.addEffect(new MobEffectInstance(PageRegistry.CHOKING, hit.flatTicks("choke_seconds", 5f), Math.max(0, amplifier)));
            puff(hit, target, ParticleTypes.SMOKE, 8);
        }
    }
}
