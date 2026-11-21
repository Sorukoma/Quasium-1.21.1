package net.sorukoma.quasium.ecology;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.sorukoma.quasium.advancements.triggers.SmokecraftTrigger;
import net.sorukoma.quasium.data.PlayerKaifData;
import net.sorukoma.quasium.item.EssenceBottleItem;
import net.sorukoma.quasium.magic.NoteType;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;

public class DymnikEntity extends PathfinderMob {
    private static final int SECOND = 20;
    private static final double SEEK_RANGE = 16.0;
    private static final double HOVER_DISTANCE = 1.8;
    private static final float WHISPER_CHANCE = 0.25f;

    public DymnikEntity(EntityType<? extends DymnikEntity> type, Level level) {
        super(type, level);
        moveControl = new FlyingMoveControl(this, 20, true);
        setNoGravity(true);
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(EcologyConfig.DYMNIK_HEALTH.getAsInt());
        setHealth(getMaxHealth());
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, EcologyConfig.DYMNIK_HEALTH.getDefault())
                .add(Attributes.FLYING_SPEED, 0.25)
                .add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.FOLLOW_RANGE, SEEK_RANGE);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        navigation.setCanPassDoors(true);
        return navigation;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new HauntGoal(this));
        goalSelector.addGoal(2, new WaterAvoidingRandomFlyingGoal(this, 1.0));
        goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8f));
    }

    @Override
    public void travel(Vec3 travelVector) {
        if (!isControlledByLocalInstance()) return;
        moveRelative(isInWater() ? 0.02f : getSpeed(), travelVector);
        move(MoverType.SELF, getDeltaMovement());
        setDeltaMovement(getDeltaMovement().scale(isInWater() ? 0.8 : 0.91));
        calculateEntityAnimation(false);
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel server && tickCount % SECOND == 0) {
            if (isNight()) {
                drain(server.getEntitiesOfClass(Player.class, getBoundingBox().inflate(EcologyConfig.DYMNIK_STEAL_RADIUS.getAsInt())));
            } else if (inSunlight()) {
                burn(server);
            }
        }
    }

    public boolean isNight() {
        return !level().isDay();
    }

    public boolean inSunlight() {
        BlockPos eyes = BlockPos.containing(getX(), getEyeY(), getZ());
        return level().isDay() && level().canSeeSky(eyes) && !isInWaterRainOrBubble();
    }

    public int drain(List<? extends Player> nearby) {
        if (!isNight()) return 0;
        double reach = EcologyConfig.DYMNIK_STEAL_RADIUS.getAsInt();
        int stolen = 0;
        for (Player victim : nearby) {
            int kaif = PlayerKaifData.getKaifValue(victim);
            if (!victim.isAlive() || victim.isCreative() || victim.isSpectator() || kaif <= 0 || distanceToSqr(victim) > reach * reach) continue;
            PlayerKaifData.subtractKaif(victim, Math.min(kaif, EcologyConfig.DYMNIK_STEAL_KAIF_PER_SECOND.getAsInt()));
            int taken = kaif - PlayerKaifData.getKaifValue(victim);
            if (taken <= 0) continue;
            stolen += taken;
            if (level() instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.SMOKE, victim.getX(), victim.getEyeY() - 0.2, victim.getZ(), 4, 0.15, 0.1, 0.15, 0.01);
            }
        }
        if (stolen > 0 && random.nextFloat() < WHISPER_CHANCE) playSound(EcologyRegistry.DYMNIK_WHISPER.get(), 0.8f, getVoicePitch());
        return stolen;
    }

    private void burn(ServerLevel level) {
        level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY(0.5), getZ(), 6, 0.2, 0.25, 0.2, 0.02);
        hurt(damageSources().onFire(), getMaxHealth() / EcologyConfig.DYMNIK_SUNBURN_SECONDS.getAsInt());
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (source.getEntity() instanceof ServerPlayer player) SmokecraftTrigger.fire(player, "dymnik_kill");
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        int ml = EcologyConfig.DYMNIK_ASH_ML.getAsInt();
        if (ml > 0) spawnAtLocation(EssenceBottleItem.of(NoteType.ASH.id(), ml));
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return isNight() ? EcologyRegistry.DYMNIK_WHISPER.get() : null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.BREEZE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.GENERIC_EXTINGUISH_FIRE;
    }

    private static final class HauntGoal extends Goal {
        private static final int REPATH_TICKS = 10;

        private final DymnikEntity dymnik;
        @Nullable
        private Player victim;
        private int repath;

        private HauntGoal(DymnikEntity dymnik) {
            this.dymnik = dymnik;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!dymnik.isNight()) return false;
            victim = dymnik.level().getNearestPlayer(dymnik.getX(), dymnik.getY(), dymnik.getZ(), SEEK_RANGE,
                    player -> player instanceof Player target && !target.isCreative() && !target.isSpectator()
                            && PlayerKaifData.getKaifValue(target) > 0);
            return victim != null;
        }

        @Override
        public boolean canContinueToUse() {
            return victim != null && victim.isAlive() && dymnik.isNight() && PlayerKaifData.getKaifValue(victim) > 0
                    && dymnik.distanceToSqr(victim) < SEEK_RANGE * SEEK_RANGE;
        }

        @Override
        public void start() {
            repath = 0;
        }

        @Override
        public void stop() {
            victim = null;
            dymnik.getNavigation().stop();
        }

        @Override
        public void tick() {
            if (victim == null) return;
            dymnik.getLookControl().setLookAt(victim);
            if (--repath > 0) return;
            repath = REPATH_TICKS;
            if (dymnik.distanceToSqr(victim) > HOVER_DISTANCE * HOVER_DISTANCE) {
                dymnik.getNavigation().moveTo(victim.getX(), victim.getEyeY(), victim.getZ(), 1.0);
            } else {
                dymnik.getNavigation().stop();
            }
        }
    }
}
