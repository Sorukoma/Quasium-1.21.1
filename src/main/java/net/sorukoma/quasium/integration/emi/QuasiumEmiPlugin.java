package net.sorukoma.quasium.integration.emi;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiInfoRecipe;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.Comparison;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.component.ModDataComponents;
import net.sorukoma.quasium.magic.NoteProfiles;
import net.sorukoma.quasium.selection.world.LootOnlyItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.sorukoma.quasium.block.ModBlocks;
import net.sorukoma.quasium.client.RecipeGate;
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

        registry.addWorkstation(QuasiumEmiCategories.DRYING, EmiStack.of(ModBlocks.DRYING_SHELF_ITEM.get()));

        registry.addWorkstation(QuasiumEmiCategories.FERMENTATION, EmiStack.of(ModBlocks.FERMENTATION_BARREL_ITEM.get()));
        registry.addWorkstation(QuasiumEmiCategories.FERMENTATION, EmiStack.of(ModItems.NETHERITE_HAMMER.get()));

        registry.addWorkstation(QuasiumEmiCategories.SOAKING, EmiStack.of(ModBlocks.SOAKING_BOWL_ITEM.get()));
        registry.addWorkstation(QuasiumEmiCategories.SOAKING, EmiStack.of(Items.STICK));

        registry.addWorkstation(QuasiumEmiCategories.ANVIL, EmiStack.of(ModBlocks.QUASIUM_ANVIL_ITEM.get()));
        registry.addWorkstation(QuasiumEmiCategories.ANVIL, EmiStack.of(ModItems.NETHERITE_HAMMER.get()));

        registry.addWorkstation(QuasiumEmiCategories.ESSENCE, EmiStack.of(ModBlocks.ESSENCE_PRESS_ITEM.get()));

        registry.addCategory(QuasiumEmiCategories.DISTILLATION);
        registry.addWorkstation(QuasiumEmiCategories.DISTILLATION, EmiStack.of(ModBlocks.DISTILLATION_CUBE_ITEM.get()));
        registry.addWorkstation(QuasiumEmiCategories.DISTILLATION, EmiStack.of(ModItems.KAIFOMETER.get()));
        Comparison byEssence = Comparison.compareData(stack -> stack.get(ModDataComponents.ESSENCE_TYPE.get()));
        registry.setDefaultComparison(ModItems.ESSENCE_BOTTLE.get(), byEssence);
        registry.setDefaultComparison(ModItems.AROMA_VIAL.get(), byEssence);
        registry.setDefaultComparison(ModItems.SELF_MIXED_TOBACCO.get(), Comparison.compareData(stack -> stack.get(DataComponents.ITEM_NAME)));
        for (LootOnlyItems.Entry entry : LootOnlyItems.entries()) {
            registry.addRecipe(new EmiInfoRecipe(entry.stacks().stream().<EmiIngredient>map(EmiStack::of).toList(), List.of(entry.text()),
                    ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "/loot_info/" + entry.id())));
        }
        NoteProfiles.known().forEach((id, profile) -> {
            if (!profile.isEntity()) registry.addRecipe(new GatedEmiRecipe(new DistillationEmiRecipe(id, profile), RecipeGate.DISTILLATION));
        });

        DryingRecipes.init();
        List<DryingRecipes.DryingRecipe> dryingRecipes = DryingRecipes.getRecipes();
        for (int i = 0; i < dryingRecipes.size(); i++) {
            DryingRecipes.DryingRecipe drying = dryingRecipes.get(i);
            EmiRecipe shelf = new GatedEmiRecipe(new DryingEmiRecipe(drying, i), RecipeGate.DRYING);
            registry.addRecipe(drying.gate() == null ? shelf : new GatedEmiRecipe(shelf, drying.gate()));
        }

        List<FermentationRecipes.FermentationRecipe> fermentationRecipes = FermentationRecipes.createRecipes();
        for (int i = 0; i < fermentationRecipes.size(); i++) {
            registry.addRecipe(new GatedEmiRecipe(new FermentationEmiRecipe(fermentationRecipes.get(i), i), RecipeGate.FERMENTATION));
        }

        List<SoakingRecipes.SoakingRecipe> soakingRecipes = SoakingRecipes.createRecipes();
        for (int i = 0; i < soakingRecipes.size(); i++) {
            registry.addRecipe(new GatedEmiRecipe(new SoakingEmiRecipe(soakingRecipes.get(i), i), RecipeGate.SOAKING));
        }

        AnvilRecipes.init();
        List<AnvilRecipes.AnvilRecipe> anvilRecipes = AnvilRecipes.getRecipes();
        for (int i = 0; i < anvilRecipes.size(); i++) {
            AnvilRecipes.AnvilRecipe anvil = anvilRecipes.get(i);
            registry.addRecipe(new GatedEmiRecipe(GatedEmiRecipe.research(new AnvilEmiRecipe(anvil, i), anvil.research()), RecipeGate.ANVIL));
        }

        List<EssenceRecipes.EssenceRecipe> essenceRecipes = EssenceRecipes.createRecipes();
        for (int i = 0; i < essenceRecipes.size(); i++) {
            registry.addRecipe(new GatedEmiRecipe(new EssenceEmiRecipe(essenceRecipes.get(i), i), RecipeGate.ESSENCE));
        }

        EmiVanillaRecipes.gate(registry);
    }
}
