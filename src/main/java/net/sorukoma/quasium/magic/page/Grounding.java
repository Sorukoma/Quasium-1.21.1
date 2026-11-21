package net.sorukoma.quasium.magic.page;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class Grounding {
    private static final double GROUNDED_FALL = -0.4;

    private Grounding() {
    }

    public static boolean flies(LivingEntity entity) {
        if (entity instanceof Player player) return player.getAbilities().flying || player.isFallFlying();
        return entity.isNoGravity() || !entity.onGround() && entity.getDeltaMovement().y > -0.1 && !entity.isInWater();
    }

    public static void pull(LivingEntity entity, float speed) {
        if (!flies(entity) || immune(entity)) return;
        Vec3 motion = entity.getDeltaMovement();
        entity.setDeltaMovement(motion.x, Math.min(motion.y, -speed), motion.z);
        entity.hurtMarked = true;
    }

    public static void ground(LivingEntity entity, float speed, int ticks) {
        if (immune(entity)) return;
        pull(entity, speed);
        dropFlight(entity);
        entity.addEffect(new MobEffectInstance(PageRegistry.GROUNDED, ticks, 0));
    }

    private static boolean immune(LivingEntity entity) {
        return entity instanceof Player player && (player.isCreative() || player.isSpectator());
    }

    private static void dropFlight(LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player)) return;
        if (player.isFallFlying()) player.stopFallFlying();
        if (player.getAbilities().flying) {
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
    }

    public static class GroundedEffect extends MobEffect {
        public GroundedEffect() {
            super(MobEffectCategory.HARMFUL, 0x4A3A2A);
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            return true;
        }

        @Override
        public boolean applyEffectTick(LivingEntity entity, int amplifier) {
            if (immune(entity)) return true;
            dropFlight(entity);
            if (!(entity instanceof Player) && flies(entity)) {
                Vec3 motion = entity.getDeltaMovement();
                entity.setDeltaMovement(motion.x, Math.min(motion.y, GROUNDED_FALL), motion.z);
                entity.hurtMarked = true;
            }
            return true;
        }
    }
}
