package net.sorukoma.quasium.ecology;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.sorukoma.quasium.advancements.triggers.SmokecraftTrigger;
import net.sorukoma.quasium.item.EssenceBottleItem;
import net.sorukoma.quasium.magic.NoteType;

public class GarevshchikEntity extends Monster {
    private static final int EMBER_EVERY_TICKS = 4;

    public GarevshchikEntity(EntityType<? extends GarevshchikEntity> type, Level level) {
        super(type, level);
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(EcologyConfig.GAREVSHCHIK_HEALTH.getAsInt());
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(EcologyConfig.GAREVSHCHIK_DAMAGE.getAsInt());
        setHealth(getMaxHealth());
        xpReward = 8;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, EcologyConfig.GAREVSHCHIK_HEALTH.getDefault())
                .add(Attributes.ATTACK_DAMAGE, EcologyConfig.GAREVSHCHIK_DAMAGE.getDefault())
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        int seconds = EcologyConfig.GAREVSHCHIK_BLINDNESS_SECONDS.getAsInt();
        if (hit && seconds > 0 && target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, seconds * 20), this);
        }
        return hit;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide && tickCount % EMBER_EVERY_TICKS == 0) {
            level().addParticle(random.nextInt(3) == 0 ? ParticleTypes.SMALL_FLAME : ParticleTypes.SMOKE,
                    getRandomX(0.4), getRandomY(), getRandomZ(0.4), 0, 0.02, 0);
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (source.getEntity() instanceof ServerPlayer player) SmokecraftTrigger.fire(player, "garevshchik_kill");
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        int ml = EcologyConfig.GAREVSHCHIK_EMBER_ML.getAsInt();
        if (ml > 0) spawnAtLocation(EssenceBottleItem.of(NoteType.EMBER.id(), ml));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.CAMPFIRE_CRACKLE;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.BLAZE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.FIRE_EXTINGUISH;
    }
}
