package net.sorukoma.quasium.magic.page;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.sorukoma.quasium.magic.MagicMath;

public class ChokingEffect extends MobEffect {
    public ChokingEffect() {
        super(MobEffectCategory.HARMFUL, 0x4A4A4A);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % MagicMath.TICKS_PER_SECOND == 0;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (!CoreHearts.breathes(entity)) return false;
        entity.hurt(entity.damageSources().drown(), amplifier + 1);
        return true;
    }
}
