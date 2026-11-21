package net.sorukoma.quasium.smokelands.mob;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.sorukoma.quasium.advancements.triggers.SmokecraftTrigger;
import net.sorukoma.quasium.smokelands.SmokelandsConfig;
import net.sorukoma.quasium.smokelands.SmokelandsSounds;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class DraftWisp extends PathfinderMob implements FlyingAnimal {
    public static final int GLOW = 0x9FD8FF;
    private static final String OWNER = "Owner";
    @Nullable
    private UUID owner;
    @Nullable
    private BlockPos light;

    public DraftWisp(EntityType<? extends DraftWisp> type, Level level) {
        super(type, level);
        moveControl = new FlyingMoveControl(this, 20, true);
        setNoGravity(true);
        settleSpeed();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 4).add(Attributes.FLYING_SPEED);
    }

    private void settleSpeed() {
        double speed = SmokelandsConfig.WISP_FLYING_SPEED.get();
        getAttribute(Attributes.FLYING_SPEED).setBaseValue(speed);
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(speed);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType,
                                        @Nullable SpawnGroupData groupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, groupData);
        SmokelandsEntities.setHealth(this, SmokelandsConfig.WISP_HEALTH.get());
        return data;
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
        goalSelector.addGoal(1, new WispFollowGoal(this));
        goalSelector.addGoal(2, new WaterAvoidingRandomFlyingGoal(this, 1.0));
        goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 6f));
        goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }

    @Override
    public void travel(Vec3 travelVector) {
        if (!isControlledByLocalInstance()) return;
        moveRelative(isInWater() || isInLava() ? 0.02f : getSpeed(), travelVector);
        move(MoverType.SELF, getDeltaMovement());
        setDeltaMovement(getDeltaMovement().scale(isInWater() || isInLava() ? 0.6 : 0.91));
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    @Override
    public boolean isFlying() {
        return !onGround();
    }

    public boolean isTamed() {
        return owner != null;
    }

    public boolean serves(Player player) {
        return player.getUUID().equals(owner);
    }

    @Nullable
    public Player owner() {
        return owner == null ? null : level().getPlayerByUUID(owner);
    }

    public void serve(Player player) {
        owner = player.getUUID();
        setPersistenceRequired();
    }

    @Nullable
    BlockPos light() {
        return light;
    }

    void light(BlockPos at) {
        light = at;
    }

    @Override
    public void remove(RemovalReason reason) {
        if (reason.shouldDestroy() && level() instanceof ServerLevel server) WispService.darken(server, light);
        super.remove(reason);
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return isTamed() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || super.isInvulnerableTo(source);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return !isTamed();
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (isTamed() || !stack.is(Items.HONEY_BOTTLE)) return super.mobInteract(player, hand);
        if (player instanceof ServerPlayer serverPlayer) tameBy(serverPlayer, stack);
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    public void tameBy(ServerPlayer player, ItemStack honey) {
        honey.consume(1, player);
        give(player, new ItemStack(Items.GLASS_BOTTLE));
        give(player, new ItemStack(SmokelandsEntities.WISP_FLUTE.get()));
        level().playSound(null, blockPosition(), SmokelandsSounds.WISP_TAME.get(), SoundSource.NEUTRAL, 1f, 1f);
        player.serverLevel().sendParticles(ParticleTypes.HEART, getX(), getY() + 0.3, getZ(), 6, 0.3, 0.3, 0.3, 0.02);
        SmokecraftTrigger.fire(player, "wisp_tamed");
        discard();
    }

    private static void give(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) player.drop(stack, false);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level() instanceof ServerLevel server && isTamed() && tickCount % 10 == 0) WispService.serve(server, this);
        if (level().isClientSide && random.nextInt(3) == 0) {
            level().addParticle(ParticleTypes.END_ROD, getRandomX(0.4), getRandomY(), getRandomZ(0.4), 0, 0.01, 0);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (owner != null) tag.putUUID(OWNER, owner);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        owner = tag.hasUUID(OWNER) ? tag.getUUID(OWNER) : null;
        settleSpeed();
    }
}
