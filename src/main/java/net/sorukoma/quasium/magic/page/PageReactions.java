package net.sorukoma.quasium.magic.page;

import net.minecraft.world.entity.LivingEntity;
import net.sorukoma.quasium.magic.CastContext;
import net.sorukoma.quasium.magic.page.effect.AccordStrike;

public final class PageReactions {
    private PageReactions() {
    }

    static void afterHit(CastContext ctx, CompiledPage page, LivingEntity target, float strength) {
        for (String id : page.accords()) AccordStrike.sound(ctx, page, id, target, strength);
    }
}
