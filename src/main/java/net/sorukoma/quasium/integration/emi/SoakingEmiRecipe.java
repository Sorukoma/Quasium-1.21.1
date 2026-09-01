package net.sorukoma.quasium.integration.emi;

import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.Comparison;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.recipe.SoakingRecipes;

import java.util.ArrayList;
import java.util.List;

public class SoakingEmiRecipe extends BasicEmiRecipe {
    private static final int WIDTH = 130;
    private static final int HEIGHT = 60;
    private final boolean hasMolasses;
    private final boolean hasEssence;

    public SoakingEmiRecipe(SoakingRecipes.SoakingRecipe recipe, int index) {
        super(QuasiumEmiCategories.SOAKING,
                ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "/soaking/" + index),
                WIDTH, HEIGHT);

        List<EmiIngredient> in = new ArrayList<>();
        in.add(EmiStack.of(recipe.fermentedLeaves()));
        this.hasEssence = recipe.essences() != null && !recipe.essences().isEmpty();
        if (hasEssence) {
            in.add(EmiIngredient.of(recipe.essences().stream()
                    .map(e -> (EmiIngredient) EmiStack.of(e).comparison(Comparison.compareComponents()))
                    .toList()));
        }
        this.hasMolasses = recipe.molasses() != null;
        if (hasMolasses) {
            in.add(EmiStack.of(recipe.molasses()));
        }
        this.inputs = in;
        this.outputs = List.of(EmiStack.of(recipe.output()));
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        widgets.addSlot(inputs.get(0), 0, 9);
        if (hasEssence) {
            widgets.addSlot(inputs.get(1), 0, 31);
        }
        if (hasMolasses) {
            widgets.addSlot(inputs.get(hasEssence ? 2 : 1), 22, 31);
        }
        widgets.addTexture(EmiTexture.EMPTY_ARROW, 60, 10);
        widgets.addSlot(outputs.get(0), 100, 9).recipeContext(this);

        widgets.addText(Component.literal("10s"), 70, 31, 0xFF808080, false);
    }
}
