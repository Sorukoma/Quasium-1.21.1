package net.sorukoma.quasium.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;
import net.sorukoma.quasium.advancements.triggers.SmokecraftTrigger;
import net.sorukoma.quasium.magic.CastContext;
import net.sorukoma.quasium.magic.page.PageAction;
import net.sorukoma.quasium.magic.page.form.PageRingFlight;
import net.sorukoma.quasium.magic.page.form.Quiet;
import net.sorukoma.quasium.particle.ModParticles;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SmokeRingEntity extends Entity {
    public enum Kind { TRICK, SHIELD, TRAP, PORTAL, STEP, PAGE }

    private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(SmokeRingEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> KIND = SynchedEntityData.defineId(SmokeRingEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> SETTLED = SynchedEntityData.defineId(SmokeRingEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> ANCHOR = SynchedEntityData.defineId(SmokeRingEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> SILENT = SynchedEntityData.defineId(SmokeRingEntity.class, EntityDataSerializers.BOOLEAN);
    private static final Map<UUID, SmokeRingEntity> OPEN_PORTALS = new HashMap<>();
    private static final int TRICK_LIFETIME = 100;
    private static final float TRICK_DRAG = 0.93f;
    private static final double LENS_REACH = 0.9;
    private static final double SHIELD_REACH = 2.5;
    private static final int SHIELD_BREAK_TICKS = 20;
    private static final int PORTAL_COOLDOWN = 40;

    @Nullable
    private ServerPlayer owner;
    private float power = 1f;
    private double maxDistance;
    private double traveled;
    private int lifetime = TRICK_LIFETIME;
    private int rootTicks;
    private int pairCooldown;
    @Nullable
    private net.minecraft.resources.ResourceLocation spellId;
    @Nullable
    private SmokeRingEntity partner;
    @Nullable
    private PageRingFlight pageFlight;
    private final Map<UUID, Long> teleportReadyAt = new HashMap<>();

    public SmokeRingEntity(EntityType<?> type, Level level) {
        super(type, level);
        noCulling = true;
        noPhysics = true;
    }

    private static SmokeRingEntity create(ServerPlayer owner, Kind kind, int color, float speed, Vec3 direction) {
        SmokeRingEntity ring = new SmokeRingEntity(ModEntityTypes.SMOKE_RING.get(), owner.level());
        ring.owner = owner;
        ring.entityData.set(KIND, (byte) kind.ordinal());
        ring.entityData.set(COLOR, color);
        ring.setPos(owner.getEyePosition().add(direction.scale(0.6)).subtract(0, 0.15, 0));
        ring.setDeltaMovement(direction.scale(speed));
        ring.face(direction);
        return ring;
    }

    public static SmokeRingEntity trick(ServerPlayer owner, boolean perfect) {
        SmokeRingEntity ring = create(owner, Kind.TRICK, perfect ? 0xFFFFFF : 0xD8D8D8, 0.35f, owner.getLookAngle());
        ring.lifetime = TRICK_LIFETIME;
        return ring;
    }

    public static SmokeRingEntity fromSpell(CastContext ctx) {
        Kind kind = switch (ctx.spell().action()) {
            case "shield" -> Kind.SHIELD;
            case "trap" -> Kind.TRAP;
            case "portal" -> Kind.PORTAL;
            case PageAction.ID -> Kind.PAGE;
            default -> Kind.STEP;
        };
        SmokeRingEntity ring = create(ctx.caster(), kind, ctx.spell().color(), ctx.param("speed", 1f), ctx.direction());
        ring.power = ctx.power();
        ring.maxDistance = ctx.param("range", 16f);
        ring.lifetime = ctx.scaledTicks("duration", 10f);
        ring.rootTicks = ctx.scaledTicks("root_seconds", 3f);
        ring.spellId = ctx.spellId();
        ring.pairCooldown = ctx.spell().ticks("pair_cooldown", 60f);
        ring.entityData.set(SILENT, Quiet.of(ctx));
        if (kind == Kind.PAGE) ring.pageFlight = new PageRingFlight(ctx, ctx.caster());
        if (kind == Kind.SHIELD) {
            ring.settle();
            ring.entityData.set(ANCHOR, ctx.caster().getId());
        }
        return ring;
    }

    public void face(Vec3 direction) {
        setYRot((float) Math.toDegrees(Math.atan2(-direction.x, direction.z)));
        setXRot((float) Math.toDegrees(-Math.asin(Math.max(-1, Math.min(1, direction.y)))));
    }

    public Kind kind() {
        return Kind.values()[entityData.get(KIND)];
    }

    public boolean isSettled() {
        return entityData.get(SETTLED);
    }

    private void settle() {
        entityData.set(SETTLED, true);
        setDeltaMovement(Vec3.ZERO);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(COLOR, 0xDDDDDD);
        builder.define(KIND, (byte) 0);
        builder.define(SETTLED, false);
        builder.define(ANCHOR, -1);
        builder.define(SILENT, false);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            clientTick();
            return;
        }
        if (owner == null || owner.isRemoved()) {
            discard();
            return;
        }
        switch (kind()) {
            case TRICK -> tickTrick();
            case SHIELD -> tickShield();
            case TRAP -> tickFlight(this::onTrapStop);
            case STEP -> tickFlight(this::onStepStop);
            case PORTAL -> tickPortal();
            case PAGE -> {
                if (pageFlight == null || !pageFlight.tick(this)) discard();
            }
        }
    }

    private void tickTrick() {
        setPos(position().add(getDeltaMovement()));
        setDeltaMovement(getDeltaMovement().scale(TRICK_DRAG));
        if (--lifetime <= 0) discard();
    }

    private void tickShield() {
        setPos(owner.position().add(0, owner.getBbHeight() * 0.55, 0));
        for (Projectile projectile : level().getEntitiesOfClass(Projectile.class, owner.getBoundingBox().inflate(SHIELD_REACH),
                projectile -> projectile.getOwner() != owner)) {
            ((ServerLevel) level()).sendParticles(ParticleTypes.POOF, projectile.getX(), projectile.getY(), projectile.getZ(), 6, 0.1, 0.1, 0.1, 0.02);
            projectile.discard();
            lifetime -= SHIELD_BREAK_TICKS;
        }
        if (--lifetime <= 0) discard();
    }

    private void tickFlight(Runnable onStop) {
        Vec3 next = position().add(getDeltaMovement());
        BlockHitResult wall = level().clip(new ClipContext(position(), next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        LivingEntity victim = firstVictim();
        boolean blocked = wall.getType() != HitResult.Type.MISS;
        setPos(blocked ? wall.getLocation().subtract(getDeltaMovement().normalize().scale(0.4)) : next);
        traveled += getDeltaMovement().length();
        if (kind() == Kind.TRAP && victim != null) {
            root(victim);
            discard();
        } else if (blocked || victim != null || traveled >= maxDistance) {
            onStop.run();
        }
    }

    @Nullable
    private LivingEntity firstVictim() {
        return level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(0.6),
                target -> target.isAlive() && target != owner && !target.isSpectator()).stream().findFirst().orElse(null);
    }

    private void root(LivingEntity target) {
        int ticks = target.getType().is(Tags.EntityTypes.BOSSES) ? 20 : rootTicks;
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 6, false, true));
        if (target instanceof Mob mob) mob.getNavigation().stop();
        ((ServerLevel) level()).sendParticles(ModParticles.noteSmoke(entityData.get(COLOR)), target.getX(), target.getY(0.3),
                target.getZ(), 16, target.getBbWidth() / 2, 0.2, target.getBbWidth() / 2, 0.0);
    }

    private void onTrapStop() {
        discard();
    }

    private void onStepStop() {
        BlockPos feet = BlockPos.containing(position());
        for (int lift = 0; lift < 3 && !fitsAt(feet); lift++) feet = feet.above();
        owner.teleportTo(position().x, feet.getY(), position().z);
        owner.resetFallDistance();
        ((ServerLevel) level()).sendParticles(ParticleTypes.POOF, position().x, feet.getY() + 1, position().z, 20, 0.4, 0.6, 0.4, 0.02);
        SmokecraftTrigger.fire(owner, "smoke_step");
        discard();
    }

    private boolean fitsAt(BlockPos feet) {
        return level().getBlockState(feet).getCollisionShape(level(), feet).isEmpty()
                && level().getBlockState(feet.above()).getCollisionShape(level(), feet.above()).isEmpty();
    }

    private void tickPortal() {
        if (!isSettled()) {
            tickFlight(this::openPortal);
            return;
        }
        if (partner != null && partner.isAlive()) {
            long now = level().getGameTime();
            for (LivingEntity traveler : level().getEntitiesOfClass(LivingEntity.class, new AABB(position(), position()).inflate(LENS_REACH),
                    LivingEntity::isAlive)) {
                if (now < teleportReadyAt.getOrDefault(traveler.getUUID(), 0L)) continue;
                partner.teleportReadyAt.put(traveler.getUUID(), now + PORTAL_COOLDOWN);
                Vec3 exit = partner.position();
                traveler.teleportTo(exit.x, exit.y - traveler.getBbHeight() / 2, exit.z);
                traveler.resetFallDistance();
            }
        }
        if (--lifetime <= 0) discard();
    }

    private void openPortal() {
        settle();
        SmokeRingEntity waiting = OPEN_PORTALS.get(owner.getUUID());
        if (waiting != null && waiting.isAlive() && waiting.partner == null && waiting != this) {
            waiting.partner = this;
            partner = waiting;
            OPEN_PORTALS.remove(owner.getUUID());
            SmokecraftTrigger.fire(owner, "portal_pair");
            if (spellId != null) net.sorukoma.quasium.magic.SmokeCasting.setCooldown(owner, spellId, pairCooldown);
        } else {
            OPEN_PORTALS.put(owner.getUUID(), this);
        }
    }

    public static boolean lensOnSegment(Level level, ServerPlayer caster, Vec3 from, Vec3 to) {
        for (SmokeRingEntity ring : level.getEntitiesOfClass(SmokeRingEntity.class, new AABB(from, to).inflate(1.0),
                ring -> ring.owner == caster && ring.isLens())) {
            if (distanceToSegment(ring.position(), from, to) <= LENS_REACH) return true;
        }
        return false;
    }

    public static boolean lensAt(Level level, Vec3 point, @Nullable ServerPlayer caster) {
        if (caster == null) return false;
        return !level.getEntitiesOfClass(SmokeRingEntity.class, new AABB(point, point).inflate(LENS_REACH),
                ring -> ring.owner == caster && ring.isLens()).isEmpty();
    }

    private boolean isLens() {
        return kind() == Kind.TRICK || kind() == Kind.PAGE;
    }

    static double distanceToSegment(Vec3 point, Vec3 from, Vec3 to) {
        Vec3 segment = to.subtract(from);
        double lengthSqr = segment.lengthSqr();
        if (lengthSqr < 1.0E-6) return point.distanceTo(from);
        double t = Math.max(0, Math.min(1, point.subtract(from).dot(segment) / lengthSqr));
        return point.distanceTo(from.add(segment.scale(t)));
    }

    private void clientTick() {
        if (Quiet.skips(entityData.get(SILENT), tickCount)) return;
        int color = entityData.get(COLOR);
        Entity anchor = level().getEntity(entityData.get(ANCHOR));
        Vec3 center = anchor != null ? anchor.position().add(0, anchor.getBbHeight() * 0.55, 0) : position();
        if (!isSettled()) center = position().add(getDeltaMovement());
        Vec3 axis = kind() == Kind.SHIELD ? new Vec3(0, 1, 0) : Vec3.directionFromRotation(getXRot(), getYRot());
        Vec3 helper = Math.abs(axis.y) > 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 u = axis.cross(helper).normalize();
        Vec3 v = axis.cross(u).normalize();
        double radius = kind() == Kind.SHIELD ? 1.2 : kind() == Kind.PORTAL && isSettled() ? 0.9 : 0.45;
        int points = kind() == Kind.TRICK ? 6 : 10;
        double phase = tickCount * 0.35;
        for (int i = 0; i < points; i++) {
            double angle = phase + i * Math.PI * 2 / points;
            Vec3 at = center.add(u.scale(Math.cos(angle) * radius)).add(v.scale(Math.sin(angle) * radius));
            level().addParticle(ModParticles.noteSmoke(color), at.x, at.y, at.z, 0, 0.002, 0);
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
