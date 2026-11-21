package net.sorukoma.quasium.smokelands.mob;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.sorukoma.quasium.smokelands.SmokelandsConfig;
import org.jetbrains.annotations.Nullable;

public class TarSlime extends Slime {
    private static final int SIZE = 2;

    public TarSlime(EntityType<? extends Slime> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 18);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType,
                                        @Nullable SpawnGroupData groupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, groupData);
        setSize(SIZE, true);
        SmokelandsEntities.setHealth(this, SmokelandsConfig.TAR_SLIME_HEALTH.get());
        return data;
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(SmokelandsConfig.TAR_SLIME_HEALTH.get());
    }

    @Override
    protected void dealDamage(LivingEntity target) {
        float before = target.getHealth();
        super.dealDamage(target);
        if (target.getHealth() >= before) return;
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SmokelandsConfig.TAR_SLIME_SLOWNESS_SECONDS.get() * 20,
                SmokelandsConfig.TAR_SLIME_SLOWNESS_LEVEL.get() - 1), this);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (isDeadOrDying()) setSize(1, false);
        super.remove(reason);
    }

    @Override
    protected ParticleOptions getParticleType() {
        return ParticleTypes.SQUID_INK;
    }
}
