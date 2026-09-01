package net.sorukoma.quasium.integration.emi;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.world.item.Items;
import net.sorukoma.quasium.block.ModBlocks;
import net.sorukoma.quasium.item.ModItems;
import net.sorukoma.quasium.recipe.AnvilRecipes;
import net.sorukoma.quasium.recipe.DryingRecipes;
import net.sorukoma.quasium.recipe.EssenceRecipes;
import net.sorukoma.quasium.recipe.FermentationRecipes;
import net.sorukoma.quasium.recipe.SoakingRecipes;

import java.util.List;

@EmiEntrypoint
public class QuasiumEmiPlugin implements EmiPlugin {

    @Override
    public void register(EmiRegistry registry) {
        registry.addCategory(QuasiumEmiCategories.DRYING);
        registry.addCategory(QuasiumEmiCategories.FERMENTATION);
        registry.addCategory(QuasiumEmiCategories.SOAKING);
        registry.addCategory(QuasiumEmiCategories.ANVIL);
        registry.addCategory(QuasiumEmiCategories.ESSENCE);

        // Workstations (mirrors the JEI recipe catalysts)
        registry.addWorkstation(QuasiumEmiCategories.DRYING, EmiStack.of(ModBlocks.DRYING_SHELF_ITEM.get()));

        registry.addWorkstation(QuasiumEmiCategories.FERMENTATION, EmiStack.of(ModBlocks.FERMENTATION_BARREL_ITEM.get()));
        registry.addWorkstation(QuasiumEmiCategories.FERMENTATION, EmiStack.of(ModItems.NETHERITE_HAMMER.get()));

        registry.addWorkstation(QuasiumEmiCategories.SOAKING, EmiStack.of(ModBlocks.SOAKING_BOWL_ITEM.get()));
        registry.addWorkstation(QuasiumEmiCategories.SOAKING, EmiStack.of(Items.STICK));

        registry.addWorkstation(QuasiumEmiCategories.ANVIL, EmiStack.of(ModBlocks.QUASIUM_ANVIL_ITEM.get()));
        registry.addWorkstation(QuasiumEmiCategories.ANVIL, EmiStack.of(ModItems.NETHERITE_HAMMER.get()));

        registry.addWorkstation(QuasiumEmiCategories.ESSENCE, EmiStack.of(ModBlocks.ESSENCE_PRESS_ITEM.get()));

        // Recipes
        DryingRecipes.init();
        List<DryingRecipes.DryingRecipe> dryingRecipes = DryingRecipes.getRecipes();
        for (int i = 0; i < dryingRecipes.size(); i++) {
            registry.addRecipe(new DryingEmiRecipe(dryingRecipes.get(i), i));
        }

        List<FermentationRecipes.FermentationRecipe> fermentationRecipes = FermentationRecipes.createRecipes();
        for (int i = 0; i < fermentationRecipes.size(); i++) {
            registry.addRecipe(new FermentationEmiRecipe(fermentationRecipes.get(i), i));
        }

        List<SoakingRecipes.SoakingRecipe> soakingRecipes = SoakingRecipes.createRecipes();
        for (int i = 0; i < soakingRecipes.size(); i++) {
            registry.addRecipe(new SoakingEmiRecipe(soakingRecipes.get(i), i));
        }

        AnvilRecipes.init();
        List<AnvilRecipes.AnvilRecipe> anvilRecipes = AnvilRecipes.getRecipes();
        for (int i = 0; i < anvilRecipes.size(); i++) {
            registry.addRecipe(new AnvilEmiRecipe(anvilRecipes.get(i), i));
        }

        List<EssenceRecipes.EssenceRecipe> essenceRecipes = EssenceRecipes.createRecipes();
        for (int i = 0; i < essenceRecipes.size(); i++) {
            registry.addRecipe(new EssenceEmiRecipe(essenceRecipes.get(i), i));
        }
    }
}
