package net.sorukoma.quasium.mind;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.sorukoma.quasium.advancements.triggers.SmokecraftTrigger;
import net.sorukoma.quasium.component.ModDataComponents;
import net.sorukoma.quasium.data.PlayerFlavorPreferenceData;
import net.sorukoma.quasium.data.PlayerTobaccoData;
import net.sorukoma.quasium.magic.SmokeSource;

import java.util.Map;

public final class Tolerance {
    private static final String DECAYED_AT = "ToleranceDecayedAt";
    private static final int HOUR_TICKS = 1000;

    private Tolerance() {
    }

    public static float of(Player player, ModDataComponents.HookahBowlContents bowl) {
        Map<String, Double> weights = PlayerFlavorPreferenceData.flavorWeights(bowl);
        double total = weights.values().stream().mapToDouble(Double::doubleValue).sum();
        if (total <= 0) return 0f;
        double tolerance = 0;
        for (Map.Entry<String, Double> entry : weights.entrySet()) {
            tolerance += PlayerTobaccoData.tolerance(player, entry.getKey()) * entry.getValue();
        }
        return (float) (tolerance / total);
    }

    public static int dampen(Player player, ModDataComponents.HookahBowlContents bowl, int kaif) {
        return MindMath.round(kaif * MindMath.toleranceMultiplier(of(player, bowl)), player.getRandom().nextFloat());
    }

    public static void smoked(ServerPlayer player, ModDataComponents.HookahBowlContents bowl, int units) {
        Map<String, Double> weights = PlayerFlavorPreferenceData.flavorWeights(bowl);
        double total = weights.values().stream().mapToDouble(Double::doubleValue).sum();
        if (total <= 0) return;
        float grams = units / (float) SmokeSource.UNITS_PER_GRAM;
        for (Map.Entry<String, Double> entry : weights.entrySet()) {
            if (PlayerTobaccoData.addSessionGrams(player, entry.getKey(), (float) (grams * entry.getValue() / total)) > 0) {
                SmokecraftTrigger.fire(player, "tolerance");
            }
        }
    }

    public static void addAll(ServerPlayer player, int amount) {
        for (String flavor : PlayerFlavorPreferenceData.FLAVORS) PlayerTobaccoData.addTolerance(player, flavor, amount);
    }

    static void decay(ServerPlayer player) {
        CompoundTag root = MindData.root(player);
        long now = player.level().getGameTime();
        if (!root.contains(DECAYED_AT)) {
            root.putLong(DECAYED_AT, now);
            return;
        }
        long hours = (now - root.getLong(DECAYED_AT)) / HOUR_TICKS;
        if (hours <= 0) return;
        root.putLong(DECAYED_AT, root.getLong(DECAYED_AT) + hours * HOUR_TICKS);
        int loss = (int) Math.min(MindConfig.TOLERANCE_CAP.getAsInt(), hours * MindConfig.TOLERANCE_DECAY_PER_HOUR.getAsInt());
        for (String flavor : PlayerTobaccoData.toleranceTag(player).getAllKeys()) PlayerTobaccoData.addTolerance(player, flavor, -loss);
    }
}
