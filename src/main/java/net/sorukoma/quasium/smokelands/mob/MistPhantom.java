package net.sorukoma.quasium.smokelands.mob;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.sorukoma.quasium.smokelands.SmokelandsConfig;
import org.jetbrains.annotations.Nullable;

public class MistPhantom extends Phantom {
    public MistPhantom(EntityType<? extends Phantom> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 30).add(Attributes.ATTACK_DAMAGE, 4);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType,
                                        @Nullable SpawnGroupData groupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, groupData);
        SmokelandsEntities.setHealth(this, SmokelandsConfig.MIST_PHANTOM_HEALTH.get());
        return data;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide && random.nextInt(3) == 0) {
            level().addParticle(ParticleTypes.WHITE_SMOKE, getRandomX(0.8), getRandomY(), getRandomZ(0.8), 0, 0.01, 0);
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = target.hurt(damageSources().mobAttack(this), SmokelandsConfig.MIST_PHANTOM_DAMAGE.get());
        if (hit && target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.CONFUSION, SmokelandsConfig.MIST_PHANTOM_NAUSEA_SECONDS.get() * 20), this);
        }
        return hit;
    }
}
