package net.sorukoma.quasium.magic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.stats.Stats;
import net.sorukoma.quasium.effect.ModEffects;
import net.sorukoma.quasium.entity.SmokeCastEntity;
import net.sorukoma.quasium.magic.page.PageAction;
import net.sorukoma.quasium.magic.page.form.SmokeVeilEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public final class SpellActions {
    private static final SpellAction RING_DRIVEN = new SpellAction() {
    };

    private static final Map<String, SpellAction> ACTIONS = Map.ofEntries(
            Map.entry("frost", new Frost()),
            Map.entry("ember", new Ember()),
            Map.entry("screen", new Screen()),
            Map.entry("sprout", new Sprout()),
            Map.entry("whisper", new Whisper()),
            Map.entry("shield", RING_DRIVEN),
            Map.entry("trap", RING_DRIVEN),
            Map.entry("portal", RING_DRIVEN),
            Map.entry("step", RING_DRIVEN),
            Map.entry("heavy", new Heavy()),
            Map.entry("dream", new Dream()),
            Map.entry("fume", new Fume()),
            Map.entry("delirium", new Delirium()),
            Map.entry("delocalize", new Delocalize()),
            Map.entry("healing", new Healing()),
            Map.entry(PageAction.ID, PageAction.INSTANCE));

    private SpellActions() {
    }

    @Nullable
    public static SpellAction get(String id) {
        return ACTIONS.get(id);
    }

    private static final class Frost implements SpellAction {
        @Override
        public void hitEntity(CastContext ctx, LivingEntity target) {
            float damage = ctx.param("damage", 2f);
            if (target.fireImmune() || target.getType().is(EntityTypeTags.FREEZE_HURTS_EXTRA_TYPES)) {
                damage *= ctx.param("fire_mob_multiplier", 2f);
            }
            ctx.damage(target, damage);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ctx.scaledTicks("slow_seconds", 4f),
                    Math.round(ctx.param("slow_level", 3f)) - 1));
            if (target.canFreeze()) {
                int frozen = target.getTicksRequiredToFreeze() + ctx.scaledTicks("freeze_seconds", 3f) * 2;
                target.setTicksFrozen(Math.max(target.getTicksFrozen(), frozen));
            }
            ctx.level().sendParticles(ParticleTypes.SNOWFLAKE, target.getX(), target.getY(0.6), target.getZ(),
                    10, target.getBbWidth() / 2, target.getBbHeight() / 3, target.getBbWidth() / 2, 0.02);
        }
    }

    private static final class Ember implements SpellAction {
        @Override
        public void hitEntity(CastContext ctx, LivingEntity target) {
            ctx.damage(target, ctx.param("damage", 3f));
            target.igniteForTicks(ctx.scaledTicks("burn_seconds", 4f));
        }

        @Override
        public void hitBlock(CastContext ctx, BlockPos pos) {
            ServerLevel level = ctx.level();
            if (!level.isEmptyBlock(pos) || !BaseFireBlock.canBePlacedAt(level, pos, Direction.UP)) return;
            for (Direction side : Direction.values()) {
                BlockPos neighbour = pos.relative(side);
                if (level.getBlockState(neighbour).isFlammable(level, neighbour, side.getOpposite())) return;
            }
            level.setBlockAndUpdate(pos, BaseFireBlock.getState(level, pos));
        }
    }

    private static final class Screen implements SpellAction {
        @Override
        public void cloudTick(CastContext ctx, Vec3 center, float radius, List<LivingEntity> inside) {
            int blindTicks = ctx.scaledTicks("blind_seconds", 2f);
            for (LivingEntity entity : inside) {
                if (ctx.isCaster(entity)) continue;
                if (entity instanceof Mob mob) mob.setTarget(null);
                entity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, blindTicks, 0));
            }
        }
    }

    private static final class Sprout implements SpellAction {
        @Override
        public boolean usesBlocks() {
            return true;
        }

        @Override
        public void hitBlock(CastContext ctx, BlockPos pos) {
            SproutGrowth.grow(ctx.level(), pos, ctx.param("crop_chance", 0.3f) * ctx.power(), ctx.param("sapling_chance", 0.1f) * ctx.power());
        }
    }

    private static final class Whisper implements SpellAction {
        @Override
        public void pulse(CastContext ctx, Vec3 center, float radius) {
            if (ctx.caster() == null) return;
            OreWhisper.reveal(ctx.level(), ctx.caster(), center, Math.round(radius), ctx.scaledTicks("glow_seconds", 10f));
        }
    }

    private static final class Heavy implements SpellAction {
        @Override
        public void cloudTick(CastContext ctx, Vec3 center, float radius, List<LivingEntity> inside) {
            int ticks = ctx.scaledTicks("effect_seconds", 2f);
            int slowLevel = Math.round(ctx.param("slow_level", 2f)) - 1;
            for (LivingEntity entity : inside) {
                if (ctx.isCaster(entity)) continue;
                entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, slowLevel));
                entity.addEffect(new MobEffectInstance(ModEffects.HEAVY, ticks, 0));
            }
        }
    }

    private static final class Dream implements SpellAction {
        @Override
        public void hitEntity(CastContext ctx, LivingEntity target) {
            if (target instanceof Player player) {
                player.resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_REST));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ctx.scaledTicks("player_seconds", 3f), 0));
                return;
            }
            target.addEffect(new MobEffectInstance(ModEffects.DROWSY, ctx.scaledTicks("sleep_seconds", 8f), 0));
            if (target instanceof Mob mob) {
                mob.setTarget(null);
                mob.getNavigation().stop();
            }
            ctx.level().sendParticles(ParticleTypes.NOTE, target.getX(), target.getY(1.1), target.getZ(), 1, 0, 0, 0, 0.0);
        }
    }

    private static final class Fume implements SpellAction {
        @Override
        public void hitEntity(CastContext ctx, LivingEntity target) {
            target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, ctx.scaledTicks("nausea_seconds", 6f), 0));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ctx.scaledTicks("weakness_seconds", 8f), 0));
        }
    }

    private static final class Delirium implements SpellAction {
        @Override
        public void cloudTick(CastContext ctx, Vec3 center, float radius, List<LivingEntity> inside) {
            for (LivingEntity entity : inside) {
                if (ctx.isCaster(entity)) continue;
                if (entity instanceof Player) {
                    entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, ctx.scaledTicks("trip_seconds", 20f), 0));
                } else if (entity instanceof Mob mob) {
                    mob.setTarget(null);
                    mob.getLookControl().setLookAt(mob.getX() + ctx.level().random.nextGaussian() * 4, mob.getEyeY(),
                            mob.getZ() + ctx.level().random.nextGaussian() * 4);
                }
            }
        }
    }

    private static final class Delocalize implements SpellAction {
        @Override
        public void hitEntity(CastContext ctx, LivingEntity target) {
            ScatterTeleport.scatter(ctx.level(), target, ctx.param("scatter", 12f) * ctx.power());
        }
    }

    private static final class Healing implements SpellAction {
        @Override
        public void cloudTick(CastContext ctx, Vec3 center, float radius, List<LivingEntity> inside) {
            int ticks = ctx.scaledTicks("effect_seconds", 3f);
            for (LivingEntity entity : inside) {
                if (entity.getType().is(net.minecraft.tags.EntityTypeTags.UNDEAD)) continue;
                entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, ticks, 0));
            }
        }
    }

    public static boolean hiddenBySmoke(LivingEntity target) {
        if (!(target.level() instanceof ServerLevel level)) return false;
        if (SmokeVeilEntity.shelters(target)) return true;
        AABB area = target.getBoundingBox().inflate(8.0);
        for (SmokeCastEntity cloud : level.getEntitiesOfClass(SmokeCastEntity.class, area, SmokeCastEntity::isSettled)) {
            Spell spell = Spells.get(cloud.spellId());
            if (spell == null || !"screen".equals(spell.action())) continue;
            if (cloud.position().distanceToSqr(target.position()) <= cloud.radius() * cloud.radius()) return true;
        }
        return false;
    }

    public static boolean isPlayerInScreen(LivingEntity target) {
        return target instanceof Player && hiddenBySmoke(target);
    }
}
