package net.sorukoma.quasium.smokelands.sultan;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.sorukoma.quasium.particle.ModParticles;
import net.sorukoma.quasium.smokelands.SmokelandsConfig;
import net.sorukoma.quasium.smokelands.SmokelandsSounds;
import net.sorukoma.quasium.smokelands.storm.CinderStorm;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SultanOfSmoke extends Monster {
    public static final int INHALING = 1;
    public static final int BURNT_OUT = 2;
    public static final int STUNNED = 4;
    private static final EntityDataAccessor<Byte> FLAGS = SynchedEntityData.defineId(SultanOfSmoke.class, EntityDataSerializers.BYTE);
    private static final double HOVER = 2.5;
    private static final double ROAM = 10.0;
    private static final float CALM_HEAL_PER_SECOND = 2f;
    private static final int XP = 300;

    private final ServerBossEvent bossBar = new ServerBossEvent(getDisplayName(), BossEvent.BossBarColor.PURPLE,
            BossEvent.BossBarOverlay.NOTCHED_10);
    private final EnumSet<SultanVows.Vow> vows = EnumSet.noneOf(SultanVows.Vow.class);
    private SultanFight fight = new SultanFight(rules());
    @Nullable
    private BlockPos home;

    public SultanOfSmoke(EntityType<? extends SultanOfSmoke> type, Level level) {
        super(type, level);
        setNoGravity(true);
        xpReward = XP;
        bossBar.setPlayBossMusic(true);
        bossBar.setDarkenScreen(true);
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(SmokelandsConfig.SULTAN_HEALTH.get());
        setHealth(getMaxHealth());
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 300).add(Attributes.ARMOR, 6)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.FOLLOW_RANGE, 64).add(Attributes.MOVEMENT_SPEED, 0.2);
    }

    public static SultanFight.Rules rules() {
        return new SultanFight.Rules(SmokelandsConfig.PHASE_TWO_PCT.get(), SmokelandsConfig.PHASE_THREE_PCT.get(),
                SmokelandsConfig.RING_INTERVAL_SECONDS.get() * 20, SmokelandsConfig.INHALE_INTERVAL_SECONDS.get() * 20,
                SmokelandsConfig.INHALE_SECONDS.get() * 20, SmokelandsConfig.CLOUD_INTERVAL_SECONDS.get() * 20,
                SmokelandsConfig.SUMMON_INTERVAL_SECONDS.get() * 20, SmokelandsConfig.BURNOUT_INTERVAL_SECONDS.get() * 20,
                SmokelandsConfig.VULNERABLE_SECONDS.get() * 20, SmokelandsConfig.STUN_SECONDS.get() * 20);
    }

    public void settleAt(BlockPos throne) {
        home = throne.immutable();
        moveTo(throne.getX() + 0.5, throne.getY() + HOVER, throne.getZ() + 0.5, 0f, 0f);
    }

    public BlockPos home() {
        return home == null ? blockPosition() : home;
    }

    public SultanFight fight() {
        return fight;
    }

    public void vow(Set<SultanVows.Vow> taken) {
        vows.clear();
        vows.addAll(taken);
        fight = new SultanFight(SultanVows.rules(vows));
        bossBar.setName(vows.isEmpty() ? getDisplayName() : Component.translatable("quasium.sultan.vows." + vows.size(), getDisplayName()));
        bossBar.setColor(SultanVows.all(vows) ? BossEvent.BossBarColor.RED : BossEvent.BossBarColor.PURPLE);
    }

    public Set<SultanVows.Vow> vows() {
        return Collections.unmodifiableSet(vows);
    }

    public ServerBossEvent bossBar() {
        return bossBar;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FLAGS, (byte) 0);
    }

    public boolean flag(int flag) {
        return (entityData.get(FLAGS) & flag) != 0;
    }

    void syncFight() {
        int flags = (fight.inhaling() ? INHALING : 0) | (fight.vulnerable() ? BURNT_OUT : 0) | (fight.stunned() ? STUNNED : 0);
        entityData.set(FLAGS, (byte) flags);
    }

    public float healthPct() {
        return getHealth() * 100f / getMaxHealth();
    }

    public AABB arena() {
        double radius = SmokelandsConfig.ARENA_RADIUS.get();
        return new AABB(home()).inflate(radius, radius / 2, radius);
    }

    public List<ServerPlayer> arenaPlayers() {
        if (!(level() instanceof ServerLevel server)) return List.of();
        AABB arena = arena();
        return server.getPlayers(player -> player.isAlive() && !player.isSpectator() && !player.isCreative()
                && arena.contains(player.position()));
    }

    @Nullable
    private ServerPlayer nearest(List<ServerPlayer> foes) {
        return foes.stream().min(Comparator.comparingDouble(player -> player.distanceToSqr(this))).orElse(null);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) aura();
    }

    private void aura() {
        double top = getY() + getBbHeight() * 1.05;
        if (flag(BURNT_OUT)) {
            level().addParticle(ParticleTypes.LARGE_SMOKE, getRandomX(0.4), top, getRandomZ(0.4), 0, 0.06, 0);
        } else if (random.nextInt(3) == 0) {
            level().addParticle(ParticleTypes.SMALL_FLAME, getRandomX(0.3), top, getRandomZ(0.3), 0, 0.02, 0);
        }
        level().addParticle(ModParticles.noteSmoke(SultanRing.COLOR), getRandomX(0.9), getY() + random.nextDouble() * getBbHeight(),
                getRandomZ(0.9), 0, 0.01, 0);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        fightTick(arenaPlayers());
    }

    public List<SultanFight.Move> fightTick(List<ServerPlayer> foes) {
        if (home == null) home = blockPosition();
        updateBossBar(foes);
        ServerPlayer target = nearest(foes);
        setTarget(target);
        hover(target);
        if (target == null) {
            if (tickCount % 20 == 0 && getHealth() < getMaxHealth()) heal(CALM_HEAL_PER_SECOND);
            return List.of();
        }
        if (vows.contains(SultanVows.Vow.ASH) && level() instanceof ServerLevel server) CinderStorm.of(server).keepRaging(server);
        List<SultanFight.Move> moves = fight.tick(healthPct());
        for (SultanFight.Move move : moves) SultanAttacks.perform(this, move, target, foes);
        if (fight.inhaling()) SultanAttacks.pull(this, foes);
        syncFight();
        return moves;
    }

    private void hover(@Nullable Player target) {
        Vec3 centre = Vec3.atBottomCenterOf(home());
        Vec3 aim = centre;
        if (target != null && !fight.stunned()) {
            Vec3 toward = target.position().subtract(centre).multiply(1, 0, 1);
            aim = centre.add(toward.length() > ROAM ? toward.normalize().scale(ROAM) : toward.scale(0.6));
            lookAt(target, 30f, 30f);
            yBodyRot = getYRot();
        }
        double bob = Math.sin(tickCount * 0.05) * 0.4;
        Vec3 wanted = new Vec3(aim.x, centre.y + HOVER + bob, aim.z);
        Vec3 step = wanted.subtract(position()).scale(0.04);
        double cap = fight.stunned() ? 0.02 : 0.25;
        setDeltaMovement(step.length() > cap ? step.normalize().scale(cap) : step);
    }

    private void updateBossBar(List<ServerPlayer> foes) {
        bossBar.setProgress(getHealth() / getMaxHealth());
        Set<ServerPlayer> wanted = new HashSet<>(foes);
        for (ServerPlayer seen : List.copyOf(bossBar.getPlayers())) {
            if (!wanted.remove(seen)) bossBar.removePlayer(seen);
        }
        wanted.forEach(bossBar::addPlayer);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        Entity attacker = source.getEntity();
        if (attacker == this || source.getDirectEntity() instanceof SultanRing) return false;
        if (attacker != null && !arena().contains(attacker.position())) {
            if (attacker instanceof ServerPlayer player) player.displayClientMessage(Component.translatable("quasium.sultan.out_of_reach"), true);
            return false;
        }
        if (SultanEvents.dewSplasher(source) instanceof ServerPlayer player && douse(player)) {
            amount *= SmokelandsConfig.STUN_DAMAGE_MULTIPLIER.get();
        } else if (fight.stunned()) {
            amount *= SmokelandsConfig.STUN_DAMAGE_MULTIPLIER.get();
        }
        return super.hurt(source, amount);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!SultanEvents.isDew(held)) return super.mobInteract(player, hand);
        if (!fight.vulnerable()) return InteractionResult.PASS;
        if (player instanceof ServerPlayer serverPlayer && douse(serverPlayer)) SultanEvents.pour(serverPlayer, held);
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    public boolean douse(ServerPlayer player) {
        if (!fight.douse()) return false;
        SultanAttacks.stunned(this, player);
        syncFight();
        return true;
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        SultanRewards.drop(this, level);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel server) SultanRewards.defeated(this, server);
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        if (reason == RemovalReason.DISCARDED && level() instanceof ServerLevel server) SultanArena.of(server).forget(home());
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossBar.removePlayer(player);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.put("Home", NbtUtils.writeBlockPos(home()));
        tag.putInt("Vows", SultanVows.mask(vows));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        NbtUtils.readBlockPos(tag, "Home").ifPresent(pos -> home = pos);
        vow(SultanVows.of(tag.getInt("Vows")));
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return !effect.is(MobEffects.POISON) && super.canBeAffected(effect);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    @Override
    public boolean canUsePortal(boolean allowPassengers) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SmokelandsSounds.SULTAN_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SmokelandsSounds.SULTAN_STUN.get();
    }

    @Override
    protected float getSoundVolume() {
        return 2f;
    }

    @Override
    public boolean isPersistenceRequired() {
        return true;
    }
}
