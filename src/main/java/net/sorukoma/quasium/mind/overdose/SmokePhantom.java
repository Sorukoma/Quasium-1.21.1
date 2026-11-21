package net.sorukoma.quasium.mind.overdose;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.sorukoma.quasium.advancements.triggers.SmokecraftTrigger;
import net.sorukoma.quasium.data.PlayerKaifData;
import net.sorukoma.quasium.data.PlayerOverdoseData;
import net.sorukoma.quasium.item.EssenceBottleItem;
import net.sorukoma.quasium.mind.MindConfig;
import net.sorukoma.quasium.mind.MindRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class SmokePhantom extends FlyingMob {
    public static final String DELIRIUM = "delirium";
    private static final double ORBIT = 2.4;
    private static final double SPEED = 0.35;

    @Nullable
    private UUID haunted;
    private float phase;

    public SmokePhantom(EntityType<? extends SmokePhantom> type, Level level) {
        super(type, level);
        setNoGravity(true);
        phase = random.nextFloat() * 6.283f;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return FlyingMob.createMobAttributes().add(Attributes.MAX_HEALTH, 6.0).add(Attributes.FLYING_SPEED, 0.6);
    }

    public static SmokePhantom near(ServerPlayer player) {
        SmokePhantom phantom = new SmokePhantom(MindRegistry.SMOKE_PHANTOM.get(), player.level());
        phantom.haunted = player.getUUID();
        double angle = player.getRandom().nextDouble() * Math.PI * 2;
        double distance = 4 + player.getRandom().nextDouble() * 4;
        phantom.moveTo(player.getX() + Math.cos(angle) * distance, player.getEyeY() + 1, player.getZ() + Math.sin(angle) * distance,
                player.getRandom().nextFloat() * 360f, 0f);
        float health = MindConfig.PHANTOM_HEALTH.getAsInt();
        phantom.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
        phantom.setHealth(health);
        return phantom;
    }

    public boolean haunts(Player player) {
        return player.getUUID().equals(haunted);
    }

    @Override
    protected void customServerAiStep() {
        Player target = haunted == null ? null : level().getPlayerByUUID(haunted);
        if (target == null || !target.isAlive() || PlayerOverdoseData.stage(target) < 2) {
            dissolve();
            return;
        }
        float angle = phase + tickCount * 0.06f;
        Vec3 goal = target.getEyePosition().add(Math.cos(angle) * ORBIT, 0.4 + Math.sin(tickCount * 0.05f) * 0.4, Math.sin(angle) * ORBIT);
        Vec3 pull = goal.subtract(position()).scale(0.08);
        Vec3 velocity = getDeltaMovement().add(pull);
        if (velocity.length() > SPEED) velocity = velocity.normalize().scale(SPEED);
        setDeltaMovement(velocity);
        getLookControl().setLookAt(target);
        setYRot((float) (Math.atan2(velocity.z, velocity.x) * 180 / Math.PI) - 90f);
        yBodyRot = getYRot();
        int radius = MindConfig.PHANTOM_STEAL_RADIUS.getAsInt();
        if (tickCount % 20 == 0 && distanceToSqr(target) <= radius * radius) {
            PlayerKaifData.subtractKaif(target, MindConfig.PHANTOM_STEAL_PER_SECOND.getAsInt());
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof Player && amount > 0) amount = Math.max(amount, getHealth());
        return super.hurt(source, amount);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        if (source.getEntity() instanceof ServerPlayer killer) SmokecraftTrigger.fire(killer, "smoke_phantom");
        if (random.nextInt(100) < MindConfig.PHANTOM_DROP_CHANCE_PCT.getAsInt()) {
            spawnAtLocation(EssenceBottleItem.of(DELIRIUM, MindConfig.PHANTOM_DROP_ML.getAsInt()));
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        puff();
    }

    private void dissolve() {
        puff();
        discard();
    }

    private void puff() {
        if (level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY(0.5), getZ(), 14, 0.3, 0.2, 0.3, 0.02);
        }
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PHANTOM_AMBIENT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PHANTOM_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.55f;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (haunted != null) tag.putUUID("Haunted", haunted);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        haunted = tag.hasUUID("Haunted") ? tag.getUUID("Haunted") : null;
    }
}
