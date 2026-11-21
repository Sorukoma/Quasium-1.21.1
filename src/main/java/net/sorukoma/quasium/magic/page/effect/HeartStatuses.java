package net.sorukoma.quasium.magic.page.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.sorukoma.quasium.magic.MagicMath;
import net.sorukoma.quasium.magic.page.PageConfig;

final class HeartStatuses {
    private static final AttributeModifier.Operation TOTAL = AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
    private static final int CARAMEL_FLARE_TICKS = 5;
    private static final double ICE_GLIDE = 0.98 / 0.6;
    private static final double ICE_GRIP = 0.216 / Math.pow(0.98, 3) - 1.0;

    private HeartStatuses() {
    }

    static final class Rooted extends MobEffect {
        private final boolean holdsInAir;

        Rooted(String name, int color, boolean holdsInAir) {
            super(MobEffectCategory.HARMFUL, color);
            this.holdsInAir = holdsInAir;
            addAttributeModifier(Attributes.MOVEMENT_SPEED, HeartRegistry.id(name + "_speed"), -1.0, TOTAL);
            addAttributeModifier(Attributes.FLYING_SPEED, HeartRegistry.id(name + "_flight"), -1.0, TOTAL);
            addAttributeModifier(Attributes.JUMP_STRENGTH, HeartRegistry.id(name + "_jump"), -1.0, TOTAL);
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            return true;
        }

        @Override
        public boolean applyEffectTick(LivingEntity entity, int amplifier) {
            Vec3 motion = entity.getDeltaMovement();
            entity.setDeltaMovement(0, holdsInAir ? 0 : motion.y, 0);
            if (entity instanceof Mob mob && !mob.level().isClientSide) mob.getNavigation().stop();
            return true;
        }
    }

    static final class Calm extends MobEffect {
        Calm() {
            super(MobEffectCategory.HARMFUL, 0x99DDFF);
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            return duration % 10 == 0;
        }

        @Override
        public boolean applyEffectTick(LivingEntity entity, int amplifier) {
            if (entity instanceof Mob mob && !mob.level().isClientSide) Strikes.forgetTarget(mob);
            return true;
        }
    }

    static final class Slippery extends MobEffect {
        Slippery() {
            super(MobEffectCategory.HARMFUL, 0xB0D8E0);
            addAttributeModifier(Attributes.MOVEMENT_SPEED, HeartRegistry.id("slippery_grip"), ICE_GRIP, TOTAL);
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            return true;
        }

        @Override
        public boolean applyEffectTick(LivingEntity entity, int amplifier) {
            if (!entity.onGround()) return true;
            Vec3 motion = entity.getDeltaMovement();
            entity.setDeltaMovement(motion.x * ICE_GLIDE, motion.y, motion.z * ICE_GLIDE);
            return true;
        }
    }

    static final class Caramel extends MobEffect {
        Caramel() {
            super(MobEffectCategory.HARMFUL, 0xE08A30);
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            return duration % CARAMEL_FLARE_TICKS == 0;
        }

        @Override
        public boolean applyEffectTick(LivingEntity entity, int amplifier) {
            if (!(entity.level() instanceof ServerLevel level) || entity.fireImmune()) return true;
            boolean wet = entity.isInWaterRainOrBubble();
            if (wet) {
                level.sendParticles(ParticleTypes.FLAME, entity.getX(), entity.getY(0.6), entity.getZ(), 6,
                        entity.getBbWidth() / 2, entity.getBbHeight() / 3, entity.getBbWidth() / 2, 0.01);
                level.sendParticles(ParticleTypes.FALLING_HONEY, entity.getX(), entity.getY(0.8), entity.getZ(), 2, 0.2, 0.2, 0.2, 0.0);
            }
            if (entity.tickCount % MagicMath.TICKS_PER_SECOND >= CARAMEL_FLARE_TICKS) return true;
            if (wet) entity.hurt(entity.damageSources().onFire(), 1f);
            entity.igniteForTicks(MagicMath.TICKS_PER_SECOND);
            FireAccords.spreadCaramel(entity);
            return true;
        }
    }

    static final class Fog extends MobEffect {
        Fog() {
            super(MobEffectCategory.HARMFUL, 0xA0A0A0);
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            return duration % 10 == 0;
        }

        @Override
        public boolean applyEffectTick(LivingEntity entity, int amplifier) {
            if (!(entity instanceof Mob mob) || mob.level().isClientSide) return true;
            LivingEntity target = mob.getTarget();
            if (target != null && !seesThroughFog(mob, target)) Strikes.forgetTarget(mob);
            return true;
        }
    }

    static boolean seesThroughFog(LivingEntity looker, LivingEntity target) {
        double sight = PageConfig.FOG_SIGHT.get();
        return looker.distanceToSqr(target) <= sight * sight;
    }

    static boolean blocksTarget(LivingEntity mob, LivingEntity target) {
        if (mob instanceof Player) return false;
        if (mob.hasEffect(HeartRegistry.CALMED)) return true;
        boolean clouded = mob.hasEffect(HeartRegistry.FOGGED) || mob.hasEffect(MobEffects.BLINDNESS);
        return clouded && !seesThroughFog(mob, target);
    }
}
