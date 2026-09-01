package net.sorukoma.quasium.integration.emi;

import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.recipe.FermentationRecipes;

import java.util.List;

public class FermentationEmiRecipe extends BasicEmiRecipe {
    private static final int WIDTH = 90;
    private static final int HEIGHT = 36;
    private final int time;

    public FermentationEmiRecipe(FermentationRecipes.FermentationRecipe recipe, int index) {
        super(QuasiumEmiCategories.FERMENTATION,
                ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "/fermentation/" + index),
                WIDTH, HEIGHT);
        this.inputs = List.of(EmiStack.of(recipe.input()));
        this.outputs = List.of(EmiStack.of(recipe.output()));
        this.time = recipe.time();
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        widgets.addSlot(inputs.get(0), 0, 9);
        widgets.addTexture(EmiTexture.EMPTY_ARROW, 26, 10);
        widgets.addSlot(outputs.get(0), 60, 9).recipeContext(this);

        int seconds = time / 20;
        widgets.addText(Component.literal(seconds + " s"), 30, 0, 0xFF808080, false);
    }
}
