package net.sorukoma.quasium.magic.page.spirit;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.sorukoma.quasium.magic.CastContext;
import net.sorukoma.quasium.magic.MagicMath;
import net.sorukoma.quasium.magic.NoteType;
import net.sorukoma.quasium.magic.SpellAction;
import net.sorukoma.quasium.magic.page.CompiledPage;
import net.sorukoma.quasium.magic.page.PageCatalog;
import net.sorukoma.quasium.magic.page.PageConfig;
import net.sorukoma.quasium.magic.page.PageEffect;
import net.sorukoma.quasium.magic.page.PageSpells;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

public final class SpiritLaunch {
    private static final float SPREAD_DEGREES = 25f;
    private static final double MOUTH_REACH = 0.6;
    private static final double MOUTH_DROP = 0.15;

    private SpiritLaunch() {
    }

    static void launch(CastContext ctx, SpellAction action) {
        ServerPlayer caster = ctx.caster();
        CompiledPage page = PageSpells.get(ctx.spellId());
        if (caster == null || page == null) return;
        int count = Math.max(1, Math.round(ctx.param("count", 1f)));
        SpiritOrders orders = orders(ctx, page, count, SpiritMix.stars(caster));
        LivingEntity looked = SpiritTargets.lookedAt(caster, orders.search());
        Vec3 look = caster.getLookAngle();
        Vec3 mouth = caster.getEyePosition().add(look.scale(MOUTH_REACH)).subtract(0, MOUTH_DROP, 0);
        for (int i = 0; i < count; i++) {
            float spread = (i - (count - 1) / 2f) * SPREAD_DEGREES;
            Vec3 heading = look.yRot((float) Math.toRadians(spread));
            double orbitOffset = Math.PI * 2 * i / count + ctx.level().random.nextDouble();
            ctx.level().addFreshEntity(HookahSpirit.breathe(caster, orders, looked, mouth, heading, orbitOffset));
        }
        if (!orders.silent()) {
            ctx.level().playSound(null, mouth.x, mouth.y, mouth.z, SpiritRegistry.SQUEAK.get(), SoundSource.PLAYERS, 0.6f,
                    SpiritMath.squeakPitch(orders.note()));
        }
        makeRoom(ctx.level(), caster);
    }

    public static List<? extends HookahSpirit> of(ServerLevel level, ServerPlayer owner) {
        return level.getEntities(SpiritRegistry.HOOKAH_SPIRIT.get(), spirit -> spirit.serves(owner) && !spirit.popping());
    }

    private static void makeRoom(ServerLevel level, ServerPlayer owner) {
        List<? extends HookahSpirit> spirits = of(level, owner);
        int extra = spirits.size() - PageConfig.SPIRIT_MAX_PER_PLAYER.getAsInt();
        if (extra <= 0) return;
        spirits.stream()
                .sorted(Comparator.comparingInt((HookahSpirit spirit) -> spirit.tickCount).reversed())
                .limit(extra)
                .forEach(spirit -> spirit.fade(level));
    }

    private static SpiritOrders orders(CastContext ctx, CompiledPage page, int count, int stars) {
        double turn = SpiritMath.turnDegrees(ctx.param("turn", 8f), ctx.param("homing", 0f), stars,
                PageConfig.SPIRIT_HOMING_TURN_PCT.getAsInt(), PageConfig.SPIRIT_STAR_TURN_PCT.getAsInt());
        int lifeTicks = SpiritMath.lifeTicks(ctx.param("lifetime", 8f), stars, PageConfig.SPIRIT_STAR_LIFE_SECONDS.getAsInt());
        float power = SpiritMath.flockPower(ctx.power(), count, PageConfig.FAN_COPY_POWER_PCT.getAsInt());
        return new SpiritOrders(ctx.spellId(), power, ctx.param("speed", 0.6f), turn, ctx.param("search", 16f),
                PageConfig.SPIRIT_LAUNCH_TICKS.getAsInt(), lifeTicks, seconds(ctx, "loyal"), seconds(ctx, "taunt"),
                SpiritTargets.heals(page), ctx.param("silent", 0f) > 0f, page.spell().color(), firstNote(page));
    }

    private static int seconds(CastContext ctx, String key) {
        return Math.round(ctx.param(key, 0f) * MagicMath.TICKS_PER_SECOND);
    }

    @Nullable
    private static NoteType firstNote(CompiledPage page) {
        if (page.hearts().isEmpty()) return null;
        PageEffect effect = PageCatalog.get().effect(page.hearts().getFirst());
        return effect == null ? null : effect.note();
    }
}
