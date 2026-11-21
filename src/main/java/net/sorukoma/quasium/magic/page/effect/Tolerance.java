package net.sorukoma.quasium.magic.page.effect;

import net.minecraft.world.entity.LivingEntity;
import net.sorukoma.quasium.magic.MagicMath;
import net.sorukoma.quasium.magic.NoteType;
import net.sorukoma.quasium.magic.page.PageConfig;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class Tolerance {
    private Tolerance() {
    }

    public static Map<NoteType, Float> strike(LivingEntity target, List<NoteType> notes) {
        Map<NoteType, Float> felt = new EnumMap<>(NoteType.class);
        if (notes.isEmpty()) return felt;
        long now = target.level().getGameTime();
        NoteMemory memory = target.getData(HeartRegistry.NOTE_MEMORY);
        for (NoteType note : notes) {
            int earlier = memory.hitsSince(note, windowStart(now));
            felt.put(note, ToleranceMath.factor(earlier, PageConfig.TOLERANCE_FREE_HITS.getAsInt(),
                    PageConfig.TOLERANCE_STEP_PCT.getAsInt(), PageConfig.TOLERANCE_MAX_PCT.getAsInt()));
            memory.hit(note, now);
        }
        return felt;
    }

    static void reset(LivingEntity target, NoteType first, NoteType second) {
        if (!target.hasData(HeartRegistry.NOTE_MEMORY)) return;
        NoteMemory memory = target.getData(HeartRegistry.NOTE_MEMORY);
        memory.forget(first);
        memory.forget(second);
    }

    static long windowStart(long now) {
        return now - (long) PageConfig.TOLERANCE_WINDOW_SECONDS.getAsInt() * MagicMath.TICKS_PER_SECOND;
    }
}
