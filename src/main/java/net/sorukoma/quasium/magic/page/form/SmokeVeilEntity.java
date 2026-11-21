package net.sorukoma.quasium.magic.page.form;

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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.sorukoma.quasium.magic.CastContext;
import net.sorukoma.quasium.magic.Spell;
import net.sorukoma.quasium.magic.SpellAction;
import net.sorukoma.quasium.magic.SpellActions;
import net.sorukoma.quasium.magic.Spells;
import net.sorukoma.quasium.particle.ModParticles;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SmokeVeilEntity extends Entity {
    private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(SmokeVeilEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> WIDTH = SynchedEntityData.defineId(SmokeVeilEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> HEIGHT = SynchedEntityData.defineId(SmokeVeilEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DEPTH = SynchedEntityData.defineId(SmokeVeilEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> LURKING = SynchedEntityData.defineId(SmokeVeilEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SILENT = SynchedEntityData.defineId(SmokeVeilEntity.class, EntityDataSerializers.BOOLEAN);
    private static final int SIGHT_EVERY_TICKS = 5;
    private static final float DEFAULT_TICK_INTERVAL = 20f;
    private static final double PROJECTILE_SEARCH = 4.0;
    private static final double VEIL_SEARCH = 4.0;
    private static final double STILL = 1.0E-4;
    private static final float SMOKE_PER_BLOCK = 0.5f;

    @Nullable
    private ServerPlayer owner;
    @Nullable
    private ResourceLocation spellId;
    private float power = 1f;
    private int lifetime;
    private int lurkTicks;

    public SmokeVeilEntity(EntityType<?> type, Level level) {
        super(type, level);
        noPhysics = true;
        noCulling = true;
    }

    public static SmokeVeilEntity raise(CastContext ctx, ServerPlayer caster) {
        SmokeVeilEntity veil = new SmokeVeilEntity(PageFormRegistry.SMOKE_VEIL.get(), ctx.level());
        veil.owner = caster;
        veil.spellId = ctx.spellId();
        veil.power = ctx.power();
        veil.lifetime = ctx.scaledTicks("duration", 8f);
        veil.lurkTicks = Lurk.ticks(ctx);
        veil.entityData.set(COLOR, ctx.spell().color());
        veil.entityData.set(WIDTH, ctx.param("width", 5f));
        veil.entityData.set(HEIGHT, ctx.param("height", 3f));
        veil.entityData.set(DEPTH, FormConfig.VEIL_DEPTH.get().floatValue());
        veil.entityData.set(LURKING, veil.lurkTicks > 0);
        veil.entityData.set(SILENT, Quiet.of(ctx));
        Vec3 facing = new Vec3(ctx.direction().x, 0, ctx.direction().z);
        facing = facing.lengthSqr() < STILL ? Vec3.directionFromRotation(0f, caster.getYRot()) : facing.normalize();
        veil.setYRot((float) Math.toDegrees(Math.atan2(-facing.x, facing.z)));
        veil.setPos(caster.position().add(facing.scale(ctx.param("distance", 3f))));
        return veil;
    }

    public static boolean shelters(LivingEntity target) {
        return !target.level().getEntitiesOfClass(SmokeVeilEntity.class, target.getBoundingBox().inflate(VEIL_SEARCH),
                veil -> veil.isActive() && veil.holds(target)).isEmpty();
    }

    public static boolean hides(LivingEntity seer, LivingEntity target) {
        AABB sight = new AABB(seer.getEyePosition(), target.getEyePosition()).inflate(VEIL_SEARCH);
        return !seer.level().getEntitiesOfClass(SmokeVeilEntity.class, sight,
                veil -> veil.isActive() && veil.screens(seer, target)).isEmpty();
    }

    public float width() {
        return entityData.get(WIDTH);
    }

    public float height() {
        return entityData.get(HEIGHT);
    }

    private float depth() {
        return entityData.get(DEPTH);
    }

    public boolean isActive() {
        return !entityData.get(LURKING);
    }

    private Vec3 normal() {
        return Vec3.directionFromRotation(0f, getYRot());
    }

    private Vec3 tangent() {
        Vec3 normal = normal();
        return new Vec3(-normal.z, 0, normal.x);
    }

    private Vec3 local(Vec3 point) {
        Vec3 offset = point.subtract(position());
        return new Vec3(offset.dot(normal()), offset.dot(tangent()), offset.y);
    }

    private AABB localBox() {
        double depth = depth() / 2.0;
        double half = width() / 2.0;
        return new AABB(-depth, -half, 0, depth, half, height());
    }

    public boolean holds(Entity entity) {
        return localBox().inflate(entity.getBbWidth() / 2.0).contains(local(entity.getBoundingBox().getCenter()));
    }

    public boolean crosses(Vec3 from, Vec3 to) {
        Vec3 start = local(from);
        Vec3 end = local(to);
        AABB box = localBox();
        return box.contains(start) || box.contains(end) || box.clip(start, end).isPresent();
    }

    public boolean screens(LivingEntity seer, LivingEntity target) {
        return holds(target) || crosses(seer.getEyePosition(), target.getEyePosition());
    }

    @Override
    protected AABB makeBoundingBox() {
        Vec3 normal = normal();
        Vec3 tangent = tangent();
        double depth = depth() / 2.0;
        double half = width() / 2.0;
        double dx = Math.abs(normal.x) * depth + Math.abs(tangent.x) * half;
        double dz = Math.abs(normal.z) * depth + Math.abs(tangent.z) * half;
        return new AABB(getX() - dx, getY(), getZ() - dz, getX() + dx, getY() + height(), getZ() + dz);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (WIDTH.equals(key) || HEIGHT.equals(key) || DEPTH.equals(key)) setBoundingBox(makeBoundingBox());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(COLOR, 0xCCCCCC);
        builder.define(WIDTH, 5f);
        builder.define(HEIGHT, 3f);
        builder.define(DEPTH, 1.5f);
        builder.define(LURKING, false);
        builder.define(SILENT, false);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            spawnSmoke();
            return;
        }
        Spell spell = Spells.get(spellId);
        SpellAction action = spell == null ? null : SpellActions.get(spell.action());
        if (action == null || !(level() instanceof ServerLevel server)) {
            discard();
            return;
        }
        CastContext ctx = new CastContext(server, owner == null || owner.isRemoved() ? null : owner, spellId, spell, power, this);
        if (!isActive()) {
            lurk(ctx);
            return;
        }
        if (tickCount % Math.max(1, Math.round(ctx.param("tick_interval", DEFAULT_TICK_INTERVAL))) == 0) {
            action.cloudTick(ctx, getBoundingBox().getCenter(), width() / 2f, inside());
        }
        slowProjectiles();
        if (tickCount % SIGHT_EVERY_TICKS == 0) hideTargets();
        if (tickCount >= lifetime) discard();
    }

    private void lurk(CastContext ctx) {
        if (Lurk.foe(ctx, getBoundingBox().inflate(Lurk.reach()), position()) != null) {
            entityData.set(LURKING, false);
            tickCount = 0;
            return;
        }
        if (--lurkTicks <= 0) discard();
    }

    private List<LivingEntity> inside() {
        return level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(1.0),
                target -> target.isAlive() && !target.isSpectator() && holds(target));
    }

    private void slowProjectiles() {
        double keep = FormConfig.VEIL_PROJECTILE_KEEP_PCT.getAsInt() / 100.0;
        for (Projectile projectile : level().getEntitiesOfClass(Projectile.class, getBoundingBox().inflate(PROJECTILE_SEARCH))) {
            Vec3 motion = projectile.getDeltaMovement();
            Vec3 at = projectile.position();
            if (motion.lengthSqr() < STILL || !crosses(at.subtract(motion), at.add(motion))) continue;
            projectile.setDeltaMovement(motion.scale(keep));
            projectile.hurtMarked = true;
        }
    }

    private void hideTargets() {
        for (Mob mob : level().getEntitiesOfClass(Mob.class, getBoundingBox().inflate(FormConfig.VEIL_SIGHT_REACH.get()),
                seer -> seer.getTarget() != null)) {
            if (screens(mob, mob.getTarget())) mob.setTarget(null);
        }
    }

    private void spawnSmoke() {
        boolean faint = entityData.get(SILENT) || !isActive();
        if (Quiet.skips(faint, tickCount)) return;
        int count = faint ? 1 : Math.max(2, Math.round(width() * height() * SMOKE_PER_BLOCK));
        int color = entityData.get(COLOR);
        Vec3 across = tangent();
        Vec3 through = normal();
        for (int i = 0; i < count; i++) {
            Vec3 at = position().add(across.scale((random.nextDouble() - 0.5) * width()))
                    .add(through.scale((random.nextDouble() - 0.5) * depth()))
                    .add(0, random.nextDouble() * height(), 0);
            level().addParticle(ModParticles.noteSmoke(color), at.x, at.y, at.z, 0, 0.004, 0);
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
