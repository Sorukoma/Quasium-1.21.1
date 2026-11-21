package net.sorukoma.quasium.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.sorukoma.quasium.component.ModDataComponents;
import net.sorukoma.quasium.mind.Tolerance;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PlayerFlavorPreferenceData {
    private static final String PREFERENCES_KEY = "QuasiumFlavorPreferences";
    public static final List<String> FLAVORS = List.of(
            "mint", "berry", "lemon", "lime", "orange", "grapefruit", "ice", "honey", "nether", "lavender", "cocoa",
            "apple", "watermelon", "grape", "cherry", "vanilla", "coffee", "cinnamon", "coconut", "rose", "anise"
    );
    private static final int[] BALANCED_SCORES = {-2, -2, -2, -2, -1, -1, -1, -1, -1, -1, -1, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2};

    private PlayerFlavorPreferenceData() {
    }

    public static Map<String, Double> flavorWeights(ModDataComponents.HookahBowlContents contents) {
        Map<String, Double> flavorWeights = new LinkedHashMap<>();
        if (contents == null) return flavorWeights;
        for (int groupIndex : contents.slotAssignments().values()) {
            if (groupIndex < 0 || groupIndex >= contents.groups().size()) continue;
            List<String> groupFlavors = parseFlavors(contents.groups().get(groupIndex).flavor());
            if (groupFlavors.isEmpty()) continue;
            double weight = 1.0 / groupFlavors.size();
            for (String groupFlavor : groupFlavors) {
                flavorWeights.merge(groupFlavor, weight, Double::sum);
            }
        }
        return flavorWeights;
    }

    public static double getPreference(Player player, ModDataComponents.HookahBowlContents contents) {
        Map<String, Double> flavorWeights = flavorWeights(contents);
        double totalWeight = flavorWeights.values().stream().mapToDouble(Double::doubleValue).sum();
        if (totalWeight <= 0.0) return 0.0;

        CompoundTag preferences = getOrCreatePreferences(player);
        double weightedPreference = 0.0;
        for (Map.Entry<String, Double> entry : flavorWeights.entrySet()) {
            weightedPreference += preferences.getInt(entry.getKey()) * entry.getValue();
        }
        return weightedPreference / totalWeight;
    }

    public static Map<String, Integer> getAllPreferences(Player player) {
        CompoundTag preferences = getOrCreatePreferences(player);
        Map<String, Integer> result = new LinkedHashMap<>();
        for (String flavor : FLAVORS) {
            result.put(flavor, preferences.getInt(flavor));
        }
        return result;
    }

    public static int score(Player player, String flavor) {
        return getOrCreatePreferences(player).getInt(flavor);
    }

    public static boolean liked(Player player, String flavor) {
        if (flavor == null || flavor.isBlank()) return false;
        return getOrCreatePreferences(player).getInt(flavor) > 0;
    }

    public static int adjustKaif(Player player, ModDataComponents.HookahBowlContents contents, int baseKaif, boolean tolerance) {
        if (baseKaif <= 0) return 0;
        double preference = getPreference(player, contents);
        if (preference <= -1.5) return -Math.max(1, baseKaif / 2);
        if (preference <= 0.0) return 0;
        int liked = preference < 1.5 ? Math.max(1, baseKaif / 2) : baseKaif;
        return tolerance ? Tolerance.dampen(player, contents, liked) : liked;
    }

    private static CompoundTag getOrCreatePreferences(Player player) {
        CompoundTag playerData = player.getPersistentData();
        if (playerData.contains(PREFERENCES_KEY)) {
            return playerData.getCompound(PREFERENCES_KEY);
        }

        List<String> shuffledFlavors = new ArrayList<>(FLAVORS);
        for (int i = shuffledFlavors.size() - 1; i > 0; i--) {
            int swapIndex = player.getRandom().nextInt(i + 1);
            String flavor = shuffledFlavors.get(i);
            shuffledFlavors.set(i, shuffledFlavors.get(swapIndex));
            shuffledFlavors.set(swapIndex, flavor);
        }
        CompoundTag preferences = new CompoundTag();
        for (int i = 0; i < shuffledFlavors.size(); i++) {
            preferences.putInt(shuffledFlavors.get(i), BALANCED_SCORES[i]);
        }
        playerData.put(PREFERENCES_KEY, preferences);
        return preferences;
    }

    private static List<String> parseFlavors(String flavor) {
        if (flavor == null || flavor.isBlank() || "mixed".equals(flavor)) return List.of();
        List<String> flavors = new ArrayList<>();
        for (String part : flavor.split("\\+")) {
            String normalized = part.trim();
            if (FLAVORS.contains(normalized) && !flavors.contains(normalized)) {
                flavors.add(normalized);
            }
        }
        return flavors;
    }
}
