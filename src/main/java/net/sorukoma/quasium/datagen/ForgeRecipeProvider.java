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
import net.sorukoma.quasium.draft.ForgeBlocks;
import net.sorukoma.quasium.item.ModItems;

import java.util.concurrent.CompletableFuture;

public class ForgeRecipeProvider implements DataProvider {
    private final Recipes recipes;

    public ForgeRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        recipes = new Recipes(output, registries);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return recipes.run(cache);
    }

    @Override
    public String getName() {
        return "Forge recipes";
    }

    private static final class Recipes extends SteppedRecipeProvider {
        private Recipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries);
        }

        @Override
        protected void buildRecipes(RecipeOutput output) {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ForgeBlocks.PNEUMATIC_HAMMER_ITEM.get())
                    .pattern("C ")
                    .pattern("PQ")
                    .pattern("I ")
                    .define('C', DraftBlocks.COPPER_PIPE_ITEM.get())
                    .define('P', Items.PISTON)
                    .define('Q', ModItems.QUASIUMINGOT.get())
                    .define('I', Items.IRON_BLOCK)
                    .unlockedBy(STEP, ON_STEP)
                    .save(output);
            ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ForgeBlocks.SMOKE_CANNON_ITEM.get())
                    .pattern("PPP")
                    .pattern("CDC")
                    .pattern("CQC")
                    .define('P', DraftBlocks.COPPER_PIPE_ITEM.get())
                    .define('C', Items.COPPER_INGOT)
                    .define('D', Items.DISPENSER)
                    .define('Q', ModItems.QUASIUMINGOT.get())
                    .unlockedBy(STEP, ON_STEP)
                    .save(output);
        }
    }
}
