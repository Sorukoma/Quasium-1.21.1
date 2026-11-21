package net.sorukoma.quasium.magic.page.effect;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.sorukoma.quasium.magic.OreWhisper;
import net.sorukoma.quasium.magic.SpellForm;
import net.sorukoma.quasium.magic.SproutGrowth;
import net.sorukoma.quasium.magic.page.HeartBehavior;
import net.sorukoma.quasium.magic.page.PageHit;
import net.sorukoma.quasium.magic.page.PageTarget;
import net.sorukoma.quasium.magic.page.PageTargets;

final class WorldHearts {
    private static final double PUSH_PER_BLOCK = 0.2;
    private static final double DASH_PER_BLOCK = 0.3;
    private static final double DASH_LIFT = 0.2;
    private static final int GROWTH_HEIGHT = 2;

    private WorldHearts() {
    }

    private static boolean onSelf(PageHit hit) {
        return hit.page().spell().form() == SpellForm.SELF;
    }

    static final class Gust implements HeartBehavior {
        @Override
        public void hitEntity(PageHit hit, LivingEntity target) {
            float push = hit.param("push", 3f) * hit.power();
            if (onSelf(hit) && hit.ctx().isCaster(target)) {
                Vec3 dash = hit.ctx().direction().normalize().scale(push * DASH_PER_BLOCK).add(0, DASH_LIFT, 0);
                target.setDeltaMovement(target.getDeltaMovement().add(dash));
                target.hurtMarked = true;
                target.resetFallDistance();
            } else {
                Strikes.push(target, Strikes.origin(hit.ctx()), push * PUSH_PER_BLOCK);
            }
            Strikes.puff(hit.level(), hit.page(), target, ParticleTypes.CLOUD, 10);
        }
    }

    static final class Blast implements HeartBehavior {
        @Override
        public void hitEntity(PageHit hit, LivingEntity target) {
            burst(hit, target.position().add(0, target.getBbHeight() / 2, 0));
        }

        @Override
        public void hitBlock(PageHit hit, BlockPos pos) {
            burst(hit, Vec3.atCenterOf(pos));
        }

        private static void burst(PageHit hit, Vec3 center) {
            float radius = hit.param("radius", 2f) * Strikes.widened(hit.page(), "radius");
            float damage = hit.param("damage", 4f);
            float knockback = hit.param("knockback", 1.2f);
            for (LivingEntity victim : Strikes.around(hit.level(), center, radius,
                    entity -> PageTargets.accepts(PageTarget.FOE, hit.ctx().caster(), entity))) {
                hit.damage(victim, damage);
                Strikes.push(victim, center, knockback);
            }
            hit.level().sendParticles(ParticleTypes.EXPLOSION, center.x, center.y, center.z, 1, 0, 0, 0, 0);
            if (Strikes.silent(hit.page())) return;
            hit.level().playSound(null, center.x, center.y, center.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.6f, 1.5f);
        }
    }

    static final class Whisper implements HeartBehavior {
        @Override
        public void hitBlock(PageHit hit, BlockPos pos) {
            reveal(hit, Vec3.atCenterOf(pos));
        }

        @Override
        public void pulse(PageHit hit, Vec3 center, float radius) {
            reveal(hit, center);
        }

        private static void reveal(PageHit hit, Vec3 center) {
            if (hit.ctx().caster() == null) return;
            int radius = Math.round(hit.param("radius", 16f) * Strikes.widened(hit.page(), "radius"));
            OreWhisper.reveal(hit.level(), hit.ctx().caster(), center, radius, hit.ticks("glow_seconds", 10f));
        }
    }

    static final class Growth implements HeartBehavior {
        @Override
        public boolean usesBlocks() {
            return true;
        }

        @Override
        public void hitBlock(PageHit hit, BlockPos pos) {
            SproutGrowth.grow(hit.level(), pos, hit.param("crop_chance", 0.3f) * hit.power(), hit.param("sapling_chance", 0.1f) * hit.power());
        }

        @Override
        public void pulse(PageHit hit, Vec3 center, float radius) {
            if (!onSelf(hit)) return;
            BlockPos origin = BlockPos.containing(center);
            int reach = Math.round(radius);
            for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-reach, -GROWTH_HEIGHT, -reach), origin.offset(reach, GROWTH_HEIGHT, reach))) {
                if (pos.distSqr(origin) <= (double) reach * reach) hitBlock(hit, pos.immutable());
            }
        }
    }
}
