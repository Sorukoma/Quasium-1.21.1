package net.sorukoma.quasium.smokelands.sultan;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.sorukoma.quasium.particle.ModParticles;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class SultanRing extends Entity {
    public static final int COLOR = 0x8E7FA6;
    private static final int POINTS = 12;
    private static final double RADIUS = 0.8;
    private static final double REACH = 1.1;
    private static final int LIFETIME = 100;

    private final Set<UUID> hit = new HashSet<>();
    private float damage;
    @Nullable
    private SultanOfSmoke sultan;

    public SultanRing(EntityType<? extends SultanRing> type, Level level) {
        super(type, level);
        noPhysics = true;
        noCulling = true;
    }

    public static SultanRing launch(SultanOfSmoke sultan, Vec3 from, Vec3 velocity, float damage) {
        SultanRing ring = new SultanRing(Sultans.RING.get(), sultan.level());
        ring.sultan = sultan;
        ring.damage = damage;
        ring.setPos(from);
        ring.setDeltaMovement(velocity);
        ring.setYRot((float) Math.toDegrees(Math.atan2(-velocity.x, velocity.z)));
        ring.setXRot((float) Math.toDegrees(-Math.asin(Math.max(-1, Math.min(1, velocity.normalize().y)))));
        sultan.level().addFreshEntity(ring);
        return ring;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        setPos(position().add(getDeltaMovement()));
        if (level().isClientSide) {
            swirl();
            return;
        }
        if (tickCount > LIFETIME || sultan == null || !sultan.isAlive()) {
            discard();
            return;
        }
        for (Player player : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(REACH))) {
            if (player.isSpectator() || player.isCreative() || !hit.add(player.getUUID())) continue;
            player.hurt(damageSource(), damage);
        }
    }

    public DamageSource damageSource() {
        return level().damageSources().mobProjectile(this, sultan);
    }

    private void swirl() {
        Vec3 axis = Vec3.directionFromRotation(getXRot(), getYRot());
        Vec3 helper = Math.abs(axis.y) > 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 u = axis.cross(helper).normalize();
        Vec3 v = axis.cross(u).normalize();
        double phase = tickCount * 0.45;
        for (int i = 0; i < POINTS; i++) {
            double angle = phase + i * Math.PI * 2 / POINTS;
            Vec3 at = position().add(u.scale(Math.cos(angle) * RADIUS)).add(v.scale(Math.sin(angle) * RADIUS));
            level().addParticle(ModParticles.noteSmoke(COLOR), at.x, at.y, at.z, 0, 0.003, 0);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }
}
