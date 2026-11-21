package net.sorukoma.quasium.integration.emi;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiInfoRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.resources.ResourceLocation;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.draft.DraftCatalog;

import java.util.List;

@EmiEntrypoint
public class DraftEmiPlugin implements EmiPlugin {
    @Override
    public void register(EmiRegistry registry) {
        for (DraftCatalog.Entry entry : DraftCatalog.registered()) {
            registry.addRecipe(new EmiInfoRecipe(List.<EmiIngredient>of(EmiStack.of(entry.item())), entry.lines(),
                    ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "/draft_info/" + entry.id())));
        }
    }
}
