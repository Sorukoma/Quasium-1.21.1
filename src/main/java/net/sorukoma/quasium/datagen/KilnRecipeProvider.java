package net.sorukoma.quasium.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Items;
import net.sorukoma.quasium.draft.DraftBlocks;
import net.sorukoma.quasium.kiln.KilnBlocks;

import java.util.concurrent.CompletableFuture;

public class KilnRecipeProvider implements DataProvider {
    private final Recipes recipes;

    public KilnRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        recipes = new Recipes(output, registries);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return recipes.run(cache);
    }

    @Override
    public String getName() {
        return "Kiln recipes";
    }

    private static final class Recipes extends SteppedRecipeProvider {
        private Recipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries);
        }

        @Override
        protected void buildRecipes(RecipeOutput output) {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, KilnBlocks.SMOKE_KILN_ITEM.get())
                    .pattern("BIB")
                    .pattern("BPB")
                    .pattern("BCB")
                    .define('B', Items.BRICKS)
                    .define('I', Items.IRON_INGOT)
                    .define('P', DraftBlocks.COPPER_PIPE_ITEM.get())
                    .define('C', Items.CAMPFIRE)
                    .unlockedBy(STEP, ON_STEP)
                    .save(output);
        }
    }
}
