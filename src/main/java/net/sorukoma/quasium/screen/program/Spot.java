package net.sorukoma.quasium.screen.program;

import net.sorukoma.quasium.golem.program.ProgramPath;

import java.util.ArrayList;
import java.util.List;

public record Spot(ProgramPath path, int x, int y, boolean mouth) {
    public double distance(int pointX, int pointY) {
        return Math.hypot(pointX - x, pointY - y);
    }

    public ProgramPath beforeRemoving(ProgramPath removed) {
        int level = removed.steps().size() - 1;
        List<Integer> steps = path.steps();
        if (steps.size() <= level || !steps.subList(0, level).equals(removed.steps().subList(0, level))
                || steps.get(level) < removed.steps().get(level)) return path;
        List<Integer> shifted = new ArrayList<>(steps);
        shifted.set(level, steps.get(level) + 1);
        return new ProgramPath(shifted);
    }
}
