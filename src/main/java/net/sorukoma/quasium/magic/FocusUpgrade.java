package net.sorukoma.quasium.magic;

import net.minecraft.world.item.ItemStack;
import net.sorukoma.quasium.component.ModDataComponents;
import org.jetbrains.annotations.Nullable;

public enum FocusUpgrade {
    RESONATOR("resonator"),
    COLLIMATOR("collimator"),
    COOLER("cooler"),
    SPLITTER("splitter"),
    CONDENSER("condenser"),
    FILTER("filter");

    private final String id;

    FocusUpgrade(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    @Nullable
    public static FocusUpgrade byId(String id) {
        for (FocusUpgrade upgrade : values()) {
            if (upgrade.id.equals(id)) return upgrade;
        }
        return null;
    }

    @Nullable
    public static FocusUpgrade of(ItemStack bowl) {
        return byId(bowl.getOrDefault(ModDataComponents.BOWL_UPGRADE.get(), ""));
    }

    public boolean on(ItemStack bowl) {
        return of(bowl) == this;
    }
}
