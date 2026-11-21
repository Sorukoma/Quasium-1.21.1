package net.sorukoma.quasium.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.sorukoma.quasium.Config;
import net.sorukoma.quasium.particle.ModParticles;

public class SmokeTrickEntity extends Entity {
    public enum Kind { GHOST, JELLYFISH, TORNADO }

    private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(SmokeTrickEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> KIND = SynchedEntityData.defineId(SmokeTrickEntity.class, EntityDataSerializers.BYTE);
    private static final double GHOST_DISTANCE = 1.4;
    private static final double JELLY_SPEED = 0.15;
    private static final double JELLY_DRAG = 0.9;
    private static final double SINK = 0.02;
    private static final double DRIFT = 0.01;
    private static final double FLOW_PUSH = 0.05;

    private int lifetime;
    private Vec3 drift = Vec3.ZERO;

    public SmokeTrickEntity(EntityType<?> type, Level level) {
        super(type, level);
        noCulling = true;
        noPhysics = true;
    }

    private static SmokeTrickEntity create(ServerPlayer owner, Kind kind, int color, Vec3 at, int seconds) {
        SmokeTrickEntity trick = new SmokeTrickEntity(ModEntityTypes.SMOKE_TRICK.get(), owner.level());
        trick.entityData.set(KIND, (byte) kind.ordinal());
        trick.entityData.set(COLOR, color);
        trick.lifetime = seconds * 20;
        trick.setPos(at);
        return trick;
    }

    public static SmokeTrickEntity ghost(ServerPlayer owner, int color) {
        return create(owner, Kind.GHOST, color, owner.getEyePosition().add(owner.getLookAngle().scale(GHOST_DISTANCE)),
                Config.TRICK_GHOST_SEC.getAsInt());
    }

    public static SmokeTrickEntity jellyfish(ServerPlayer owner, boolean perfect) {
        Vec3 look = owner.getLookAngle();
        SmokeTrickEntity jelly = create(owner, Kind.JELLYFISH, perfect ? 0xFFFFFF : 0xD8D8D8, owner.getEyePosition().add(look.scale(0.6)),
                Config.TRICK_JELLYFISH_SEC.getAsInt());
        jelly.setDeltaMovement(look.scale(JELLY_SPEED));
        jelly.drift = Vec3.directionFromRotation(0, owner.getYRot()).scale(DRIFT);
        return jelly;
    }

    public static SmokeTrickEntity tornado(ServerPlayer owner, Vec3 table, int color) {
        return create(owner, Kind.TORNADO, color, table, Config.TRICK_TORNADO_SEC.getAsInt());
    }

    public Kind kind() {
        return Kind.values()[entityData.get(KIND)];
    }

    public void inhale(ServerPlayer smoker) {
        Vec3 path = smoker.getEyePosition().subtract(0, 0.1, 0).subtract(position());
        ColorParticleOption smoke = ModParticles.noteSmoke(entityData.get(COLOR));
        for (int step = 0; step <= 6; step++) {
            Vec3 at = position().add(path.scale(step / 6.0));
            ((ServerLevel) level()).sendParticles(smoke, at.x, at.y, at.z, 2, 0.04, 0.04, 0.04, 0.0);
        }
        discard();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(COLOR, 0xDDDDDD);
        builder.define(KIND, (byte) 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            puff(ModParticles.noteSmoke(entityData.get(COLOR)));
            return;
        }
        if (--lifetime <= 0) {
            discard();
            return;
        }
        if (kind() == Kind.JELLYFISH) swim();
    }

    private void swim() {
        BlockPos at = blockPosition();
        FluidState water = level().getFluidState(at);
        if (water.is(FluidTags.WATER)) {
            Vec3 push = water.getFlow(level(), at).scale(FLOW_PUSH).add(drift);
            setPos(getX() + push.x, at.getY() + water.getHeight(level(), at), getZ() + push.z);
            return;
        }
        Vec3 motion = getDeltaMovement().scale(JELLY_DRAG);
        motion = new Vec3(motion.x, Math.min(motion.y, -SINK), motion.z);
        BlockPos next = BlockPos.containing(position().add(motion));
        if (!level().getBlockState(next).getCollisionShape(level(), next).isEmpty()) motion = Vec3.ZERO;
        setDeltaMovement(motion);
        setPos(position().add(motion));
    }

    private void puff(ColorParticleOption smoke) {
        double phase = tickCount * 0.25;
        switch (kind()) {
            case GHOST -> {
                for (int i = 0; i < 6; i++) {
                    level().addParticle(smoke, getX() + random.nextGaussian() * 0.1, getY() + random.nextGaussian() * 0.1,
                            getZ() + random.nextGaussian() * 0.1, 0, 0.002, 0);
                }
            }
            case JELLYFISH -> {
                double bell = 0.3 + 0.05 * Math.sin(phase);
                for (int i = 0; i < 6; i++) {
                    double angle = phase * 0.3 + i * Math.PI / 3;
                    double x = Math.cos(angle);
                    double z = Math.sin(angle);
                    level().addParticle(smoke, getX() + x * bell, getY(), getZ() + z * bell, 0, 0.002, 0);
                    double sway = Math.sin(phase + i) * 0.05;
                    level().addParticle(smoke, getX() + x * 0.15 + sway, getY() - 0.25 - random.nextDouble() * 0.35, getZ() + z * 0.15 + sway,
                            0, -0.01, 0);
                }
            }
            case TORNADO -> {
                for (int i = 0; i < 10; i++) {
                    double height = i * 0.12;
                    double radius = 0.06 + height * 0.3;
                    double angle = phase * 2 + i * 0.8;
                    level().addParticle(smoke, getX() + Math.cos(angle) * radius, getY() + height, getZ() + Math.sin(angle) * radius,
                            -Math.sin(angle) * 0.03, 0.01, Math.cos(angle) * 0.03);
                }
            }
        }
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

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }
}
