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
import net.sorukoma.quasium.draft.GeneratorBlocks;
import net.sorukoma.quasium.item.ModItems;

import java.util.concurrent.CompletableFuture;

public class GeneratorRecipeProvider implements DataProvider {
    private final Recipes recipes;

    public GeneratorRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        recipes = new Recipes(output, registries);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return recipes.run(cache);
    }

    @Override
    public String getName() {
        return "Generator recipes";
    }

    private static final class Recipes extends SteppedRecipeProvider {
        private Recipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries);
        }

        @Override
        protected void buildRecipes(RecipeOutput output) {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, GeneratorBlocks.SMOKE_BOILER_ITEM.get())
                    .pattern("CPC")
                    .pattern("BFB")
                    .pattern("BBB")
                    .define('C', Items.COPPER_INGOT)
                    .define('P', DraftBlocks.COPPER_PIPE_ITEM.get())
                    .define('B', Items.BRICKS)
                    .define('F', Items.FURNACE)
                    .unlockedBy(STEP, ON_STEP)
                    .save(output);
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, GeneratorBlocks.QUASIUM_FAN_ITEM.get())
                    .pattern("QCQ")
                    .pattern("CRC")
                    .pattern("QPQ")
                    .define('Q', ModItems.QUASIUMINGOT.get())
                    .define('C', Items.COPPER_INGOT)
                    .define('R', Items.REDSTONE_BLOCK)
                    .define('P', DraftBlocks.COPPER_PIPE_ITEM.get())
                    .unlockedBy(STEP, ON_STEP)
                    .save(output);
        }
    }
}
