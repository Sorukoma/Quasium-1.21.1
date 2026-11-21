package net.sorukoma.quasium.magic.page;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.sorukoma.quasium.magic.CastContext;
import net.sorukoma.quasium.magic.SpellAction;

import java.util.List;

public final class PageAction implements SpellAction {
    public static final String ID = "page";
    public static final PageAction INSTANCE = new PageAction();
    private static final int MAX_BLOCK_RADIUS = 8;

    private PageAction() {
    }

    @Override
    public void hitEntity(CastContext ctx, LivingEntity target) {
        CompiledPage page = PageSpells.get(ctx.spellId());
        if (page != null) PageStrike.hit(ctx, page, target, 1f);
    }

    @Override
    public boolean usesBlocks(CastContext ctx) {
        CompiledPage page = PageSpells.get(ctx.spellId());
        if (page == null) return false;
        for (String id : page.hearts()) {
            HeartBehavior behavior = HeartEffects.get(id);
            if (behavior != null && behavior.usesBlocks()) return true;
        }
        return false;
    }

    @Override
    public void hitBlock(CastContext ctx, BlockPos pos) {
        CompiledPage page = PageSpells.get(ctx.spellId());
        if (page == null) return;
        forEachHeart(ctx, page, 1f, (hit, behavior) -> {
            if (behavior.usesBlocks()) behavior.hitBlock(hit, pos);
        });
    }

    @Override
    public void impactBlock(CastContext ctx, BlockPos pos) {
        CompiledPage page = PageSpells.get(ctx.spellId());
        if (page == null) return;
        forEachHeart(ctx, page, 1f, (hit, behavior) -> behavior.hitBlock(hit, pos));
    }

    @Override
    public void cloudTick(CastContext ctx, Vec3 center, float radius, List<LivingEntity> inside) {
        CompiledPage page = PageSpells.get(ctx.spellId());
        if (page == null) return;
        float strength = page.param("cloud_strength", 1f);
        for (LivingEntity target : inside) {
            if (target.isAlive() && !target.isSpectator()) PageStrike.hit(ctx, page, target, strength);
        }
    }

    @Override
    public void pulse(CastContext ctx, Vec3 center, float radius) {
        CompiledPage page = PageSpells.get(ctx.spellId());
        if (page == null) return;
        List<LivingEntity> around = ctx.level().getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius),
                target -> target.isAlive() && !target.isSpectator() && !ctx.isCaster(target)
                        && target.distanceToSqr(center) <= radius * radius);
        around.forEach(target -> PageStrike.hit(ctx, page, target, 1f));
        forEachHeart(ctx, page, 1f, (hit, behavior) -> behavior.pulse(hit, center, radius));
        if (radius > MAX_BLOCK_RADIUS || !usesBlocks(ctx)) return;
        BlockPos origin = BlockPos.containing(center);
        int reach = Math.round(radius);
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-reach, -2, -reach), origin.offset(reach, 2, reach))) {
            if (pos.distSqr(origin) <= (double) reach * reach) hitBlock(ctx, pos.immutable());
        }
    }

    public void self(CastContext ctx) {
        CompiledPage page = PageSpells.get(ctx.spellId());
        if (page == null || ctx.caster() == null) return;
        PageStrike.hit(ctx, page, ctx.caster(), 1f);
        Vec3 center = ctx.caster().position();
        forEachHeart(ctx, page, 1f, (hit, behavior) -> behavior.pulse(hit, center, hit.param("radius", 4f)));
    }

    private static void forEachHeart(CastContext ctx, CompiledPage page, float strength, HeartCall call) {
        PageCatalog catalog = PageCatalog.get();
        for (String id : page.hearts()) {
            PageEffect effect = catalog.effect(id);
            HeartBehavior behavior = HeartEffects.get(id);
            if (effect != null && behavior != null) call.run(new PageHit(ctx, page, effect, strength), behavior);
        }
    }

    @FunctionalInterface
    private interface HeartCall {
        void run(PageHit hit, HeartBehavior behavior);
    }
}
