package net.sorukoma.quasium.magic.page.effect;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.sorukoma.quasium.magic.MagicMath;
import net.sorukoma.quasium.magic.page.HeartBehavior;
import net.sorukoma.quasium.magic.page.HeartEffects;
import net.sorukoma.quasium.magic.page.PageCatalog;
import net.sorukoma.quasium.magic.page.PageEffect;
import net.sorukoma.quasium.magic.page.PageHit;

final class FireAccords {
    private static final String HEAT = "heat";

    private FireAccords() {
    }

    static void caramel(AccordHit hit, LivingEntity target) {
        if (!hit.isFoe(target) || target.fireImmune()) return;
        int ticks = hit.ticks("burn_seconds", 4f);
        target.addEffect(new MobEffectInstance(HeartRegistry.CARAMELIZED, ticks, 0));
        target.igniteForTicks(ticks);
        hit.puff(target, ParticleTypes.FALLING_HONEY, 10);
    }

    static void spreadCaramel(LivingEntity burning) {
        double reach = Accords.param("caramel", "spread_radius", 1.5f);
        int ticks = Math.round(Accords.param("caramel", "burn_seconds", 4f) * MagicMath.TICKS_PER_SECOND);
        ignite(burning, reach, ticks);
    }

    static void steam(AccordHit hit, LivingEntity target) {
        if (!hit.isFoe(target)) return;
        hit.damage(target, hit.param("damage", 2f), HeartRegistry.STEAM_BURN);
        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, hit.ticks("blind_seconds", 1f), 0));
        hit.puff(target, ParticleTypes.CLOUD, 14);
    }

    static void magma(AccordHit hit, LivingEntity target) {
        if (!hit.isFoe(target)) return;
        PageEffect heat = PageCatalog.get().effect(HEAT);
        HeartBehavior lighter = HeartEffects.get(HEAT);
        if (heat == null || lighter == null) return;
        PageHit spark = new PageHit(hit.ctx(), hit.page(), heat, hit.strength());
        ServerLevel level = hit.level();
        BlockPos feet = target.blockPosition();
        int reach = Math.round(hit.radius("radius", 1f));
        int ticks = hit.ticks("fire_seconds", 4f);
        for (BlockPos pos : BlockPos.betweenClosed(feet.offset(-reach, 0, -reach), feet.offset(reach, 0, reach))) {
            if (pos.distSqr(feet) > (double) reach * reach || !level.isEmptyBlock(pos)) continue;
            BlockPos ground = pos.immutable();
            lighter.hitBlock(spark, ground);
            if (level.getBlockState(ground).getBlock() instanceof BaseFireBlock) {
                LingeringGround.place(level, ground, Blocks.AIR.defaultBlockState(), ticks);
            }
        }
    }

    static void smolder(AccordHit hit, LivingEntity target) {
        if (!hit.isFoe(target)) return;
        int ticks = Math.max(target.getRemainingFireTicks(), hit.ticks("burn_seconds", 4f));
        target.addEffect(new MobEffectInstance(HeartRegistry.SMOLDERING, ticks, 0));
        hit.puff(target, ParticleTypes.SMOKE, 10);
        if (target.isDeadOrDying()) smolderBurst(target);
    }

    static void smolderBurst(LivingEntity dying) {
        if (!dying.hasEffect(HeartRegistry.SMOLDERING) || !dying.isOnFire()) return;
        double radius = Accords.param("smolder", "radius", 3f);
        int ticks = Math.round(Accords.param("smolder", "burn_seconds", 4f) * MagicMath.TICKS_PER_SECOND);
        ignite(dying, radius, ticks);
        if (dying.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.FLAME, dying.getX(), dying.getY(0.5), dying.getZ(), 30, radius / 2, 0.4, radius / 2, 0.05);
        }
    }

    private static void ignite(LivingEntity source, double radius, int ticks) {
        if (!(source.level() instanceof ServerLevel level)) return;
        for (LivingEntity near : Strikes.around(level, source.position(), radius, entity -> entity != source && !entity.fireImmune()
                && !(entity instanceof Player player && (player.isCreative() || player.isSpectator())))) {
            near.igniteForTicks(ticks);
        }
    }
}
