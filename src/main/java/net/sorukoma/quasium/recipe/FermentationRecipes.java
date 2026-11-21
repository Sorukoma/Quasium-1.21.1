package net.sorukoma.quasium.recipe;

import net.minecraft.world.item.ItemStack;
import net.sorukoma.quasium.component.ModDataComponents;
import net.sorukoma.quasium.item.ModItems;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared fermentation recipe data used by both the JEI and EMI integrations.
 */
public class FermentationRecipes {

    public record FermentationRecipe(ItemStack input, ItemStack output, int time) {}

    public static List<FermentationRecipe> createRecipes() {
        List<FermentationRecipe> recipes = new ArrayList<>();
        ItemStack tobaccoInput = new ItemStack(ModItems.TOBACCO_LEAVES.get());
        ItemStack tobaccoOutput = new ItemStack(ModItems.TOBACCO_LEAVES.get());
        tobaccoOutput.set(ModDataComponents.FERMENTED.get(), true);
        recipes.add(new FermentationRecipe(tobaccoInput, tobaccoOutput, 200));

        ItemStack peschanikInput = new ItemStack(ModItems.PESCHANIK_LEAVES.get());
        ItemStack peschanikOutput = new ItemStack(ModItems.PESCHANIK_LEAVES.get());
        peschanikOutput.set(ModDataComponents.FERMENTED.get(), true);
        recipes.add(new FermentationRecipe(peschanikInput, peschanikOutput, 200));

        return recipes;
    }
}
