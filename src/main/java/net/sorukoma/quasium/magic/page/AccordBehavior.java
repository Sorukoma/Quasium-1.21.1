package net.sorukoma.quasium.magic.page;

import net.minecraft.world.entity.LivingEntity;
import net.sorukoma.quasium.magic.CastContext;

public interface AccordBehavior {
    void sound(CastContext ctx, CompiledPage page, Accord accord, LivingEntity target, float strength);
}
