package net.sorukoma.quasium.magic.page;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.sorukoma.quasium.magic.CastContext;
import net.sorukoma.quasium.magic.NoteType;
import net.sorukoma.quasium.magic.SpellForm;
import net.sorukoma.quasium.magic.page.form.Quiet;
import net.sorukoma.quasium.magic.page.effect.NoteMarks;
import net.sorukoma.quasium.magic.page.effect.Tolerance;
import net.sorukoma.quasium.particle.ModParticles;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class PageStrike {
    private PageStrike() {
    }

    public static void hit(CastContext ctx, CompiledPage page, LivingEntity target, float strength) {
        Set<UUID> struck = new HashSet<>();
        struck.add(target.getUUID());
        strike(ctx, page, target, strength);
        chain(ctx, page, target, strength, Math.round(page.param("chain", 0f)), struck);
    }

    private static void strike(CastContext ctx, CompiledPage page, LivingEntity target, float strength) {
        boolean self = page.spell().form() == SpellForm.SELF;
        PageCatalog catalog = PageCatalog.get();
        List<Landing> landings = new ArrayList<>();
        for (String id : page.hearts()) {
            PageEffect effect = catalog.effect(id);
            HeartBehavior behavior = HeartEffects.get(id);
            if (effect == null || behavior == null) continue;
            if (!self && !PageTargets.accepts(effect.target(), ctx.caster(), target)) continue;
            landings.add(new Landing(effect, behavior));
        }
        List<NoteType> notes = landings.stream().map(Landing::effect).filter(effect -> effect.target() != PageTarget.WORLD)
                .map(PageEffect::note).distinct().toList();
        Map<NoteType, Float> felt = Tolerance.strike(target, notes);
        for (Landing landing : landings) {
            float tolerated = strength * felt.getOrDefault(landing.effect().note(), 1f);
            landing.behavior().hitEntity(new PageHit(ctx, page, landing.effect(), tolerated), target);
        }
        PageReactions.afterHit(ctx, page, target, strength);
        NoteMarks.afterHit(ctx, page, target, notes, strength);
    }

    private record Landing(PageEffect effect, HeartBehavior behavior) {
    }

    private static void chain(CastContext ctx, CompiledPage page, LivingEntity from, float strength, int jumps, Set<UUID> struck) {
        if (jumps <= 0) return;
        double reach = PageConfig.CHAIN_REACH.get();
        LivingEntity next = ctx.level().getEntitiesOfClass(LivingEntity.class, from.getBoundingBox().inflate(reach),
                        other -> other.isAlive() && !other.isSpectator() && !struck.contains(other.getUUID())
                                && PageTargets.accepts(PageTarget.FOE, ctx.caster(), other))
                .stream()
                .min(Comparator.comparingDouble(other -> other.distanceToSqr(from)))
                .orElse(null);
        if (next == null) return;
        struck.add(next.getUUID());
        float chained = strength * PageConfig.CHAIN_POWER_PCT.getAsInt() / 100f;
        if (!Quiet.of(ctx)) trail(ctx, page, from.getEyePosition(), next.getEyePosition());
        strike(ctx, page, next, chained);
        chain(ctx, page, next, chained, jumps - 1, struck);
    }

    private static void trail(CastContext ctx, CompiledPage page, Vec3 from, Vec3 to) {
        Vec3 step = to.subtract(from);
        int points = Math.max(1, (int) (step.length() * 2));
        for (int i = 1; i <= points; i++) {
            Vec3 point = from.add(step.scale(i / (double) points));
            ctx.level().sendParticles(ModParticles.noteSmoke(page.spell().color()), point.x, point.y, point.z, 1, 0.02, 0.02, 0.02, 0.0);
        }
    }
}
