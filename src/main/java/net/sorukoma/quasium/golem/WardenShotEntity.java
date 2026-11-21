package net.sorukoma.quasium.golem;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.sorukoma.quasium.particle.ModParticles;

public class WardenShotEntity extends ThrowableProjectile {
    private static final int LIFETIME = 40;
    private static final int TRAIL_PUFFS = 3;

    public WardenShotEntity(EntityType<? extends WardenShotEntity> type, Level level) {
        super(type, level);
    }

    WardenShotEntity(SmokeWardenEntity warden, Vec3 muzzle) {
        super(WardenGolems.WARDEN_SHOT.get(), muzzle.x, muzzle.y, muzzle.z, warden.level());
        setOwner(warden);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected double getDefaultGravity() {
        return 0;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            Vec3 back = getDeltaMovement().scale(-1.0 / TRAIL_PUFFS);
            for (int i = 0; i < TRAIL_PUFFS; i++) {
                level().addParticle(ModParticles.noteSmoke(SmokeWardenEntity.SMOKE_COLOR),
                        getX() + back.x * i, getY() + back.y * i, getZ() + back.z * i, 0, 0.01, 0);
            }
        } else if (tickCount > LIFETIME) {
            discard();
        }
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && getOwner() instanceof SmokeWardenEntity warden && warden.isEnemy(target);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (!level().isClientSide && getOwner() instanceof SmokeWardenEntity warden && result.getEntity() instanceof LivingEntity target) {
            warden.strike(this, target);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ModParticles.noteSmoke(SmokeWardenEntity.SMOKE_COLOR), getX(), getY(), getZ(), 6, 0.15, 0.15, 0.15, 0.01);
            discard();
        }
    }
}
