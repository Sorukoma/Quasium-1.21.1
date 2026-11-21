package net.sorukoma.quasium.magic.page;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.sorukoma.quasium.magic.CastContext;
import net.sorukoma.quasium.magic.Spell;
import net.sorukoma.quasium.magic.SpellAction;
import net.sorukoma.quasium.magic.SpellActions;
import net.sorukoma.quasium.magic.Spells;
import net.sorukoma.quasium.magic.page.form.Lurk;
import net.sorukoma.quasium.magic.page.form.Quiet;
import net.sorukoma.quasium.particle.ModParticles;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class SmokeBoltEntity extends Entity {
    private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(SmokeBoltEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> SILENT = SynchedEntityData.defineId(SmokeBoltEntity.class, EntityDataSerializers.BOOLEAN);
    private static final double HOMING_REACH = 10.0;
    private static final double HOMING_TURN_PER_LEVEL = 0.12;

    @Nullable
    private ServerPlayer owner;
    @Nullable
    private ResourceLocation spellId;
    private float power = 1f;
    private double maxDistance = 32;
    private double traveled;
    private int pierceLeft;
    private float homing;
    private float speed;
    private int lurkTicks;
    private boolean parked;
    private final Set<UUID> struck = new HashSet<>();

    public SmokeBoltEntity(EntityType<?> type, Level level) {
        super(type, level);
        noPhysics = true;
        noCulling = true;
    }

    public static SmokeBoltEntity launch(CastContext ctx) {
        SmokeBoltEntity bolt = new SmokeBoltEntity(PageRegistry.SMOKE_BOLT.get(), ctx.level());
        bolt.owner = ctx.caster();
        bolt.spellId = ctx.spellId();
        bolt.power = ctx.power();
        bolt.maxDistance = ctx.param("range", 32f);
        bolt.pierceLeft = Math.round(ctx.param("pierce", 0f));
        bolt.homing = ctx.param("homing", 0f);
        bolt.speed = ctx.param("speed", 1.2f);
        bolt.lurkTicks = Lurk.ticks(ctx);
        bolt.entityData.set(COLOR, ctx.spell().color());
        bolt.entityData.set(SILENT, Quiet.of(ctx));
        Vec3 direction = ctx.direction();
        if (ctx.caster() != null) bolt.setPos(ctx.caster().getEyePosition().add(direction.scale(0.6)).subtract(0, 0.15, 0));
        bolt.setDeltaMovement(direction.normalize().scale(bolt.speed));
        return bolt;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(COLOR, 0xCCCCCC);
        builder.define(SILENT, false);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            setPos(position().add(getDeltaMovement()));
            trail();
            return;
        }
        Spell spell = Spells.get(spellId);
        SpellAction action = spell == null ? null : SpellActions.get(spell.action());
        if (action == null || !(level() instanceof ServerLevel serverLevel)) {
            discard();
            return;
        }
        ServerPlayer caster = owner == null || owner.isRemoved() ? null : owner;
        CastContext ctx = new CastContext(serverLevel, caster, spellId, spell, power, this);
        if (parked) {
            lurk(ctx);
            return;
        }
        if (homing > 0f) steer(caster);
        fly(ctx, action);
    }

    private void fly(CastContext ctx, SpellAction action) {
        Vec3 from = position();
        Vec3 to = from.add(getDeltaMovement());
        BlockHitResult wall = level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        Vec3 end = wall.getType() == HitResult.Type.MISS ? to : wall.getLocation();
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(level(), this, from, end, new AABB(from, end).inflate(0.6),
                target -> target instanceof LivingEntity living && living.isAlive() && !target.isSpectator()
                        && !ctx.isCaster(target) && !struck.contains(target.getUUID()), 0.3f);
        if (entityHit != null) {
            LivingEntity target = (LivingEntity) entityHit.getEntity();
            struck.add(target.getUUID());
            action.hitEntity(ctx, target);
            pop(entityHit.getLocation());
            if (pierceLeft-- <= 0) {
                discard();
                return;
            }
        }
        setPos(end);
        traveled += from.distanceTo(end);
        if (wall.getType() == HitResult.Type.BLOCK) {
            if (park()) return;
            action.impactBlock(ctx, wall.getBlockPos().relative(wall.getDirection()));
            pop(end);
            discard();
            return;
        }
        if ((traveled >= maxDistance || tickCount > 200) && !park()) discard();
    }

    private boolean park() {
        if (lurkTicks <= 0) return false;
        parked = true;
        setDeltaMovement(Vec3.ZERO);
        return true;
    }

    private void lurk(CastContext ctx) {
        LivingEntity foe = Lurk.foe(ctx, getBoundingBox().inflate(Lurk.reach()), position());
        if (foe != null) {
            parked = false;
            lurkTicks = 0;
            traveled = 0;
            tickCount = 0;
            setDeltaMovement(foe.getBoundingBox().getCenter().subtract(position()).normalize().scale(speed));
            return;
        }
        if (--lurkTicks > 0) return;
        pop(position());
        discard();
    }

    private void steer(@Nullable ServerPlayer caster) {
        Vec3 motion = getDeltaMovement();
        double speed = motion.length();
        if (speed < 1.0E-4) return;
        Vec3 heading = motion.normalize();
        LivingEntity target = level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(HOMING_REACH),
                        living -> living.isAlive() && !living.isSpectator() && !struck.contains(living.getUUID())
                                && PageTargets.accepts(PageTarget.FOE, caster, living)
                                && living.getEyePosition().subtract(position()).normalize().dot(heading) > 0.3)
                .stream()
                .min(Comparator.comparingDouble(living -> living.distanceToSqr(this)))
                .orElse(null);
        if (target == null) return;
        Vec3 wanted = target.getBoundingBox().getCenter().subtract(position()).normalize();
        double turn = Math.min(1.0, HOMING_TURN_PER_LEVEL * homing);
        setDeltaMovement(heading.scale(1 - turn).add(wanted.scale(turn)).normalize().scale(speed));
    }

    private void pop(Vec3 at) {
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ModParticles.noteSmoke(entityData.get(COLOR)), at.x, at.y, at.z, 12, 0.4, 0.4, 0.4, 0.02);
            server.sendParticles(ParticleTypes.POOF, at.x, at.y, at.z, 4, 0.2, 0.2, 0.2, 0.02);
        }
    }

    private void trail() {
        if (Quiet.skips(entityData.get(SILENT), tickCount)) return;
        int color = entityData.get(COLOR);
        for (int i = 0; i < 3; i++) {
            Vec3 back = getDeltaMovement().scale(-i / 3.0);
            level().addParticle(ModParticles.noteSmoke(color), getX() + back.x, getY() + back.y, getZ() + back.z, 0, 0.005, 0);
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
