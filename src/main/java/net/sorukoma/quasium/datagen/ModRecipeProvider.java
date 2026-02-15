package net.sorukoma.quasium.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.conditions.IConditionBuilder;
import net.sorukoma.quasium.item.ModItems;

import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends RecipeProvider implements IConditionBuilder {
    public ModRecipeProvider(PackOutput pOutput, CompletableFuture<HolderLookup.Provider> pRegistries) {
        super(pOutput, pRegistries);
    }

    @Override
    protected void buildRecipes(RecipeOutput pRecipeOutput) {
        SimpleCookingRecipeBuilder.blasting(Ingredient.of(ModItems.RAWQUASIUM.get()), RecipeCategory.MISC, ModItems.QUASIUMINGOT.get(), 0.7f, 100)
                .unlockedBy("has_raw_quasium", has(ModItems.RAWQUASIUM.get()))
                .save(pRecipeOutput, "quasiumingot_from_blasting_rawquasium");
    }
}
