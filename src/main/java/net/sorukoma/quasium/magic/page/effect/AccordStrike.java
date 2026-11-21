package net.sorukoma.quasium.magic.page.effect;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.sorukoma.quasium.magic.CastContext;
import net.sorukoma.quasium.magic.NoteType;
import net.sorukoma.quasium.magic.page.Accord;
import net.sorukoma.quasium.magic.page.AccordBehavior;
import net.sorukoma.quasium.magic.page.AccordEffects;
import net.sorukoma.quasium.magic.page.CompiledPage;
import net.sorukoma.quasium.magic.page.PageCatalog;

public final class AccordStrike {
    private static final float TONE_VOLUME = 0.8f;

    private AccordStrike() {
    }

    public static void sound(CastContext ctx, CompiledPage page, String id, LivingEntity target, float strength) {
        Accord accord = PageCatalog.get().accords().get(id);
        AccordBehavior behavior = AccordEffects.get(id);
        if (accord == null || behavior == null) return;
        behavior.sound(ctx, page, accord, target, strength);
        Tolerance.reset(target, accord.first(), accord.second());
        ring(ctx.level(), page, accord, target);
        if (ctx.caster() != null) AccordJournal.hear(ctx.caster(), id);
    }

    private static void ring(ServerLevel level, CompiledPage page, Accord accord, LivingEntity target) {
        boolean silent = Strikes.silent(page);
        level.sendParticles(new DustParticleOptions(Vec3.fromRGB24(accord.color()).toVector3f(), 1.4f),
                target.getX(), target.getY(0.6), target.getZ(), silent ? 3 : 16, target.getBbWidth() / 2, target.getBbHeight() / 3,
                target.getBbWidth() / 2, 0);
        if (silent) return;
        tone(level, target, accord.first());
        tone(level, target, accord.second());
    }

    private static void tone(ServerLevel level, LivingEntity target, NoteType note) {
        level.playSound(null, target.getX(), target.getY(), target.getZ(), HeartRegistry.tone(note), SoundSource.PLAYERS, TONE_VOLUME, 1f);
    }
}
