package net.sorukoma.quasium.ecology;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.datagen.SteppedRecipeProvider;
import net.sorukoma.quasium.tea.TeaBlocks;

import java.util.concurrent.CompletableFuture;

public class EcologyRecipes extends SteppedRecipeProvider {
    public EcologyRecipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, TeaBlocks.TEA_SEEDS.get(), 2)
                .requires(ItemTags.LEAVES)
                .requires(Items.WHEAT_SEEDS)
                .requires(Items.BONE_MEAL)
                .unlockedBy(STEP, ON_STEP)
                .save(output, QuasiumMod.MOD_ID + ":tea_seeds_from_leaves");
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, TeaBlocks.SAMOVAR_ITEM.get())
                .pattern("GCG")
                .pattern("CBC")
                .pattern("CFC")
                .define('G', Items.GOLD_NUGGET)
                .define('C', Items.COPPER_INGOT)
                .define('B', Items.BUCKET)
                .define('F', Items.CAMPFIRE)
                .unlockedBy(STEP, ON_STEP)
                .save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, TeaBlocks.TEAPOT.get())
                .pattern(" W ")
                .pattern("WDW")
                .pattern(" W ")
                .define('W', Items.WHITE_TERRACOTTA)
                .define('D', Items.BLUE_DYE)
                .unlockedBy(STEP, ON_STEP)
                .save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, TeaBlocks.TEA_CUP.get(), 4)
                .pattern("W W")
                .pattern(" W ")
                .define('W', Items.WHITE_TERRACOTTA)
                .unlockedBy(STEP, ON_STEP)
                .save(output);
    }
}
