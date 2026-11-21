package net.sorukoma.quasium.entity;

import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.sorukoma.quasium.Config;
import net.sorukoma.quasium.magic.CastContext;
import net.sorukoma.quasium.magic.Spell;
import net.sorukoma.quasium.magic.SpellAction;
import net.sorukoma.quasium.magic.SpellActions;
import net.sorukoma.quasium.magic.SpellForm;
import net.sorukoma.quasium.magic.Spells;
import net.sorukoma.quasium.magic.page.form.Lurk;
import net.sorukoma.quasium.magic.page.form.Quiet;
import net.sorukoma.quasium.particle.ModParticles;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class SmokeCastEntity extends Entity {
    private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(SmokeCastEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(SmokeCastEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> SETTLED = SynchedEntityData.defineId(SmokeCastEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> LURKING = SynchedEntityData.defineId(SmokeCastEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SILENT = SynchedEntityData.defineId(SmokeCastEntity.class, EntityDataSerializers.BOOLEAN);
    private static final float CONE_START_RADIUS = 0.4f;
    private static final int CONE_LIFETIME = 10;
    private static final float CLOUD_SPEED = 0.9f;
    private static final int CLOUD_TICK_INTERVAL = 5;
    private static final float DORMANT_RADIUS = 0.4f;

    @Nullable
    private ServerPlayer owner;
    @Nullable
    private ResourceLocation spellId;
    private SpellForm form = SpellForm.CONE;
    private float power = 1f;
    private float maxRadius = 1f;
    private double maxDistance;
    private double traveled;
    private int lifetime = CONE_LIFETIME;
    private int lurkTicks;
    private boolean lensed;
    private final Set<UUID> hitEntities = new HashSet<>();
    private final Set<BlockPos> hitBlocks = new HashSet<>();

    public SmokeCastEntity(EntityType<?> type, Level level) {
        super(type, level);
        noCulling = true;
        noPhysics = true;
    }

    private static SmokeCastEntity launch(CastContext ctx, SpellForm form, float speed) {
        ServerPlayer caster = ctx.caster();
        SmokeCastEntity entity = new SmokeCastEntity(ModEntityTypes.SMOKE_CAST.get(), ctx.level());
        entity.owner = caster;
        entity.spellId = ctx.spellId();
        entity.form = form;
        entity.power = ctx.power();
        entity.entityData.set(COLOR, ctx.spell().color());
        entity.entityData.set(SILENT, Quiet.of(ctx));
        if (caster != null) {
            Vec3 direction = ctx.direction();
            entity.setPos(caster.getEyePosition().add(direction.scale(0.5)).subtract(0, 0.2, 0));
            entity.setDeltaMovement(direction.scale(speed));
        }
        return entity;
    }

    public static SmokeCastEntity cone(CastContext ctx) {
        float length = ctx.param("length", 5f);
        SmokeCastEntity entity = launch(ctx, SpellForm.CONE, length / CONE_LIFETIME);
        entity.maxRadius = CONE_START_RADIUS + ctx.param("width", 2f) / 2f;
        entity.lifetime = CONE_LIFETIME;
        entity.entityData.set(RADIUS, CONE_START_RADIUS);
        return entity;
    }

    public static SmokeCastEntity cloud(CastContext ctx) {
        SmokeCastEntity entity = launch(ctx, SpellForm.CLOUD, CLOUD_SPEED);
        entity.maxDistance = ctx.param("throw", 12f);
        entity.maxRadius = ctx.param("radius", 3f);
        entity.lifetime = ctx.scaledTicks("duration", 10f);
        entity.lurkTicks = Lurk.ticks(ctx);
        entity.entityData.set(RADIUS, DORMANT_RADIUS);
        return entity;
    }

    public boolean isSettled() {
        return entityData.get(SETTLED);
    }

    public float radius() {
        return entityData.get(RADIUS);
    }

    @Nullable
    public ResourceLocation spellId() {
        return spellId;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(COLOR, 0xCCCCCC);
        builder.define(RADIUS, 0.5f);
        builder.define(SETTLED, false);
        builder.define(LURKING, false);
        builder.define(SILENT, false);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (!isSettled()) setPos(position().add(getDeltaMovement()));
            spawnSmoke();
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
        if (form == SpellForm.CLOUD) {
            tickCloud(ctx, action);
        } else {
            tickCone(ctx, action);
        }
    }

    private void tickCone(CastContext ctx, SpellAction action) {
        if (!lensed && SmokeRingEntity.lensAt(level(), position(), ctx.caster())) {
            lensed = true;
            power *= Config.RING_LENS_POWER_PCT.getAsInt() / 100f;
        }
        Vec3 next = position().add(getDeltaMovement());
        BlockHitResult wall = level().clip(new ClipContext(position(), next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        setPos(wall.getType() == HitResult.Type.MISS ? next : wall.getLocation());
        float radius = CONE_START_RADIUS + (maxRadius - CONE_START_RADIUS) * Math.min(1f, tickCount / (float) lifetime);
        entityData.set(RADIUS, radius);
        affectEntities(ctx, action, radius);
        if (action.usesBlocks(ctx)) affectBlocks(ctx, action, radius);
        if (tickCount >= lifetime || wall.getType() != HitResult.Type.MISS) discard();
    }

    private void tickCloud(CastContext ctx, SpellAction action) {
        if (!isSettled()) {
            Vec3 next = position().add(getDeltaMovement());
            BlockHitResult wall = level().clip(new ClipContext(position(), next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            boolean hitMob = !level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(0.4),
                    target -> target.isAlive() && !ctx.isCaster(target)).isEmpty();
            setPos(wall.getType() == HitResult.Type.MISS ? next : wall.getLocation());
            traveled += getDeltaMovement().length();
            if (wall.getType() != HitResult.Type.MISS || hitMob || traveled >= maxDistance) {
                entityData.set(SETTLED, true);
                entityData.set(LURKING, lurkTicks > 0);
                entityData.set(RADIUS, lurkTicks > 0 ? DORMANT_RADIUS : maxRadius);
                setDeltaMovement(Vec3.ZERO);
                tickCount = 0;
            }
            return;
        }
        if (entityData.get(LURKING)) {
            lurk(ctx);
            return;
        }
        if (tickCount % Math.max(1, Math.round(ctx.param("tick_interval", CLOUD_TICK_INTERVAL))) == 0) {
            float radius = radius();
            List<LivingEntity> inside = level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(radius),
                    target -> target.isAlive() && target.distanceToSqr(this) <= radius * radius);
            action.cloudTick(ctx, position(), radius, inside);
        }
        if (tickCount >= lifetime) discard();
    }

    private void lurk(CastContext ctx) {
        if (Lurk.foe(ctx, getBoundingBox().inflate(maxRadius), position()) != null) {
            entityData.set(LURKING, false);
            entityData.set(RADIUS, maxRadius);
            tickCount = 0;
            return;
        }
        if (--lurkTicks <= 0) discard();
    }

    private void affectEntities(CastContext ctx, SpellAction action, float radius) {
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(radius))) {
            if (!target.isAlive() || ctx.isCaster(target) || target.isSpectator()) continue;
            double reach = radius + target.getBbWidth() / 2;
            if (target.getBoundingBox().getCenter().distanceToSqr(position()) > reach * reach + target.getBbHeight()) continue;
            if (hitEntities.add(target.getUUID())) action.hitEntity(ctx, target);
        }
    }

    private void affectBlocks(CastContext ctx, SpellAction action, float radius) {
        int reach = (int) Math.ceil(radius);
        BlockPos center = blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-reach, -reach - 1, -reach), center.offset(reach, reach, reach))) {
            if (pos.distToCenterSqr(position()) > (radius + 0.5) * (radius + 0.5)) continue;
            if (hitBlocks.add(pos.immutable())) action.hitBlock(ctx, pos.immutable());
        }
    }

    private void spawnSmoke() {
        boolean faint = entityData.get(SILENT) || entityData.get(LURKING);
        if (Quiet.skips(faint, tickCount)) return;
        int color = entityData.get(COLOR);
        float radius = radius();
        int count = faint ? 1 : isSettled() ? Math.max(2, Math.round(radius * radius)) : 3;
        for (int i = 0; i < count; i++) {
            double dx = (random.nextDouble() * 2 - 1) * radius;
            double dy = (random.nextDouble() * 2 - 1) * radius * (isSettled() ? 0.6 : 1.0);
            double dz = (random.nextDouble() * 2 - 1) * radius;
            Vec3 drift = isSettled() ? Vec3.ZERO : getDeltaMovement().scale(0.3);
            level().addParticle(ModParticles.noteSmoke(color), getX() + dx, getY() + dy, getZ() + dz,
                    drift.x, drift.y + 0.01, drift.z);
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
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }
}
