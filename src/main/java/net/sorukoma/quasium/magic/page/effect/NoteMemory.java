package net.sorukoma.quasium.magic.page.effect;

import net.sorukoma.quasium.magic.NoteType;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

final class NoteMemory {
    private final Map<NoteType, Long> marks = new EnumMap<>(NoteType.class);
    private final Map<NoteType, Deque<Long>> hits = new EnumMap<>(NoteType.class);

    Set<NoteType> marks(long now) {
        marks.values().removeIf(until -> until <= now);
        return marks.isEmpty() ? EnumSet.noneOf(NoteType.class) : EnumSet.copyOf(marks.keySet());
    }

    void mark(NoteType note, long until) {
        marks.merge(note, until, Math::max);
    }

    void eat(NoteType note) {
        marks.remove(note);
    }

    int hitsSince(NoteType note, long since) {
        Deque<Long> times = hits.get(note);
        if (times == null) return 0;
        while (!times.isEmpty() && times.peekFirst() < since) times.pollFirst();
        return times.size();
    }

    void hit(NoteType note, long now) {
        hits.computeIfAbsent(note, key -> new ArrayDeque<>()).addLast(now);
    }

    void forget(NoteType note) {
        hits.remove(note);
    }

    boolean faded(long now, long hitsSince) {
        for (NoteType note : NoteType.values()) hitsSince(note, hitsSince);
        hits.values().removeIf(Deque::isEmpty);
        return marks(now).isEmpty() && hits.isEmpty();
    }
}
