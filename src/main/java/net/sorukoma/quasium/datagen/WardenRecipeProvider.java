package net.sorukoma.quasium.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Items;
import net.sorukoma.quasium.golem.WardenGolems;
import net.sorukoma.quasium.item.ModItems;

import java.util.concurrent.CompletableFuture;

public class WardenRecipeProvider implements DataProvider {
    private final Recipes recipes;

    public WardenRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        recipes = new Recipes(output, registries);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return recipes.run(cache);
    }

    @Override
    public String getName() {
        return "Smoke warden recipes";
    }

    private static final class Recipes extends SteppedRecipeProvider {
        private Recipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries);
        }

        @Override
        protected void buildRecipes(RecipeOutput output) {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, WardenGolems.WARDEN_BLANK.get())
                    .pattern("CQC")
                    .pattern("CCC")
                    .define('C', Items.CLAY)
                    .define('Q', ModItems.QUASIUMINGOT.get())
                    .unlockedBy(STEP, ON_STEP)
                    .save(output);
        }
    }
}
