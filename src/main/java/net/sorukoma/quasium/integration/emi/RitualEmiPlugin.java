package net.sorukoma.quasium.integration.emi;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.resources.ResourceLocation;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.ritual.RitualRecipes;
import net.sorukoma.quasium.ritual.RitualRegistry;

@EmiEntrypoint
public class RitualEmiPlugin implements EmiPlugin {
    public static final ResourceLocation BRAZIER_RECIPE = ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "ritual_brazier");
    public static final EmiRecipeCategory CATEGORY = new EmiRecipeCategory(
            ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "ritual"), EmiStack.of(RitualRegistry.RITUAL_BRAZIER_ITEM.get()));

    @Override
    public void register(EmiRegistry registry) {
        registry.addCategory(CATEGORY);
        registry.addWorkstation(CATEGORY, EmiStack.of(RitualRegistry.RITUAL_BRAZIER_ITEM.get()));
        RitualRecipes.all().forEach((id, recipe) -> registry.addRecipe(new GatedEmiRecipe(
                GatedEmiRecipe.research(new RitualEmiRecipe(id, recipe), recipe.research().orElse(null)), BRAZIER_RECIPE)));
    }
}
