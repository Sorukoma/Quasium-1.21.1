package net.sorukoma.quasium.magic;

import com.mojang.logging.LogUtils;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.sorukoma.quasium.Config;
import net.sorukoma.quasium.entity.SmokeCastEntity;
import net.sorukoma.quasium.entity.SmokeRingEntity;
import net.sorukoma.quasium.magic.page.PageConfig;
import net.sorukoma.quasium.magic.page.PageForms;
import net.sorukoma.quasium.magic.page.PageMath;
import net.sorukoma.quasium.magic.page.form.PageEcho;
import net.sorukoma.quasium.magic.page.form.Quiet;
import net.sorukoma.quasium.mind.research.Perk;
import net.sorukoma.quasium.particle.ModParticles;
import net.sorukoma.quasium.sound.ModSoundEvents;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class SpellCaster {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final float SPLIT_SPREAD_DEGREES = 15f;
    private static final double RESONATOR_REACH = 6.0;
    private static final double BEAM_MARGIN = 0.3;

    private SpellCaster() {
    }

    public static void cast(ServerPlayer caster, ResourceLocation spellId, Spell spell, float power) {
        cast(caster, spellId, spell, power, null);
    }

    public static void cast(ServerPlayer caster, ResourceLocation spellId, Spell spell, float power, @Nullable FocusUpgrade upgrade) {
        exhale(caster, spellId, spell, power, upgrade);
        if (spell.param("echo", 0f) > 0f) PageEcho.schedule(caster, spellId, spell, power, upgrade);
    }

    public static void exhale(ServerPlayer caster, ResourceLocation spellId, Spell spell, float power, @Nullable FocusUpgrade upgrade) {
        SpellAction action = SpellActions.get(spell.action());
        if (action == null) {
            LOGGER.warn("Spell {} points to unknown action {}", spellId, spell.action());
            return;
        }
        Vec3 look = caster.getLookAngle();
        boolean split = upgrade == FocusUpgrade.SPLITTER && spell.form() != SpellForm.PULSE && spell.form() != SpellForm.RING;
        int copies = Math.round(spell.param("copies", 0f));
        if (copies > 0) {
            float copyPower = power * PageConfig.FAN_COPY_POWER_PCT.getAsInt() / 100f;
            for (int i = 0; i <= copies; i++) {
                float angle = PageMath.fanAngle(i, copies, PageConfig.FAN_SPREAD_DEGREES.getAsInt());
                Vec3 direction = look.yRot((float) Math.toRadians(angle));
                fire(new CastContext(caster.serverLevel(), caster, spellId, spell, copyPower, null, upgrade, direction), action);
            }
        } else if (split) {
            float splitPower = power * Math.min(100, Perk.focusBonus(caster, Config.UPGRADE_SPLITTER_POWER_PCT.getAsInt())) / 100f;
            for (int side = -1; side <= 1; side++) {
                Vec3 direction = look.yRot((float) Math.toRadians(side * SPLIT_SPREAD_DEGREES));
                fire(new CastContext(caster.serverLevel(), caster, spellId, spell, splitPower, null, upgrade, direction), action);
            }
        } else {
            fire(new CastContext(caster.serverLevel(), caster, spellId, spell, power, null, upgrade, look), action);
        }
        if (spell.param("silent", 0f) > 0f) return;
        caster.level().playSound(null, caster.blockPosition(), exhaleSound(spell.form()).value(), SoundSource.PLAYERS,
                0.9f, 0.95f + caster.getRandom().nextFloat() * 0.1f);
    }

    private static void fire(CastContext ctx, SpellAction action) {
        ServerLevel level = ctx.level();
        switch (ctx.spell().form()) {
            case BEAM -> beam(ctx, action);
            case CONE -> level.addFreshEntity(SmokeCastEntity.cone(ctx));
            case CLOUD -> level.addFreshEntity(SmokeCastEntity.cloud(ctx));
            case RING -> level.addFreshEntity(SmokeRingEntity.fromSpell(ctx));
            case PULSE -> action.pulse(ctx, ctx.caster().position(), ctx.param("radius", 16f));
            default -> {
                if (!PageForms.launch(ctx, action)) LOGGER.warn("Spell {} has form {} without a launcher", ctx.spellId(), ctx.spell().form());
            }
        }
    }

    private static Holder<SoundEvent> exhaleSound(SpellForm form) {
        return switch (form) {
            case BEAM -> ModSoundEvents.CAST_EXHALE_BEAM;
            case CLOUD -> ModSoundEvents.CAST_EXHALE_CLOUD;
            case RING -> ModSoundEvents.RING_RELEASE;
            default -> ModSoundEvents.CAST_EXHALE_CONE;
        };
    }

    private static void beam(CastContext ctx, SpellAction action) {
        ServerPlayer caster = ctx.caster();
        ServerLevel level = ctx.level();
        Vec3 from = caster.getEyePosition();
        Vec3 to = from.add(ctx.direction().scale(ctx.param("range", 24f)));
        BlockHitResult blockHit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        Vec3 end = blockHit.getType() == HitResult.Type.MISS ? to : blockHit.getLocation();
        if (SmokeRingEntity.lensOnSegment(level, caster, from, end)) {
            ctx = ctx.withPower(ctx.power() * Config.RING_LENS_POWER_PCT.getAsInt() / 100f);
        }
        int limit = 1 + Math.round(ctx.param("pierce", 0f));
        List<EntityHitResult> hits = beamHits(level, caster, from, end, limit);
        for (EntityHitResult hit : hits) action.hitEntity(ctx, (LivingEntity) hit.getEntity());
        if (!hits.isEmpty() && ctx.upgrade() == FocusUpgrade.RESONATOR) resonate(ctx, action, (LivingEntity) hits.getFirst().getEntity());
        if (hits.size() == limit) {
            end = hits.getLast().getLocation();
        } else if (blockHit.getType() == HitResult.Type.BLOCK) {
            action.impactBlock(ctx, blockHit.getBlockPos().relative(blockHit.getDirection()));
        }
        trail(ctx, from, end);
    }

    private static List<EntityHitResult> beamHits(ServerLevel level, ServerPlayer caster, Vec3 from, Vec3 end, int limit) {
        List<EntityHitResult> hits = new ArrayList<>();
        for (Entity target : level.getEntities(caster, new AABB(from, end).inflate(1.0),
                candidate -> candidate instanceof LivingEntity living && living.isAlive() && !candidate.isSpectator())) {
            target.getBoundingBox().inflate(BEAM_MARGIN).clip(from, end).ifPresent(at -> hits.add(new EntityHitResult(target, at)));
        }
        return hits.stream()
                .sorted(Comparator.comparingDouble(hit -> hit.getLocation().distanceToSqr(from)))
                .limit(limit)
                .toList();
    }

    private static void resonate(CastContext ctx, SpellAction action, LivingEntity first) {
        ctx.level().getEntitiesOfClass(LivingEntity.class, first.getBoundingBox().inflate(RESONATOR_REACH),
                        other -> other != first && other.isAlive() && !ctx.isCaster(other) && !other.isSpectator())
                .stream()
                .min(Comparator.comparingDouble(other -> other.distanceToSqr(first)))
                .ifPresent(second -> {
                    int pct = Math.min(100, Perk.focusBonus(ctx.caster(), Config.UPGRADE_RESONATOR_POWER_PCT.getAsInt()));
                    action.hitEntity(ctx.withPower(ctx.power() * pct / 100f), second);
                    trail(ctx, first.getEyePosition(), second.getEyePosition());
                });
    }

    private static void trail(CastContext ctx, Vec3 from, Vec3 end) {
        Vec3 step = end.subtract(from);
        int points = Math.max(1, (int) (step.length() * 2));
        int stride = Quiet.stride(Quiet.of(ctx));
        for (int i = 1; i <= points; i += stride) {
            Vec3 point = from.add(step.scale(i / (double) points));
            ctx.level().sendParticles(ModParticles.noteSmoke(ctx.spell().color()), point.x, point.y - 0.1, point.z,
                    1, 0.03, 0.03, 0.03, 0.0);
        }
    }
}
