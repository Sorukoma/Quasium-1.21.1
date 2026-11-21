package net.sorukoma.quasium.hookah;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.sorukoma.quasium.Config;
import net.sorukoma.quasium.component.ModDataComponents.BowlTobaccoGroup;
import net.sorukoma.quasium.component.ModDataComponents.HookahBowlContents;
import net.sorukoma.quasium.data.PlayerFlavorPreferenceData;
import net.sorukoma.quasium.magic.AromaEffects;
import net.sorukoma.quasium.magic.SmokeSource;
import net.sorukoma.quasium.util.SmokeColorUtil;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public final class BowlLayers {
    public static final int ROW_GRAMS = 5;

    private List<Map.Entry<Integer, BowlTobaccoGroup>> layout = List.of();
    private int row = -1;
    private String taste = "";
    private int freshPuffs;

    public void reset(@Nullable HookahBowlContents bowl) {
        layout = bowl == null ? List.of() : bowl.slotAssignments().entrySet().stream()
                .sorted(Map.Entry.<Integer, Integer>comparingByKey().reversed())
                .map(slot -> Map.entry(slot.getKey(), bowl.groups().get(slot.getValue())))
                .toList();
        row = layout.isEmpty() ? -1 : layout.getFirst().getKey() / ROW_GRAMS;
        taste = row < 0 ? "" : taste(rowContents(row));
        freshPuffs = 0;
    }

    @Nullable
    public HookahBowlContents burning(int remainingUnits) {
        int burning = rowAt(remainingUnits);
        return burning < 0 ? null : rowContents(burning);
    }

    public void burned(ServerPlayer smoker, Vec3 top, int remainingUnits) {
        int next = rowAt(remainingUnits);
        if (next == row) return;
        row = next;
        if (next < 0) return;
        HookahBowlContents layer = rowContents(next);
        String now = taste(layer);
        if (now.isEmpty()) return;
        if (!taste.isEmpty() && !now.equals(taste)) {
            freshPuffs = Config.ROW_FRESH_PUFFS.getAsInt();
            HookahSignals.flavorChanged(smoker, top, taste, now, SmokeColorUtil.colorForContents(layer));
        }
        taste = now;
    }

    public boolean fresh() {
        return freshPuffs > 0;
    }

    public void exhaled() {
        if (freshPuffs > 0) freshPuffs--;
    }

    public static String flavor(@Nullable HookahBowlContents row) {
        if (row == null || row.slotAssignments().isEmpty()) return "";
        Map<Integer, Integer> grams = row.getGroupWeights();
        int main = 0;
        for (int group = 1; group < row.groups().size(); group++) {
            if (grams.getOrDefault(group, 0) > grams.getOrDefault(main, 0)) main = group;
        }
        BowlTobaccoGroup group = row.groups().get(main);
        return group.isRegular() ? group.flavor() : AromaEffects.primaryFlavor(group.flavor());
    }

    private static String taste(HookahBowlContents row) {
        String flavor = flavor(row);
        return PlayerFlavorPreferenceData.FLAVORS.contains(flavor) ? flavor : "";
    }

    private int rowAt(int remainingUnits) {
        if (layout.isEmpty() || remainingUnits <= 0) return -1;
        int unfinished = (remainingUnits + SmokeSource.UNITS_PER_GRAM - 1) / SmokeSource.UNITS_PER_GRAM;
        return layout.get(Math.clamp(layout.size() - unfinished, 0, layout.size() - 1)).getKey() / ROW_GRAMS;
    }

    private HookahBowlContents rowContents(int row) {
        HookahBowlContents contents = HookahBowlContents.cleared();
        for (Map.Entry<Integer, BowlTobaccoGroup> slot : layout) {
            if (slot.getKey() / ROW_GRAMS == row) contents = contents.addSlot(slot.getKey(), slot.getValue());
        }
        return contents;
    }
}
