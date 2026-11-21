package net.sorukoma.quasium.magic;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.sorukoma.quasium.component.ModDataComponents.HookahBowlContents;
import org.jetbrains.annotations.Nullable;

public interface SmokeSource {
    ItemStack smokeBowl();

    int UNITS_PER_GRAM = 250;

    boolean burnForCast(ServerPlayer player, int tobaccoUnits);

    default boolean useFlaskCharge() {
        return false;
    }

    default boolean atPeak() {
        return true;
    }

    @Nullable
    default HookahBowlContents burningRow() {
        return null;
    }
}
