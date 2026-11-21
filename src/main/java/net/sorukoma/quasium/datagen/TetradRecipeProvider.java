package net.sorukoma.quasium.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.sorukoma.quasium.item.ModItems;
import net.sorukoma.quasium.magic.page.PageRegistry;

import java.util.concurrent.CompletableFuture;

public class TetradRecipeProvider implements DataProvider {
    private final Recipes recipes;

    public TetradRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        recipes = new Recipes(output, registries);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return recipes.run(cache);
    }

    @Override
    public String getName() {
        return "Mix notebook recipes";
    }

    private static final class Recipes extends SteppedRecipeProvider {
        private Recipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries);
        }

        @Override
        protected void buildRecipes(RecipeOutput output) {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, PageRegistry.SPELL_TETRAD.get())
                    .requires(Items.BOOK)
                    .requires(Ingredient.of(Items.CHARCOAL, Items.COAL))
                    .requires(ModItems.ESSENCE_BOTTLE.get())
                    .unlockedBy(STEP, ON_STEP)
                    .save(output);
        }
    }
}
