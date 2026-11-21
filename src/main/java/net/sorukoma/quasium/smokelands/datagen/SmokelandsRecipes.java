package net.sorukoma.quasium.smokelands.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.world.item.Items;
import net.sorukoma.quasium.datagen.SteppedRecipeProvider;
import net.sorukoma.quasium.smokelands.block.SmokelandsBlocks;
import net.sorukoma.quasium.smokelands.item.SmokelandsItems;

import java.util.concurrent.CompletableFuture;

public class SmokelandsRecipes implements DataProvider {
    private final Recipes recipes;

    public SmokelandsRecipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        recipes = new Recipes(output, registries);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return recipes.run(cache);
    }

    @Override
    public String getName() {
        return "Smokelands recipes";
    }

    private static final class Recipes extends SteppedRecipeProvider {
        private Recipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries);
        }

        @Override
        protected void buildRecipes(RecipeOutput output) {
            ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, SmokelandsBlocks.ASH_BRICKS.get(), 4)
                    .pattern("AA")
                    .pattern("AA")
                    .define('A', SmokelandsBlocks.ASH_STONE.get())
                    .unlockedBy(STEP, ON_STEP)
                    .save(output);
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, SmokelandsItems.ASH_SACHET.get())
                    .requires(SmokelandsItems.ASH_OF_TIME.get())
                    .requires(Items.PAPER)
                    .requires(Items.STRING)
                    .unlockedBy(STEP, ON_STEP)
                    .save(output);
        }
    }
}
