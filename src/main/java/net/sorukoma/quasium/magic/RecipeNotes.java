package net.sorukoma.quasium.magic;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class RecipeNotes {
    private record Made(Recipe<?> recipe, int count) {
    }

    private final Map<Item, List<Made>> byResult = new HashMap<>();
    private final NoteDerivation.Tuning tuning;

    RecipeNotes(Collection<RecipeHolder<?>> recipes, HolderLookup.Provider registries, NoteDerivation.Tuning tuning) {
        this.tuning = tuning;
        recipes.stream().sorted(Comparator.comparing(RecipeHolder::id)).forEach(holder -> {
            Recipe<?> recipe = holder.value();
            ItemStack result = recipe.getResultItem(registries);
            if (result.isEmpty() || recipe.getIngredients().isEmpty()) return;
            byResult.computeIfAbsent(result.getItem(), item -> new ArrayList<>()).add(new Made(recipe, result.getCount()));
        });
    }

    boolean produces(Item item) {
        return byResult.containsKey(item);
    }

    @Nullable
    Map<NoteType, Integer> mix(Item item, Map<Item, Map<NoteType, Integer>> known) {
        for (Made made : byResult.getOrDefault(item, List.of())) {
            List<Map<NoteType, Integer>> parts = ingredientNotes(made.recipe, item, known);
            if (parts == null || parts.isEmpty()) continue;
            Map<NoteType, Integer> notes = NoteMix.blend(parts, made.count, tuning.recipePct());
            if (notes.isEmpty()) continue;
            return tuning.limit(made.recipe instanceof AbstractCookingRecipe ? NoteMix.plus(notes, NoteType.EMBER, tuning.cookingEmber()) : notes);
        }
        return null;
    }

    @Nullable
    private static List<Map<NoteType, Integer>> ingredientNotes(Recipe<?> recipe, Item result, Map<Item, Map<NoteType, Integer>> known) {
        List<Map<NoteType, Integer>> parts = new ArrayList<>();
        for (Ingredient ingredient : recipe.getIngredients()) {
            if (ingredient.isEmpty()) continue;
            Map<NoteType, Integer> notes = firstKnown(ingredient, result, known);
            if (notes == null) return null;
            parts.add(notes);
        }
        return parts;
    }

    @Nullable
    private static Map<NoteType, Integer> firstKnown(Ingredient ingredient, Item result, Map<Item, Map<NoteType, Integer>> known) {
        for (ItemStack option : ingredient.getItems()) {
            if (option.is(result)) continue;
            Map<NoteType, Integer> notes = known.get(option.getItem());
            if (notes != null) return notes;
        }
        return null;
    }
}
