package net.sorukoma.quasium.integration.emi;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.resources.ResourceLocation;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.block.MixingBlocks;
import net.sorukoma.quasium.client.RecipeGate;
import net.sorukoma.quasium.recipe.MixingRecipes;

@EmiEntrypoint
public class MixingEmiPlugin implements EmiPlugin {
    public static final EmiRecipeCategory CATEGORY = new EmiRecipeCategory(
            ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "mixing"), EmiStack.of(MixingBlocks.MIXING_TABLE_ITEM.get()));

    @Override
    public void register(EmiRegistry registry) {
        registry.addCategory(CATEGORY);
        registry.addWorkstation(CATEGORY, EmiStack.of(MixingBlocks.MIXING_TABLE_ITEM.get()));
        MixingRecipes.all().forEach((id, recipe) -> registry.addRecipe(new GatedEmiRecipe(
                GatedEmiRecipe.research(new MixingEmiRecipe(id, recipe), recipe.research().orElse(null)), RecipeGate.MIXING)));
    }
}
