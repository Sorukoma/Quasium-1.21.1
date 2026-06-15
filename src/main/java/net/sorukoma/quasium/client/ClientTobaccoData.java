package net.sorukoma.quasium.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.HashSet;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
public class ClientTobaccoData {
    private static Set<String> triedFlavors = new HashSet<>();
    private static Set<Float> triedSelfMixedStrengths = new HashSet<>();

    public static Set<String> getTriedFlavors() {
        return triedFlavors;
    }

    public static Set<Float> getTriedSelfMixedStrengths() {
        return triedSelfMixedStrengths;
    }

    public static void setTriedFlavors(Set<String> flavors) {
        triedFlavors = new HashSet<>(flavors);
    }

    public static void setTriedSelfMixedStrengths(Set<Float> strengths) {
        triedSelfMixedStrengths = new HashSet<>(strengths);
    }
}