package net.sorukoma.quasium.magic.page.form;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.sorukoma.quasium.magic.CastContext;
import net.sorukoma.quasium.magic.MagicMath;
import net.sorukoma.quasium.magic.page.PageTarget;
import net.sorukoma.quasium.magic.page.PageTargets;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;

public final class Lurk {
    private Lurk() {
    }

    public static int ticks(CastContext ctx) {
        return Math.round(ctx.param("lurk", 0f) * MagicMath.TICKS_PER_SECOND);
    }

    public static double reach() {
        return FormConfig.LURK_REACH.get();
    }

    @Nullable
    public static LivingEntity foe(CastContext ctx, AABB area, Vec3 from) {
        return ctx.level().getEntitiesOfClass(LivingEntity.class, area, target -> wakes(ctx, target)).stream()
                .min(Comparator.comparingDouble(target -> target.distanceToSqr(from)))
                .orElse(null);
    }

    private static boolean wakes(CastContext ctx, LivingEntity target) {
        return target.isAlive() && !target.isSpectator() && (target instanceof Enemy || target instanceof Player)
                && PageTargets.accepts(PageTarget.FOE, ctx.caster(), target);
    }
}
