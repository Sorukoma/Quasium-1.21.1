package net.sorukoma.quasium.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.block.MixingBlocks;
import net.sorukoma.quasium.item.ModItems;

import java.util.concurrent.CompletableFuture;

public class MixingRecipeProvider implements DataProvider {
    private final RecipeProvider recipes;

    public MixingRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        recipes = new SteppedRecipeProvider(output, registries) {
            @Override
            protected void buildRecipes(RecipeOutput recipeOutput) {
                ShapedRecipeBuilder.shaped(RecipeCategory.MISC, MixingBlocks.MIXING_TABLE_ITEM.get())
                        .pattern("CBC")
                        .pattern("PPP")
                        .pattern("P P")
                        .define('C', Items.COPPER_INGOT)
                        .define('B', ModItems.HOOKAH_BOWL.get())
                        .define('P', ItemTags.PLANKS)
                        .unlockedBy(STEP, ON_STEP)
                        .save(recipeOutput, ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "mixing_table"));
            }
        };
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        return recipes.run(output);
    }

    @Override
    public String getName() {
        return "Mixing table recipes";
    }
}
