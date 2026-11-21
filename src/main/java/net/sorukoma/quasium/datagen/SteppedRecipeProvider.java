package net.sorukoma.quasium.datagen;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.ImpossibleTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;

import java.util.concurrent.CompletableFuture;

public abstract class SteppedRecipeProvider extends RecipeProvider {
    protected static final String STEP = "progression_step";
    protected static final Criterion<ImpossibleTrigger.TriggerInstance> ON_STEP =
            CriteriaTriggers.IMPOSSIBLE.createCriterion(new ImpossibleTrigger.TriggerInstance());

    protected SteppedRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected final void buildRecipes(RecipeOutput output, HolderLookup.Provider registries) {
        super.buildRecipes(ProgressionSteps.gate(output), registries);
    }
}
