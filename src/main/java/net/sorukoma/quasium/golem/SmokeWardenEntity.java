package net.sorukoma.quasium.golem;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidType;
import net.sorukoma.quasium.Config;
import net.sorukoma.quasium.advancements.triggers.SmokecraftTrigger;
import net.sorukoma.quasium.magic.CastContext;
import net.sorukoma.quasium.particle.ModParticles;
import net.sorukoma.quasium.sound.ModSoundEvents;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.UUID;

public class SmokeWardenEntity extends AbstractGolem {
    static final int SMOKE_COLOR = 0x8FA488;
    private static final int AIM_TICKS = 10;
    private static final int SAD_PUFF_CHANCE = 120;
    private static final float SHOT_SPEED = 1.4f;
    private static final double MUZZLE_REACH = 0.5;

    private final WardenFriends friends = new WardenFriends();
    private int coal;
    private int charcoal;
    private int shotsLeft;
    private int cooldown;

    public SmokeWardenEntity(EntityType<? extends SmokeWardenEntity> type, Level level) {
        super(type, level);
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(Config.WARDEN_MAX_HEALTH.getAsInt());
        setHealth(getMaxHealth());
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1)
                .add(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE, 1);
    }

    public void claim(@Nullable Player player) {
        if (player == null) return;
        friends.claim(player);
        float facing = player.getYRot() + 180f;
        setYRot(facing);
        yBodyRot = facing;
        yHeadRot = facing;
    }

    public int fuel() {
        return coal + charcoal;
    }

    public boolean isFriend(UUID id) {
        return friends.isFriend(id);
    }

    void standUpFor(Player victim, Player attacker) {
        if (friends.isFriend(victim.getUUID())) friends.holdGrudge(attacker.getUUID());
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new RandomLookAroundGoal(this));
    }

    @Override
    protected BodyRotationControl createBodyControl() {
        return new BodyRotationControl(this) {
            @Override
            public void clientTick() {
            }
        };
    }

    @Override
    protected void customServerAiStep() {
        if (cooldown > 0) cooldown--;
        if (fuel() == 0 && shotsLeft == 0) {
            if (getTarget() != null) setTarget(null);
            if (random.nextInt(SAD_PUFF_CHANCE) == 0) puff(ParticleTypes.SMOKE, position().add(0, getBbHeight(), 0), 3);
            return;
        }
        LivingEntity target = getTarget();
        if (tickCount % AIM_TICKS == 0 || target != null && !inSight(target)) {
            LivingEntity choice = chooseTarget();
            if (choice != target) setTarget(choice);
            target = getTarget();
        }
        if (target == null) return;
        getLookControl().setLookAt(target, 30f, 30f);
        if (cooldown == 0) shoot(target);
    }

    private void shoot(LivingEntity target) {
        if (shotsLeft == 0) {
            if (charcoal > 0) charcoal--;
            else coal--;
            shotsLeft = Config.WARDEN_SHOTS_PER_COAL.getAsInt();
        }
        shotsLeft--;
        cooldown = Config.WARDEN_FIRE_TICKS.getAsInt();
        Vec3 eye = getEyePosition();
        Vec3 lead = target.getDeltaMovement().multiply(1, 0, 1).scale(eye.distanceTo(target.position()) / SHOT_SPEED);
        Vec3 aim = target.getEyePosition().add(lead).subtract(eye).normalize();
        Vec3 muzzle = eye.add(aim.scale(MUZZLE_REACH));
        WardenShotEntity shot = new WardenShotEntity(this, muzzle);
        shot.shoot(aim.x, aim.y, aim.z, SHOT_SPEED, 0f);
        level().addFreshEntity(shot);
        level().playSound(null, muzzle.x, muzzle.y, muzzle.z, ModSoundEvents.WARDEN_SHOT.get(), SoundSource.NEUTRAL,
                1f, 0.9f + random.nextFloat() * 0.2f);
        puff(ModParticles.noteSmoke(SMOKE_COLOR), muzzle, 4);
    }

    void strike(WardenShotEntity shot, LivingEntity target) {
        boolean wasAlive = target.isAlive();
        if (!target.hurt(damageSources().source(CastContext.SMOKE_MAGIC, shot, this), Config.WARDEN_DAMAGE.getAsInt())) return;
        afflict(target, MobEffects.POISON, Config.WARDEN_POISON_SECONDS.getAsInt(), 0);
        afflict(target, MobEffects.MOVEMENT_SLOWDOWN, Config.WARDEN_SLOW_SECONDS.getAsInt(), 1);
        UUID owner = friends.owner();
        if (wasAlive && !target.isAlive() && target instanceof Mob && owner != null
                && level().getServer().getPlayerList().getPlayer(owner) instanceof ServerPlayer player) {
            SmokecraftTrigger.fire(player, "warden_kill");
        }
    }

    private void afflict(LivingEntity target, Holder<MobEffect> effect, int seconds, int amplifier) {
        if (seconds > 0) target.addEffect(new MobEffectInstance(effect, seconds * 20, amplifier), this);
    }

    boolean isEnemy(Entity entity) {
        if (!(entity instanceof LivingEntity target) || !target.canBeSeenAsEnemy()) return false;
        if (target instanceof Player) return friends.bearsGrudge(target.getUUID());
        if (target instanceof NeutralMob neutral && !neutral.isAngry()) return false;
        return target instanceof Enemy;
    }

    private boolean inSight(LivingEntity target) {
        double range = Config.WARDEN_FIRE_RANGE.getAsInt();
        return isEnemy(target) && distanceToSqr(target) <= range * range && hasLineOfSight(target);
    }

    @Nullable
    private LivingEntity chooseTarget() {
        return level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(Config.WARDEN_FIRE_RANGE.getAsInt()), this::inSight)
                .stream()
                .min(Comparator.comparing((LivingEntity target) -> !(target instanceof Creeper)).thenComparingDouble(this::distanceToSqr))
                .orElse(null);
    }

    private void puff(ParticleOptions particle, Vec3 at, int count) {
        if (level() instanceof ServerLevel server) server.sendParticles(particle, at.x, at.y, at.z, count, 0.08, 0.08, 0.08, 0.01);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(Items.COAL) || stack.is(Items.CHARCOAL)) {
            if (!level().isClientSide) feed(player, stack);
        } else if (stack.is(Items.CLAY_BALL) && getHealth() < getMaxHealth()) {
            if (!level().isClientSide) repair(player, stack);
        } else if (stack.is(Items.PAPER) && stack.has(DataComponents.CUSTOM_NAME)) {
            if (!level().isClientSide) {
                player.sendSystemMessage(friends.edit(player, stack.getHoverName().getString().trim(), player.isSecondaryUseActive()));
            }
        } else if (hand == InteractionHand.MAIN_HAND && stack.isEmpty()) {
            if (!level().isClientSide) useBareHand(player);
        } else {
            return InteractionResult.PASS;
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    private void feed(Player player, ItemStack stack) {
        int capacity = Config.WARDEN_INVENTORY.getAsInt();
        if (fuel() < capacity) {
            if (stack.is(Items.CHARCOAL)) charcoal++;
            else coal++;
            stack.consume(1, player);
        }
        player.displayClientMessage(Component.translatable("quasium.warden.fuel", fuel(), capacity), true);
    }

    private void repair(Player player, ItemStack clay) {
        heal(Config.WARDEN_REPAIR_HEALTH.getAsInt());
        clay.consume(1, player);
        playSound(SoundEvents.DECORATED_POT_INSERT, 1f, 0.9f + random.nextFloat() * 0.2f);
        player.displayClientMessage(Component.translatable("quasium.warden.repaired", Mth.ceil(getHealth()), Mth.ceil(getMaxHealth())), true);
    }

    private void useBareHand(Player player) {
        if (friends.owner() == null) {
            claim(player);
            player.sendSystemMessage(Component.translatable("quasium.warden.claimed"));
        } else if (player.isSecondaryUseActive() && friends.isOwner(player)) {
            pickUp(player);
        } else {
            player.sendSystemMessage(Component.translatable("quasium.warden.status", friends.ownerName(), fuel(), Config.WARDEN_INVENTORY.getAsInt(),
                    Mth.ceil(getHealth()), Mth.ceil(getMaxHealth())));
            player.sendSystemMessage(friends.describeFor(player));
        }
    }

    private void pickUp(Player player) {
        CompoundTag data = new CompoundTag();
        data.putString("id", EntityType.getKey(getType()).toString());
        saveLoadout(data);
        data.putFloat("Health", getHealth());
        ItemStack item = new ItemStack(WardenGolems.SMOKE_WARDEN_ITEM.get());
        item.set(DataComponents.ENTITY_DATA, CustomData.of(data));
        if (hasCustomName()) item.set(DataComponents.CUSTOM_NAME, getCustomName());
        player.setItemInHand(InteractionHand.MAIN_HAND, item);
        playSound(SoundEvents.ITEM_PICKUP, 0.6f, 1f);
        discard();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && source.getEntity() instanceof Player player) friends.holdGrudge(player.getUUID());
        return hurt;
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        spawnAtLocation(WardenGolems.WARDEN_BLANK.get());
        spawnAtLocation(new ItemStack(Items.COAL, coal));
        spawnAtLocation(new ItemStack(Items.CHARCOAL, charcoal));
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.DECORATED_POT_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.DECORATED_POT_SHATTER;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isPushedByFluid(FluidType type) {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        saveLoadout(tag);
    }

    private void saveLoadout(CompoundTag tag) {
        friends.save(tag);
        tag.putInt("Coal", coal);
        tag.putInt("Charcoal", charcoal);
        tag.putInt("ShotsLeft", shotsLeft);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(Config.WARDEN_MAX_HEALTH.getAsInt());
        friends.load(tag, level().getServer());
        coal = tag.getInt("Coal");
        charcoal = tag.getInt("Charcoal");
        shotsLeft = tag.getInt("ShotsLeft");
    }
}
