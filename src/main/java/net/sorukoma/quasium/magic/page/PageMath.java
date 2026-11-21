package net.sorukoma.quasium.magic.page;

import net.sorukoma.quasium.magic.NoteType;

import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class PageMath {
    private PageMath() {
    }

    public static int volume(int formVolume, Collection<Integer> heartVolumes, Collection<Integer> topVolumes) {
        int total = formVolume;
        for (int volume : heartVolumes) total += volume;
        for (int volume : topVolumes) total += volume;
        return total;
    }

    public static Map<NoteType, Integer> noteCost(List<NoteType> heartNotes, int extraPerHeart) {
        EnumMap<NoteType, Integer> cost = new EnumMap<>(NoteType.class);
        for (NoteType note : heartNotes) cost.merge(note, 1 + extraPerHeart, Integer::sum);
        return cost;
    }

    public static float grams(float inhaledMl, float gramsPerMl, float extraGrams) {
        return Math.max(0f, inhaledMl) * gramsPerMl + extraGrams;
    }

    public static int overflowStep(float lungs, int volume, int stepMl, int maxSteps) {
        if (stepMl <= 0 || lungs <= volume) return 0;
        return Math.min(maxSteps, (int) ((lungs - volume) / stepMl));
    }

    public static float overflowMultiplier(int step, int pctPerStep) {
        return 1f + step * pctPerStep / 100f;
    }

    public static float capPower(float power, float cap) {
        return Math.min(power, cap);
    }

    public static float stackedScale(float scale, int count) {
        return (float) Math.pow(scale, count);
    }

    public static float fanAngle(int index, int copies, float spreadDegrees) {
        return (index - copies / 2f) * spreadDegrees;
    }
}
