package net.sorukoma.quasium.integration.emi;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.resources.ResourceLocation;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.client.RecipeGate;
import net.sorukoma.quasium.kiln.KilnBlocks;
import net.sorukoma.quasium.magic.NoteType;

@EmiEntrypoint
public class KilnEmiPlugin implements EmiPlugin {
    public static final EmiRecipeCategory CATEGORY = new EmiRecipeCategory(
            ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "kiln"), EmiStack.of(KilnBlocks.SMOKE_KILN_ITEM.get()));

    @Override
    public void register(EmiRegistry registry) {
        registry.addCategory(CATEGORY);
        registry.addWorkstation(CATEGORY, EmiStack.of(KilnBlocks.SMOKE_KILN_ITEM.get()));
        for (NoteType note : NoteType.values()) {
            registry.addRecipe(new GatedEmiRecipe(new KilnEmiRecipe(note), RecipeGate.KILN));
        }
    }
}
