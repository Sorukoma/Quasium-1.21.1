package net.sorukoma.quasium.integration.emi;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.world.level.ItemLike;

public final class EmiAutotest {
    private EmiAutotest() {
    }

    public static void showRecipes(ItemLike item) {
        EmiApi.displayRecipes(EmiStack.of(item));
    }
}
