package net.sorukoma.quasium.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.sorukoma.quasium.compat.CreateDivingProtection;

import java.util.UUID;

public class ToxicSmokeEntity extends Entity {

    private static final EntityDataAccessor<Float> DAMAGE =
            SynchedEntityData.defineId(ToxicSmokeEntity.class, EntityDataSerializers.FLOAT);

    private int lifetimeTicks = 20; // ~1 секунда жизни
    private UUID ownerUUID;

    public ToxicSmokeEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    public ToxicSmokeEntity(Level level, Player owner, Vec3 startPos, Vec3 velocity, float damage) {
        this(ModEntityTypes.TOXIC_SMOKE.get(), level);
        this.ownerUUID = owner.getUUID();
        this.setPos(startPos);
        this.setDeltaMovement(velocity);
        this.entityData.set(DAMAGE, damage);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DAMAGE, 0.0f);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.lifetimeTicks = tag.getInt("LifetimeTicks");
        if (tag.hasUUID("OwnerUUID")) {
            this.ownerUUID = tag.getUUID("OwnerUUID");
        }
        this.entityData.set(DAMAGE, tag.getFloat("Damage"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("LifetimeTicks", this.lifetimeTicks);
        if (this.ownerUUID != null) {
            tag.putUUID("OwnerUUID", this.ownerUUID);
        }
        tag.putFloat("Damage", this.entityData.get(DAMAGE));
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) {
            // Клиент: спавним частицы дыма
            for (int i = 0; i < 3; i++) {
                this.level().addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                        this.getX() + (this.random.nextDouble() - 0.5) * 0.3,
                        this.getY() + (this.random.nextDouble() - 0.5) * 0.3,
                        this.getZ() + (this.random.nextDouble() - 0.5) * 0.3,
                        this.getDeltaMovement().x * 0.1,
                        this.getDeltaMovement().y * 0.1 + 0.02,
                        this.getDeltaMovement().z * 0.1);
            }
            return;
        }

        // Сервер: двигаем снаряд
        Vec3 motion = this.getDeltaMovement();
        this.setPos(this.getX() + motion.x, this.getY() + motion.y, this.getZ() + motion.z);

        // Спавним частицы на сервере для других игроков
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    this.getX(), this.getY(), this.getZ(), 2,
                    (this.random.nextDouble() - 0.5) * 0.2,
                    0.02,
                    (this.random.nextDouble() - 0.5) * 0.2,
                    0.01);
        }

        // Проверяем столкновения с живыми существами
        float damage = this.entityData.get(DAMAGE);
        Player owner = this.ownerUUID != null ? this.level().getPlayerByUUID(this.ownerUUID) : null;

        for (LivingEntity target : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(0.5))) {
            if (target.equals(owner)) continue;
            if (target.isInvulnerable()) continue;

            DamageSource damageSource = this.level().damageSources().drown();
            float remainingDamage = CreateDivingProtection.absorbDamage(target, damage);
            if (remainingDamage > 0) {
                target.hurt(damageSource, remainingDamage);
            }
        }

        // Уменьшаем время жизни
        this.lifetimeTicks--;
        if (this.lifetimeTicks <= 0) {
            this.discard();
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 4096.0;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false; // Smoke can't be hurt
    }
}