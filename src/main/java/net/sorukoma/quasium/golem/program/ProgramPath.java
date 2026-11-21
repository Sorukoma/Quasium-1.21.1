package net.sorukoma.quasium.golem.program;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;

public record ProgramPath(List<Integer> steps) {
    private static final int MAX_STEPS = 33;
    public static final Codec<ProgramPath> CODEC = Codec.INT.listOf().xmap(ProgramPath::new, ProgramPath::steps);
    public static final StreamCodec<ByteBuf, ProgramPath> STREAM_CODEC = ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(MAX_STEPS))
            .map(ProgramPath::new, ProgramPath::steps);

    public ProgramPath {
        steps = List.copyOf(steps);
    }

    public static ProgramPath top(int index) {
        return new ProgramPath(List.of(index));
    }

    public ProgramPath child(int body, int index) {
        List<Integer> deeper = new ArrayList<>(steps);
        deeper.add(body);
        deeper.add(index);
        return new ProgramPath(deeper);
    }

    public boolean valid() {
        if (steps.size() % 2 == 0) return false;
        for (int at = 0; at < steps.size(); at++) {
            int step = steps.get(at);
            if (step < 0 || at % 2 == 1 && step > 1) return false;
        }
        return true;
    }

    public int index() {
        return steps.getLast();
    }

    public int enclosing() {
        return steps.size() / 2;
    }

    public boolean inside(ProgramPath outer) {
        return steps.size() > outer.steps.size() && steps.subList(0, outer.steps.size()).equals(outer.steps);
    }

    public ProgramPath afterRemoving(ProgramPath removed) {
        int level = removed.steps.size() - 1;
        if (steps.size() <= level || !steps.subList(0, level).equals(removed.steps.subList(0, level))
                || steps.get(level) <= removed.steps.get(level)) return this;
        List<Integer> shifted = new ArrayList<>(steps);
        shifted.set(level, steps.get(level) - 1);
        return new ProgramPath(shifted);
    }
}
