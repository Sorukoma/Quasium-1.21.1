package net.sorukoma.quasium.recipe;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredItem;
import net.sorukoma.quasium.block.ModBlocks;
import net.sorukoma.quasium.item.ModItems;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class AnvilRecipes {

    private static final List<AnvilRecipe> recipes = new ArrayList<>();
    private static boolean initialized = false;

    public record AnvilRecipe(List<Supplier<Item>> inputs, DeferredItem<Item> output, int hitsRequired, int kaifLevel) {}

    public static void init() {
        if (initialized) return;
        initialized = true;

        // Backpack Hookah = Hookah Block + Leather, 5 hits, kaif 50
        add(
                List.of(() -> ModBlocks.HOOKAH_BLOCK_ITEM.get(), () -> net.minecraft.world.item.Items.LEATHER),
                ModItems.BACKPACK_HOOKAH,
                5,
                50
        );
    }

    public static void add(List<Supplier<Item>> inputs, DeferredItem<Item> output, int hitsRequired, int kaifLevel) {
        recipes.add(new AnvilRecipe(inputs, output, hitsRequired, kaifLevel));
    }

    /**
     * Find a matching recipe for the given slot contents.
     * Slot order matters — inputs must match in sequence.
     * Empty trailing slots are allowed.
     */
    public static AnvilRecipe find(List<ItemStack> slotContents) {
        for (AnvilRecipe recipe : recipes) {
            boolean matches = true;
            for (int i = 0; i < recipe.inputs().size(); i++) {
                ItemStack slotStack = i < slotContents.size() ? slotContents.get(i) : ItemStack.EMPTY;
                if (slotStack.isEmpty() || !slotStack.is(recipe.inputs().get(i).get())) {
                    matches = false;
                    break;
                }
            }
            // Remaining slots must be empty
            for (int i = recipe.inputs().size(); i < slotContents.size(); i++) {
                if (!slotContents.get(i).isEmpty()) {
                    matches = false;
                    break;
                }
            }
            if (matches) return recipe;
        }
        return null;
    }

    public static List<AnvilRecipe> getRecipes() {
        return Collections.unmodifiableList(recipes);
    }
}
