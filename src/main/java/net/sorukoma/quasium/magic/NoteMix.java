package net.sorukoma.quasium.magic;

import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class NoteMix {
    private NoteMix() {
    }

    public static Map<NoteType, Integer> blend(Collection<Map<NoteType, Integer>> parts, int outputs, int pct) {
        EnumMap<NoteType, Integer> sum = new EnumMap<>(NoteType.class);
        parts.forEach(part -> part.forEach((note, ml) -> sum.merge(note, ml, Integer::sum)));
        EnumMap<NoteType, Integer> blended = new EnumMap<>(NoteType.class);
        double share = pct / 100.0 / Math.max(1, outputs);
        sum.forEach((note, ml) -> {
            int amount = (int) Math.round(ml * share);
            if (amount > 0) blended.put(note, amount);
        });
        if (blended.isEmpty() && !sum.isEmpty()) blended.put(dominant(sum), 1);
        return blended;
    }

    public static Map<NoteType, Integer> weaken(Map<NoteType, Integer> notes, int pct) {
        return blend(List.of(notes), 1, pct);
    }

    public static Map<NoteType, Integer> plus(Map<NoteType, Integer> notes, NoteType note, int ml) {
        if (ml <= 0) return notes;
        EnumMap<NoteType, Integer> raised = new EnumMap<>(NoteType.class);
        raised.putAll(notes);
        raised.merge(note, ml, Integer::sum);
        return raised;
    }

    public static Map<NoteType, Integer> limit(Map<NoteType, Integer> notes, int cap, int totalCap) {
        EnumMap<NoteType, Integer> capped = new EnumMap<>(NoteType.class);
        notes.forEach((note, ml) -> {
            if (ml > 0) capped.put(note, Math.min(cap, ml));
        });
        int total = capped.values().stream().mapToInt(Integer::intValue).sum();
        if (total <= totalCap) return capped;
        return shrink(capped, total, Math.max(1, totalCap));
    }

    private static Map<NoteType, Integer> shrink(EnumMap<NoteType, Integer> notes, int total, int target) {
        EnumMap<NoteType, Integer> shrunk = new EnumMap<>(NoteType.class);
        EnumMap<NoteType, Double> rest = new EnumMap<>(NoteType.class);
        notes.forEach((note, ml) -> {
            double exact = (double) ml * target / total;
            shrunk.put(note, (int) exact);
            rest.put(note, exact - (int) exact);
        });
        int missing = target - shrunk.values().stream().mapToInt(Integer::intValue).sum();
        rest.entrySet().stream().sorted(Map.Entry.<NoteType, Double>comparingByValue(Comparator.reverseOrder())).limit(missing)
                .forEach(entry -> shrunk.merge(entry.getKey(), 1, Integer::sum));
        shrunk.values().removeIf(ml -> ml <= 0);
        return shrunk;
    }

    private static NoteType dominant(Map<NoteType, Integer> sum) {
        NoteType best = null;
        for (Map.Entry<NoteType, Integer> entry : sum.entrySet()) {
            if (best == null || entry.getValue() > sum.get(best)) best = entry.getKey();
        }
        return best;
    }
}
