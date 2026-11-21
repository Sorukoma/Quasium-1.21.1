package net.sorukoma.quasium.recipe;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.sorukoma.quasium.block.ModBlocks;
import net.sorukoma.quasium.item.ModItemTags;
import net.sorukoma.quasium.item.ModItems;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.IntSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class DryingRecipes {

    private static final List<DryingRecipe> recipes = new ArrayList<>();
    private static boolean initialized = false;

    public record DryingRecipe(Predicate<ItemStack> inputPredicate, Supplier<Item> displayInput,
                               Supplier<Item> output, IntSupplier ticks, @Nullable ResourceLocation gate) {
        public boolean matches(ItemStack stack) {
            return inputPredicate.test(stack);
        }

        public int dryingTime() {
            return ticks.getAsInt();
        }
    }

    public static void init() {
        if (initialized) return;
        initialized = true;

        add(() -> Items.ROTTEN_FLESH, ModItems.TALLOW, 400);
        add(() -> ModBlocks.UNDRIED_QUASIUM_BLOCK_ITEM.get().asItem(),
                () -> ModBlocks.QUASIUM_BLOCK_ITEM.get().asItem(), 600);

        addTag(ModItemTags.RAW_BEEF, () -> Items.BEEF, ModItems.BEEF_JERKY, 2400);
        addTag(ModItemTags.RAW_PORK, () -> Items.PORKCHOP, ModItems.PORK_JERKY, 2400);
        addTag(ModItemTags.RAW_CHICKEN, () -> Items.CHICKEN, ModItems.CHICKEN_JERKY, 2000);
        addTag(ModItemTags.RAW_MUTTON, () -> Items.MUTTON, ModItems.MUTTON_JERKY, 2200);
        addTag(ModItemTags.RAW_RABBIT, () -> Items.RABBIT, ModItems.RABBIT_JERKY, 2000);
        addTag(ModItemTags.RAW_COD, () -> Items.COD, ModItems.COD_JERKY, 1800);
        addTag(ModItemTags.RAW_SALMON, () -> Items.SALMON, ModItems.SALMON_JERKY, 2000);
    }

    public static void add(Supplier<Item> input, Supplier<Item> output, int dryingTime) {
        add(input, output, () -> dryingTime, null);
    }

    public static void add(Supplier<Item> input, Supplier<Item> output, IntSupplier ticks, @Nullable ResourceLocation gate) {
        recipes.add(new DryingRecipe(stack -> stack.is(input.get()), input, output, ticks, gate));
    }

    public static void addTag(TagKey<Item> inputTag, Supplier<Item> displayInput,
                              Supplier<Item> output, int dryingTime) {
        recipes.add(new DryingRecipe(stack -> stack.is(inputTag), displayInput, output, () -> dryingTime, null));
    }

    public static DryingRecipe find(ItemStack stack) {
        if (stack.isEmpty()) return null;
        for (DryingRecipe recipe : recipes) {
            if (recipe.matches(stack)) return recipe;
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
