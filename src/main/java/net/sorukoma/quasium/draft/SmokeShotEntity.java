package net.sorukoma.quasium.draft;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import net.sorukoma.quasium.Config;
import net.sorukoma.quasium.advancements.triggers.SmokecraftTrigger;
import net.sorukoma.quasium.magic.CastContext;
import net.sorukoma.quasium.magic.NoteType;
import net.sorukoma.quasium.particle.ModParticles;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class SmokeShotEntity extends Projectile {
    private static final int LIFETIME = 40;

    @Nullable
    private UUID gunner;

    public SmokeShotEntity(EntityType<? extends SmokeShotEntity> type, Level level) {
        super(type, level);
    }

    public SmokeShotEntity(Level level, Vec3 muzzle, @Nullable UUID gunner) {
        this(ForgeBlocks.SMOKE_SHOT.get(), level);
        this.gunner = gunner;
        setPos(muzzle);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hit.getType() != HitResult.Type.MISS && !EventHooks.onProjectileImpact(this, hit)) hitTargetOrDeflectSelf(hit);
        if (isRemoved()) return;
        setPos(position().add(getDeltaMovement()));
        if (level().isClientSide) {
            level().addParticle(ModParticles.noteSmoke(NoteType.ASH.color()), getX(), getY(), getZ(), 0, 0.01, 0);
        } else if (tickCount > LIFETIME) {
            discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!(level() instanceof ServerLevel level) || !(result.getEntity() instanceof LivingEntity target)) return;
        boolean wasAlive = target.isAlive();
        DamageSource source = new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(CastContext.SMOKE_MAGIC), this, null);
        if (!target.hurt(source, Config.CANNON_DAMAGE.getAsInt())) return;
        target.addEffect(new MobEffectInstance(MobEffects.POISON, Config.CANNON_POISON_SECONDS.getAsInt() * 20), this);
        ServerPlayer owner = gunner == null ? null : level.getServer().getPlayerList().getPlayer(gunner);
        if (owner != null && wasAlive && !target.isAlive() && target instanceof Mob) SmokecraftTrigger.fire(owner, "cannon_kill");
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!(level() instanceof ServerLevel level)) return;
        level.sendParticles(ModParticles.noteSmoke(NoteType.ASH.color()), getX(), getY(), getZ(), 8, 0.2, 0.2, 0.2, 0.02);
        discard();
    }
}
