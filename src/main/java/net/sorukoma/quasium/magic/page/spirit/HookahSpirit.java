package net.sorukoma.quasium.magic.page.spirit;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.sorukoma.quasium.magic.CastContext;
import net.sorukoma.quasium.magic.NoteType;
import net.sorukoma.quasium.magic.page.CompiledPage;
import net.sorukoma.quasium.magic.page.PageConfig;
import net.sorukoma.quasium.magic.page.PageSpells;
import net.sorukoma.quasium.magic.page.PageStrike;
import net.sorukoma.quasium.magic.page.PageTarget;
import net.sorukoma.quasium.magic.page.PageTargets;
import net.sorukoma.quasium.particle.ModParticles;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class HookahSpirit extends Mob implements OwnableEntity {
    public static final byte WAITING = 0;
    public static final byte HUNTING = 1;
    public static final byte STRUCK = 2;
    public static final byte SLEEPING = 3;
    private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(HookahSpirit.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> FACE = SynchedEntityData.defineId(HookahSpirit.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> MOOD = SynchedEntityData.defineId(HookahSpirit.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> SILENT = SynchedEntityData.defineId(HookahSpirit.class, EntityDataSerializers.BOOLEAN);
    private static final int SEARCH_EVERY_TICKS = 5;
    private static final int STRUCK_TICKS = 6;
    private static final int DOZE_AFTER_TICKS = 60;
    private static final double LAUNCH_SLOWDOWN = 0.55;
    private static final double ORBIT_SPEED = 0.12;
    private static final double TEASE_DISTANCE = 1.6;
    private static final double HIT_MARGIN = 0.3;
    private static final double KEEP_REACH_SCALE = 1.5;
    private static final double POINT_BLANK = 3.0;

    private enum Phase { LAUNCH, GUARD, SEEK, CHASE, TEASE, STRUCK }

    @Nullable
    private ServerPlayer owner;
    @Nullable
    private UUID ownerId;
    @Nullable
    private SpiritOrders orders;
    @Nullable
    private LivingEntity looked;
    @Nullable
    private LivingEntity target;
    private Phase phase = Phase.LAUNCH;
    private Vec3 heading = new Vec3(0, 0, 1);
    private double orbitOffset;
    private int phaseTicks;
    private int waited;
    private float age;
    private boolean teased;

    public HookahSpirit(EntityType<? extends HookahSpirit> type, Level level) {
        super(type, level);
        setNoGravity(true);
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 1.0);
    }

    static HookahSpirit breathe(ServerPlayer owner, SpiritOrders orders, @Nullable LivingEntity looked, Vec3 mouth, Vec3 heading,
                                double orbitOffset) {
        HookahSpirit spirit = new HookahSpirit(SpiritRegistry.HOOKAH_SPIRIT.get(), owner.level());
        spirit.owner = owner;
        spirit.ownerId = owner.getUUID();
        spirit.orders = orders;
        spirit.looked = looked;
        spirit.heading = heading.normalize();
        spirit.orbitOffset = orbitOffset;
        spirit.setPos(mouth.x, mouth.y - spirit.getBbHeight() / 2, mouth.z);
        spirit.faceHeading();
        spirit.entityData.set(COLOR, orders.color());
        spirit.entityData.set(FACE, orders.face());
        spirit.entityData.set(SILENT, orders.silent());
        return spirit;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(COLOR, 0xCCCCCC);
        builder.define(FACE, (byte) 0);
        builder.define(MOOD, WAITING);
        builder.define(SILENT, false);
    }

    public int color() {
        return entityData.get(COLOR);
    }

    public int face() {
        return entityData.get(FACE);
    }

    public byte mood() {
        return entityData.get(MOOD);
    }

    @Nullable
    @Override
    public UUID getOwnerUUID() {
        return ownerId;
    }

    public boolean serves(Player player) {
        return player.getUUID().equals(ownerId);
    }

    public boolean popping() {
        return phase == Phase.STRUCK;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide && tickCount % (entityData.get(SILENT) ? 8 : 2) == 0) {
            level().addParticle(ModParticles.noteSmoke(color()), getRandomX(0.3), getY(0.5), getRandomZ(0.3), 0, 0.01, 0);
        }
    }

    @Override
    protected void customServerAiStep() {
        if (!(level() instanceof ServerLevel server)) return;
        if (phase == Phase.STRUCK) {
            setDeltaMovement(Vec3.ZERO);
            if (++phaseTicks >= STRUCK_TICKS) discard();
            return;
        }
        CompiledPage page = orders == null ? null : PageSpells.get(orders.spellId());
        if (page == null || owner == null || owner.isRemoved() || owner.level() != level()) {
            fade(server);
            return;
        }
        if (phase != Phase.GUARD) age += SpiritMath.ageStep(level().isRainingAt(blockPosition()), PageConfig.SPIRIT_RAIN_LIFE_PCT.getAsInt());
        if (age >= orders.lifeTicks()) {
            fade(server);
            return;
        }
        phaseTicks++;
        switch (phase) {
            case LAUNCH -> launch(owner, orders);
            case GUARD -> guard(server, owner, orders);
            case SEEK -> seek(owner, orders);
            case CHASE -> chase(server, page, owner, orders);
            case TEASE -> tease(owner, orders);
        }
        if (phase == Phase.STRUCK) return;
        keepOutOfWater(orders);
        faceHeading();
        entityData.set(MOOD, currentMood(orders));
    }

    private void launch(ServerPlayer owner, SpiritOrders orders) {
        LivingEntity close = orders.guardTicks() > 0 ? null : SpiritTargets.find(this, owner, looked, orders.heals(), POINT_BLANK);
        if (close != null) {
            hunt(close, orders);
            return;
        }
        double slowdown = LAUNCH_SLOWDOWN * phaseTicks / Math.max(1, orders.launchTicks());
        setDeltaMovement(heading.scale(orders.speed() * (1 - slowdown)));
        if (phaseTicks >= orders.launchTicks()) enter(orders.guardTicks() > 0 ? Phase.GUARD : Phase.SEEK);
    }

    private void guard(ServerLevel server, ServerPlayer owner, SpiritOrders orders) {
        orbit(owner, orders);
        if (phaseTicks >= orders.guardTicks()) {
            enter(Phase.SEEK);
            return;
        }
        Projectile shot = SpiritTargets.incoming(owner, PageConfig.SPIRIT_GUARD_REACH.get());
        if (shot == null) return;
        server.sendParticles(ParticleTypes.POOF, shot.getX(), shot.getY(), shot.getZ(), 4, 0.1, 0.1, 0.1, 0.02);
        shot.discard();
        pop(server);
    }

    private void seek(ServerPlayer owner, SpiritOrders orders) {
        waited++;
        if (phaseTicks % SEARCH_EVERY_TICKS == 1) {
            LivingEntity found = SpiritTargets.find(this, owner, looked, orders.heals(), orders.search());
            if (found != null) {
                hunt(found, orders);
                return;
            }
        }
        orbit(owner, orders);
    }

    private void hunt(LivingEntity found, SpiritOrders orders) {
        target = found;
        waited = 0;
        boolean teases = !teased && orders.tauntTicks() > 0 && !orders.heals() && found instanceof Mob;
        enter(teases ? Phase.TEASE : Phase.CHASE);
    }

    private void chase(ServerLevel server, CompiledPage page, ServerPlayer owner, SpiritOrders orders) {
        if (!keepsTarget(owner, orders)) {
            loseTarget();
            return;
        }
        Vec3 center = center();
        AABB body = target.getBoundingBox().inflate(HIT_MARGIN);
        heading = SpiritMath.turn(heading, body.getCenter().subtract(center), orders.turn());
        Vec3 motion = heading.scale(orders.speed());
        if (body.contains(center) || body.clip(center, center.add(motion)).isPresent()) {
            strike(server, page, owner, orders, target);
            return;
        }
        setDeltaMovement(motion);
    }

    private void tease(ServerPlayer owner, SpiritOrders orders) {
        if (!keepsTarget(owner, orders) || !(target instanceof Mob mob)) {
            loseTarget();
            return;
        }
        if (mob.getTarget() != this) mob.setTarget(this);
        double angle = phaseTicks * ORBIT_SPEED * 2 + orbitOffset;
        Vec3 eye = mob.getEyePosition();
        glide(eye.add(Math.cos(angle) * TEASE_DISTANCE, 0.3, Math.sin(angle) * TEASE_DISTANCE), orders);
        heading = eye.subtract(center()).normalize();
        if (phaseTicks < orders.tauntTicks()) return;
        teased = true;
        enter(Phase.CHASE);
    }

    private void strike(ServerLevel server, CompiledPage page, ServerPlayer owner, SpiritOrders orders, LivingEntity victim) {
        CastContext ctx = new CastContext(server, owner, orders.spellId(), page.spell(), orders.power(), this);
        if (!page.hearts().isEmpty()) {
            PageStrike.hit(ctx, page, victim, page.param("spirit_strength", 0.7f));
        } else if (PageTargets.accepts(PageTarget.FOE, owner, victim)) {
            ctx.damage(victim, page.param("bare_damage", 2f));
        }
        pop(server);
    }

    private boolean keepsTarget(ServerPlayer owner, SpiritOrders orders) {
        return target != null && SpiritTargets.keeps(this, owner, target, orders.heals(), orders.search() * KEEP_REACH_SCALE);
    }

    private void loseTarget() {
        target = null;
        enter(Phase.SEEK);
    }

    private void enter(Phase next) {
        phase = next;
        phaseTicks = 0;
    }

    private void orbit(ServerPlayer owner, SpiritOrders orders) {
        double radius = PageConfig.SPIRIT_ORBIT_RADIUS.get();
        double angle = tickCount * ORBIT_SPEED + orbitOffset;
        double bob = Math.sin(tickCount * 0.1 + orbitOffset) * 0.2;
        glide(owner.position().add(Math.cos(angle) * radius, owner.getBbHeight() * 0.75 + bob, Math.sin(angle) * radius), orders);
    }

    private void glide(Vec3 spot, SpiritOrders orders) {
        Vec3 step = spot.subtract(center());
        double length = step.length();
        Vec3 motion = length > orders.speed() ? step.scale(orders.speed() / length) : step;
        if (motion.lengthSqr() > 1.0E-4) heading = motion.normalize();
        setDeltaMovement(motion);
    }

    private void keepOutOfWater(SpiritOrders orders) {
        Vec3 next = center().add(getDeltaMovement());
        if (level().getFluidState(BlockPos.containing(next)).is(FluidTags.WATER)) setDeltaMovement(0, orders.speed() * 0.5, 0);
    }

    private void faceHeading() {
        float yaw = (float) (Mth.atan2(heading.z, heading.x) * Mth.RAD_TO_DEG) - 90f;
        float pitch = (float) -(Mth.atan2(heading.y, heading.horizontalDistance()) * Mth.RAD_TO_DEG);
        setYRot(yaw);
        setYBodyRot(yaw);
        setYHeadRot(yaw);
        setXRot(pitch);
    }

    private byte currentMood(SpiritOrders orders) {
        return switch (phase) {
            case CHASE, TEASE -> HUNTING;
            case STRUCK -> STRUCK;
            default -> orders.note() == NoteType.MENTHOL && waited >= DOZE_AFTER_TICKS ? SLEEPING : WAITING;
        };
    }

    private Vec3 center() {
        return position().add(0, getBbHeight() / 2, 0);
    }

    private void pop(ServerLevel server) {
        enter(Phase.STRUCK);
        setDeltaMovement(Vec3.ZERO);
        entityData.set(MOOD, STRUCK);
        boolean silent = entityData.get(SILENT);
        Vec3 at = center();
        server.sendParticles(ModParticles.noteSmoke(color()), at.x, at.y, at.z, silent ? 3 : 14, 0.3, 0.3, 0.3, 0.02);
        server.sendParticles(ParticleTypes.POOF, at.x, at.y, at.z, silent ? 1 : 4, 0.15, 0.15, 0.15, 0.02);
        if (!silent) {
            float pitch = SpiritMath.squeakPitch(orders == null ? null : orders.note());
            server.playSound(null, at.x, at.y, at.z, SpiritRegistry.POP.get(), SoundSource.PLAYERS, 0.7f, pitch);
        }
    }

    void fade(ServerLevel server) {
        Vec3 at = center();
        server.sendParticles(ModParticles.noteSmoke(color()), at.x, at.y, at.z, 6, 0.2, 0.2, 0.2, 0.01);
        discard();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || phase == Phase.STRUCK || isInvulnerableTo(source)) return false;
        if (phase == Phase.TEASE && target != null && source.getEntity() == target) return false;
        if (level() instanceof ServerLevel server) pop(server);
        return true;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return super.isInvulnerableTo(source) || source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.FALL)
                || source.is(DamageTypes.CRAMMING) || source.is(DamageTypes.DROWN) || source.is(DamageTypes.FLY_INTO_WALL);
    }

    @Override
    public void travel(Vec3 travelVector) {
        if (isControlledByLocalInstance()) move(MoverType.SELF, getDeltaMovement());
    }

    @Override
    protected void pushEntities() {
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isAffectedByPotions() {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }
}
