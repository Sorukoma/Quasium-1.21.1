package net.sorukoma.quasium.magic.page.form;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.sorukoma.quasium.entity.SmokeRingEntity;
import net.sorukoma.quasium.magic.CastContext;
import net.sorukoma.quasium.magic.Spell;
import net.sorukoma.quasium.magic.SpellAction;
import net.sorukoma.quasium.magic.SpellActions;
import net.sorukoma.quasium.magic.Spells;
import net.sorukoma.quasium.magic.page.PageTarget;
import net.sorukoma.quasium.magic.page.PageTargets;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class PageRingFlight {
    private static final double REACH = 1.0;
    private static final double AHEAD = 0.3;
    private static final int MAX_TICKS = 200;

    private final ServerPlayer owner;
    private final ResourceLocation spellId;
    private final float power;
    private final float homing;
    private final double range;
    private final Set<UUID> struck = new HashSet<>();
    private double traveled;

    public PageRingFlight(CastContext ctx, ServerPlayer owner) {
        this.owner = owner;
        this.spellId = ctx.spellId();
        this.power = ctx.power();
        this.homing = ctx.param("homing", 0f);
        this.range = ctx.param("range", 16f);
    }

    public boolean tick(SmokeRingEntity ring) {
        Spell spell = Spells.get(spellId);
        SpellAction action = spell == null ? null : SpellActions.get(spell.action());
        if (action == null) return false;
        CastContext ctx = new CastContext((ServerLevel) ring.level(), owner, spellId, spell, power, ring);
        if (homing > 0f) steer(ring);
        Vec3 from = ring.position();
        Vec3 to = from.add(ring.getDeltaMovement());
        BlockHitResult wall = ring.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, ring));
        Vec3 end = wall.getType() == HitResult.Type.MISS ? to : wall.getLocation();
        for (LivingEntity target : ring.level().getEntitiesOfClass(LivingEntity.class, new AABB(from, end).inflate(REACH + 1.0),
                other -> other.isAlive() && !other.isSpectator() && other != owner && !struck.contains(other.getUUID()))) {
            AABB body = target.getBoundingBox().inflate(REACH);
            if (!body.contains(from) && body.clip(from, end).isEmpty()) continue;
            struck.add(target.getUUID());
            action.hitEntity(ctx, target);
        }
        ring.setPos(end);
        traveled += from.distanceTo(end);
        return wall.getType() == HitResult.Type.MISS && traveled < range && ring.tickCount <= MAX_TICKS;
    }

    private void steer(SmokeRingEntity ring) {
        Vec3 motion = ring.getDeltaMovement();
        double speed = motion.length();
        if (speed < 1.0E-4) return;
        Vec3 heading = motion.normalize();
        LivingEntity target = ring.level().getEntitiesOfClass(LivingEntity.class, ring.getBoundingBox().inflate(FormConfig.HOMING_REACH.get()),
                        living -> living.isAlive() && !living.isSpectator() && !struck.contains(living.getUUID())
                                && PageTargets.accepts(PageTarget.FOE, owner, living)
                                && living.getBoundingBox().getCenter().subtract(ring.position()).normalize().dot(heading) > AHEAD)
                .stream()
                .min(Comparator.comparingDouble(living -> living.distanceToSqr(ring)))
                .orElse(null);
        if (target == null) return;
        Vec3 wanted = target.getBoundingBox().getCenter().subtract(ring.position()).normalize();
        double turn = Math.min(1.0, FormConfig.HOMING_TURN_PCT.getAsInt() / 100.0 * homing);
        Vec3 course = heading.scale(1 - turn).add(wanted.scale(turn)).normalize();
        ring.setDeltaMovement(course.scale(speed));
        ring.face(course);
    }
}
