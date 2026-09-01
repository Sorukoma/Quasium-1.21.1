package net.sorukoma.quasium.item;

import net.minecraft.world.item.ItemStack;
import net.sorukoma.quasium.component.ModDataComponents;

public final class TobaccoVarieties {
    public static final String DIKAR = "dikar";
    public static final String PESCHANIK = "peschanik";
    public static final String BOLOTNIK = "bolotnik";
    public static final String LEDYANOY = "ledyanoy";

    private TobaccoVarieties() {}

    public static boolean isLeaf(ItemStack stack) {
        return stack.getItem() instanceof LeavesItem;
    }

    public static String getVariety(ItemStack stack) {
        if (stack.has(ModDataComponents.TOBACCO_VARIETY.get())) {
            return stack.get(ModDataComponents.TOBACCO_VARIETY.get());
        }
        if (stack.getItem() == ModItems.MAKHORKA_LEAVES.get()) return PESCHANIK;
        return DIKAR;
    }

    public static int strength(String variety) {
        return switch (variety) {
            case PESCHANIK -> 4;
            case BOLOTNIK, DIKAR -> 3;
            case LEDYANOY -> 2;
            default -> 3;
        };
    }

    public static int yield(String variety) {
        return switch (variety) {
            case BOLOTNIK -> 4;
            case DIKAR -> 3;
            default -> 2;
        };
    }

    public static int quality(String variety) {
        return switch (variety) {
            case BOLOTNIK, LEDYANOY -> 4;
            case DIKAR -> 3;
            case PESCHANIK -> 2;
            default -> 3;
        };
    }
}
