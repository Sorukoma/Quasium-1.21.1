package net.sorukoma.quasium.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.HashSet;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
public class ClientOreLuckData {
    private static Set<Integer> achievedFortuneLevels = new HashSet<>();

    public static Set<Integer> getAchievedFortuneLevels() {
        return achievedFortuneLevels;
    }

    public static void setAchievedFortuneLevels(Set<Integer> levels) {
        achievedFortuneLevels = new HashSet<>(levels);
    }
}