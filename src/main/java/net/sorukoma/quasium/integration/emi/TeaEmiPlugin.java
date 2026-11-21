package net.sorukoma.quasium.integration.emi;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.stack.EmiStack;
import net.sorukoma.quasium.client.RecipeGate;
import net.sorukoma.quasium.tea.SamovarBrew;
import net.sorukoma.quasium.tea.TeaBlocks;

@EmiEntrypoint
public class TeaEmiPlugin implements EmiPlugin {
    @Override
    public void register(EmiRegistry registry) {
        registry.addCategory(SamovarEmiRecipe.CATEGORY);
        registry.addWorkstation(SamovarEmiRecipe.CATEGORY, EmiStack.of(TeaBlocks.SAMOVAR_ITEM.get()));
        registry.addRecipe(new GatedEmiRecipe(new SamovarEmiRecipe(SamovarBrew.current()), RecipeGate.SAMOVAR));
    }
}
