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
import net.sorukoma.quasium.draft.MachineBlocks;
import net.sorukoma.quasium.item.ModItems;

import java.util.concurrent.CompletableFuture;

public class MachineRecipeProvider implements DataProvider {
    private final Recipes recipes;

    public MachineRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        recipes = new Recipes(output, registries);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return recipes.run(cache);
    }

    @Override
    public String getName() {
        return "Machine recipes";
    }

    private static final class Recipes extends SteppedRecipeProvider {
        private Recipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries);
        }

        @Override
        protected void buildRecipes(RecipeOutput output) {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, MachineBlocks.ESSENCE_PUMP_ITEM.get())
                    .pattern("PGP")
                    .pattern("CRC")
                    .pattern("CCC")
                    .define('P', DraftBlocks.COPPER_PIPE_ITEM.get())
                    .define('G', Items.GLASS)
                    .define('R', Items.REDSTONE)
                    .define('C', Items.COPPER_INGOT)
                    .unlockedBy(STEP, ON_STEP)
                    .save(output);
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, MachineBlocks.FLASK_CHARGER_ITEM.get())
                    .pattern("CQC")
                    .pattern("RPR")
                    .pattern("CCC")
                    .define('Q', ModItems.QUASIUMINGOT.get())
                    .define('R', Items.REDSTONE)
                    .define('P', DraftBlocks.COPPER_PIPE_ITEM.get())
                    .define('C', Items.COPPER_INGOT)
                    .unlockedBy(STEP, ON_STEP)
                    .save(output);
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, MachineBlocks.VENTILATION_ITEM.get())
                    .pattern("C C")
                    .pattern("CPC")
                    .pattern("CRC")
                    .define('P', DraftBlocks.COPPER_PIPE_ITEM.get())
                    .define('R', Items.REDSTONE)
                    .define('C', Items.COPPER_INGOT)
                    .unlockedBy(STEP, ON_STEP)
                    .save(output);
        }
    }
}
