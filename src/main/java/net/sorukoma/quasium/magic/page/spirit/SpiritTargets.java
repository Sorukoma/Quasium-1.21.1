package net.sorukoma.quasium.magic.page.spirit;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.sorukoma.quasium.magic.page.CompiledPage;
import net.sorukoma.quasium.magic.page.PageCatalog;
import net.sorukoma.quasium.magic.page.PageEffect;
import net.sorukoma.quasium.magic.page.PageTarget;
import net.sorukoma.quasium.magic.page.PageTargets;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;

final class SpiritTargets {
    private static final double MIN_SHOT_STEP_SQR = 0.01;

    private SpiritTargets() {
    }

    static boolean heals(CompiledPage page) {
        if (page.hearts().isEmpty()) return false;
        PageCatalog catalog = PageCatalog.get();
        for (String id : page.hearts()) {
            PageEffect effect = catalog.effect(id);
            if (effect == null || effect.target() != PageTarget.ALLY && effect.target() != PageTarget.SUPPORT) return false;
        }
        return true;
    }

    @Nullable
    static LivingEntity lookedAt(ServerPlayer player, double reach) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(reach));
        BlockHitResult wall = player.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (wall.getType() != HitResult.Type.MISS) end = wall.getLocation();
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player.level(), player, eye, end, new AABB(eye, end).inflate(1.0),
                entity -> entity instanceof LivingEntity living && living.isAlive() && !entity.isSpectator() && !(entity instanceof HookahSpirit),
                0.3f);
        return hit == null ? null : (LivingEntity) hit.getEntity();
    }

    @Nullable
    static LivingEntity find(HookahSpirit spirit, ServerPlayer owner, @Nullable LivingEntity looked, boolean heals, double reach) {
        if (heals) return wounded(spirit, owner, looked, reach);
        if (foe(spirit, owner, looked, reach)) return looked;
        LivingEntity attacker = owner.getLastHurtByMob();
        if (foe(spirit, owner, attacker, reach)) return attacker;
        return spirit.level().getEntitiesOfClass(LivingEntity.class, spirit.getBoundingBox().inflate(reach),
                        living -> living instanceof Enemy && !ownedByAnyone(living) && foe(spirit, owner, living, reach)
                                && spirit.hasLineOfSight(living))
                .stream()
                .min(Comparator.comparingDouble(living -> living.distanceToSqr(spirit)))
                .orElse(null);
    }

    static boolean keeps(HookahSpirit spirit, ServerPlayer owner, LivingEntity target, boolean heals, double reach) {
        return heals ? woundedAlly(spirit, owner, target, reach) : foe(spirit, owner, target, reach);
    }

    @Nullable
    static Projectile incoming(ServerPlayer owner, double reach) {
        Vec3 center = owner.getBoundingBox().getCenter();
        return owner.level().getEntitiesOfClass(Projectile.class, owner.getBoundingBox().inflate(reach * 2),
                        shot -> threatens(owner, shot, center, reach))
                .stream()
                .min(Comparator.comparingDouble(shot -> shot.distanceToSqr(center)))
                .orElse(null);
    }

    @Nullable
    private static LivingEntity wounded(HookahSpirit spirit, ServerPlayer owner, @Nullable LivingEntity looked, double reach) {
        if (woundedAlly(spirit, owner, looked, reach)) return looked;
        return spirit.level().getEntitiesOfClass(LivingEntity.class, spirit.getBoundingBox().inflate(reach),
                        living -> woundedAlly(spirit, owner, living, reach))
                .stream()
                .min(Comparator.comparingDouble(living -> living.getHealth() / living.getMaxHealth()))
                .orElse(null);
    }

    private static boolean foe(HookahSpirit spirit, ServerPlayer owner, @Nullable LivingEntity target, double reach) {
        return reachable(spirit, target, reach) && PageTargets.accepts(PageTarget.FOE, owner, target);
    }

    private static boolean woundedAlly(HookahSpirit spirit, ServerPlayer owner, @Nullable LivingEntity target, double reach) {
        return reachable(spirit, target, reach) && PageTargets.isAlly(owner, target) && target.getHealth() < target.getMaxHealth();
    }

    private static boolean reachable(HookahSpirit spirit, @Nullable LivingEntity target, double reach) {
        return target != null && target.isAlive() && !target.isSpectator() && !target.isInWater() && !(target instanceof HookahSpirit)
                && target.level() == spirit.level() && target.distanceToSqr(spirit) <= reach * reach;
    }

    private static boolean ownedByAnyone(LivingEntity living) {
        return living instanceof OwnableEntity pet && pet.getOwnerUUID() != null;
    }

    private static boolean threatens(ServerPlayer owner, Projectile shot, Vec3 center, double reach) {
        if (shot.isRemoved() || shot.position().distanceToSqr(shot.xo, shot.yo, shot.zo) < MIN_SHOT_STEP_SQR) return false;
        Entity shooter = shot.getOwner();
        if (shooter instanceof LivingEntity living && PageTargets.isAlly(owner, living)) return false;
        double now = shot.position().distanceToSqr(center);
        double next = shot.position().add(shot.getDeltaMovement()).distanceToSqr(center);
        return next < now && next <= reach * reach;
    }
}
