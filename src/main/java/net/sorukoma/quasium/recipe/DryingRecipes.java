package net.sorukoma.quasium.recipe;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.registries.DeferredItem;
import net.sorukoma.quasium.block.ModBlocks;
import net.sorukoma.quasium.item.ModItems;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class DryingRecipes {

    private static final List<DryingRecipe> recipes = new ArrayList<>();
    private static boolean initialized = false;

    public record DryingRecipe(Supplier<Item> input, Supplier<Item> output, int dryingTime) {}

    public static void init() {
        if (initialized) return;
        initialized = true;

        // Гнилая плоть → жир (400 тиков = 20 сек)
        add(() -> Items.ROTTEN_FLESH, ModItems.TALLOW, 400);

        // Не сушенный квазиумовый блок → квазиумовый блок (600 тиков = 30 сек)
        add(() -> ModBlocks.UNDRIED_QUASIUM_BLOCK_ITEM.get().asItem(), () -> ModBlocks.QUASIUM_BLOCK_ITEM.get().asItem(), 600);
    }

    public static void add(Supplier<Item> input, Supplier<Item> output, int dryingTime) {
        recipes.add(new DryingRecipe(input, output, dryingTime));
    }

    public static DryingRecipe find(ItemStack stack) {
        if (stack.isEmpty()) return null;
        for (DryingRecipe recipe : recipes) {
            if (stack.is(recipe.input.get())) return recipe;
        }
        return null;
    }

    public static boolean isValidInput(ItemStack stack) {
        return find(stack) != null;
    }

    public static List<DryingRecipe> getRecipes() {
        return Collections.unmodifiableList(recipes);
    }
}