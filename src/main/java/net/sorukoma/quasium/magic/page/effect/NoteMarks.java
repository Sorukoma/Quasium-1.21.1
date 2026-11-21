package net.sorukoma.quasium.magic.page.effect;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.sorukoma.quasium.magic.CastContext;
import net.sorukoma.quasium.magic.MagicMath;
import net.sorukoma.quasium.magic.NoteType;
import net.sorukoma.quasium.magic.page.CompiledPage;
import net.sorukoma.quasium.magic.page.PageCatalog;
import net.sorukoma.quasium.magic.page.PageConfig;

import java.util.List;
import java.util.Set;

public final class NoteMarks {
    private static final int SPARKLE_EVERY_TICKS = 10;

    private NoteMarks() {
    }

    public static void afterHit(CastContext ctx, CompiledPage page, LivingEntity target, List<NoteType> notes, float strength) {
        if (notes.isEmpty()) return;
        long now = target.level().getGameTime();
        NoteMemory memory = target.getData(HeartRegistry.NOTE_MEMORY);
        PageCatalog catalog = PageCatalog.get();
        for (NoteType mark : memory.marks(now)) {
            for (NoteType note : notes) {
                String accord = catalog.accordOf(mark, note);
                if (accord == null || page.accords().contains(accord)) continue;
                memory.eat(mark);
                AccordStrike.sound(ctx, page, accord, target, strength);
                break;
            }
        }
        long until = now + (long) PageConfig.NOTE_MARK_SECONDS.getAsInt() * MagicMath.TICKS_PER_SECOND;
        notes.forEach(note -> memory.mark(note, until));
        sparkle(ctx.level(), target, notes);
    }

    static void tick(LivingEntity entity) {
        if (entity.tickCount % SPARKLE_EVERY_TICKS != 0 || !(entity.level() instanceof ServerLevel level)) return;
        if (!entity.hasData(HeartRegistry.NOTE_MEMORY)) return;
        NoteMemory memory = entity.getData(HeartRegistry.NOTE_MEMORY);
        long now = level.getGameTime();
        sparkle(level, entity, memory.marks(now));
        if (memory.faded(now, Tolerance.windowStart(now))) entity.removeData(HeartRegistry.NOTE_MEMORY);
    }

    private static void sparkle(ServerLevel level, LivingEntity target, Iterable<NoteType> notes) {
        for (NoteType note : notes) {
            level.sendParticles(new DustParticleOptions(Vec3.fromRGB24(note.color()).toVector3f(), 0.8f),
                    target.getX(), target.getY(0.7), target.getZ(), 2, target.getBbWidth() / 2, target.getBbHeight() / 4, target.getBbWidth() / 2, 0);
        }
    }

    public static Set<NoteType> live(LivingEntity target) {
        if (!target.hasData(HeartRegistry.NOTE_MEMORY)) return Set.of();
        return target.getData(HeartRegistry.NOTE_MEMORY).marks(target.level().getGameTime());
    }
}
