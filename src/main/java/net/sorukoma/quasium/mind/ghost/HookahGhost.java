package net.sorukoma.quasium.mind.ghost;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.sorukoma.quasium.mind.MindConfig;
import net.sorukoma.quasium.mind.MindPackets;
import net.sorukoma.quasium.mind.MindRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class HookahGhost extends PathfinderMob {
    @Nullable
    private UUID guest;

    public HookahGhost(EntityType<? extends HookahGhost> type, Level level) {
        super(type, level);
        setNoGravity(true);
        setInvulnerable(true);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.0);
    }

    public static HookahGhost visiting(ServerPlayer player) {
        HookahGhost ghost = new HookahGhost(MindRegistry.HOOKAH_GHOST.get(), player.level());
        ghost.guest = player.getUUID();
        return ghost;
    }

    public boolean visits(Player player) {
        return player.getUUID().equals(guest);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 16f, 1f));
    }

    @Override
    protected void customServerAiStep() {
        Player player = guest == null ? null : level().getPlayerByUUID(guest);
        if (player == null || level().isDay() || tickCount > MindConfig.GHOST_STAY_SECONDS.getAsInt() * 20) vanish();
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide && random.nextInt(4) == 0) {
            level().addParticle(ParticleTypes.SMOKE, getRandomX(0.5), getRandomY(), getRandomZ(0.5), 0, 0.02, 0);
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!visits(player)) return InteractionResult.PASS;
        if (player instanceof ServerPlayer serverPlayer) MindPackets.cue(serverPlayer, "ghost", 0);
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    public void vanish() {
        if (level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY(1), getZ(), 24, 0.3, 0.6, 0.3, 0.02);
        }
        discard();
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (guest != null) tag.putUUID("Guest", guest);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        guest = tag.hasUUID("Guest") ? tag.getUUID("Guest") : null;
    }
}
