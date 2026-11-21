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
import net.minecraft.world.phys.Vec3;
import net.sorukoma.quasium.compat.CreateDivingProtection;
import net.sorukoma.quasium.selection.SelectionConfig;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ToxicSmokeEntity extends Entity {
    private static final EntityDataAccessor<Float> DAMAGE = SynchedEntityData.defineId(ToxicSmokeEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> FREEZING = SynchedEntityData.defineId(ToxicSmokeEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> BURNING = SynchedEntityData.defineId(ToxicSmokeEntity.class, EntityDataSerializers.BOOLEAN);

    private final Set<UUID> hitEntities = new HashSet<>();
    private int lifetimeTicks = 24;
    private UUID ownerUUID;

    public ToxicSmokeEntity(EntityType<?> type, Level level) {
        super(type, level);
        noCulling = true;
    }

    public ToxicSmokeEntity(Level level, Player owner, Vec3 start, Vec3 velocity, float damage, boolean freezing) {
        this(ModEntityTypes.TOXIC_SMOKE.get(), level);
        ownerUUID = owner.getUUID();
        setPos(start);
        setDeltaMovement(velocity);
        entityData.set(DAMAGE, damage);
        entityData.set(FREEZING, freezing);
    }

    public void setBurning(boolean burning) {
        entityData.set(BURNING, burning);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DAMAGE, 0f);
        builder.define(FREEZING, false);
        builder.define(BURNING, false);
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 motion = getDeltaMovement();
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);

        if (level() instanceof ServerLevel serverLevel) {
            boolean burning = entityData.get(BURNING);
            if (entityData.get(FREEZING)) {
                serverLevel.sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY(), getZ(), 3, 0.12, 0.12, 0.12, 0.01);
            } else if (burning) {
                serverLevel.sendParticles(ParticleTypes.FLAME, getX(), getY(), getZ(), 3, 0.12, 0.12, 0.12, 0.01);
            } else {
                serverLevel.sendParticles(ParticleTypes.WITCH, getX(), getY(), getZ(), 3, 0.12, 0.12, 0.12, 0.01);
            }
            Player owner = ownerUUID == null ? null : level().getPlayerByUUID(ownerUUID);
            for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(0.65))) {
                if (target == owner || !hitEntities.add(target.getUUID())) continue;
                float damage = burning ? entityData.get(DAMAGE) : CreateDivingProtection.absorbDamage(target, entityData.get(DAMAGE));
                if (damage > 0f) target.hurt(burning ? level().damageSources().inFire() : level().damageSources().drown(), damage);
                if (burning) target.igniteForSeconds(SelectionConfig.ASHEN_BREATH_FIRE_SECONDS.get());
                if (entityData.get(FREEZING)) target.setTicksFrozen(Math.max(target.getTicksFrozen(), 180));
            }
        }

        if (--lifetimeTicks <= 0) discard();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        lifetimeTicks = tag.getInt("LifetimeTicks");
        if (tag.hasUUID("OwnerUUID")) ownerUUID = tag.getUUID("OwnerUUID");
        entityData.set(DAMAGE, tag.getFloat("Damage"));
        entityData.set(FREEZING, tag.getBoolean("Freezing"));
        entityData.set(BURNING, tag.getBoolean("Burning"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("LifetimeTicks", lifetimeTicks);
        if (ownerUUID != null) tag.putUUID("OwnerUUID", ownerUUID);
        tag.putFloat("Damage", entityData.get(DAMAGE));
        tag.putBoolean("Freezing", entityData.get(FREEZING));
        tag.putBoolean("Burning", entityData.get(BURNING));
    }

    @Override public boolean isPickable() { return false; }
    @Override public boolean isPushable() { return false; }
    @Override public boolean hurt(DamageSource source, float amount) { return false; }
}
