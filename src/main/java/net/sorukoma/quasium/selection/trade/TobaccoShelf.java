package net.sorukoma.quasium.selection.trade;

import net.minecraft.world.item.Item;
import net.sorukoma.quasium.item.ModItems;
import net.sorukoma.quasium.selection.SelectionConfig;

import java.util.List;
import java.util.function.Supplier;

public final class TobaccoShelf {
    public static final int COMMON_LEVELS = 2;
    private static final List<List<Supplier<Item>>> BY_LEVEL = List.of(
            List.of(ModItems.MINT_TOBACCO, ModItems.APPLE_TOBACCO, ModItems.WATERMELON_TOBACCO, ModItems.VANILLA_TOBACCO),
            List.of(ModItems.BERRY_TOBACCO, ModItems.LEMON_TOBACCO, ModItems.LIME_TOBACCO, ModItems.ORANGE_TOBACCO, ModItems.GRAPEFRUIT_TOBACCO),
            List.of(ModItems.COFFEE_TOBACCO, ModItems.ANISE_TOBACCO, ModItems.CHERRY_TOBACCO),
            List.of(ModItems.GRAPE_TOBACCO, ModItems.ROSE_TOBACCO),
            List.of(ModItems.COCONUT_TOBACCO, ModItems.CINNAMON_TOBACCO));

    private TobaccoShelf() {
    }

    public static List<Item> atLevel(int level) {
        return BY_LEVEL.get(level - 1).stream().map(Supplier::get).toList();
    }

    public static List<Item> all() {
        return BY_LEVEL.stream().flatMap(List::stream).map(Supplier::get).toList();
    }

    public static int level(Item tobacco) {
        for (int level = 1; level <= BY_LEVEL.size(); level++) {
            if (atLevel(level).contains(tobacco)) return level;
        }
        throw new IllegalArgumentException(tobacco + " is not on the tobacco shelf");
    }

    public static int price(Item tobacco) {
        return level(tobacco) <= COMMON_LEVELS ? SelectionConfig.TOBACCO_COMMON_PRICE.get() : SelectionConfig.TOBACCO_RARE_PRICE.get();
    }
}
