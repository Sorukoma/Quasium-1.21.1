package net.sorukoma.quasium.integration.emi;

import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.magic.NoteType;
import net.sorukoma.quasium.recipe.MixingRecipe;

import java.util.List;

public class MixingEmiRecipe extends BasicEmiRecipe {
    private static final int WIDTH = 118;
    private static final int HEIGHT = 34;

    private final MixingRecipe recipe;

    public MixingEmiRecipe(ResourceLocation id, MixingRecipe recipe) {
        super(MixingEmiPlugin.CATEGORY,
                ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "/mixing/" + id.getNamespace() + "/" + id.getPath()), WIDTH, HEIGHT);
        this.recipe = recipe;
        this.inputs = List.of(EmiStack.of(recipe.bottle()), EmiStack.of(recipe.catalyst()));
        this.outputs = List.of(EmiStack.of(recipe.result()));
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        widgets.addSlot(inputs.get(0), 0, 4);
        widgets.addTexture(EmiTexture.PLUS, 22, 7);
        widgets.addSlot(inputs.get(1), 40, 4);
        widgets.addTexture(EmiTexture.EMPTY_ARROW, 63, 5);
        widgets.addSlot(outputs.get(0), 91, 0).large(true).recipeContext(this);
        NoteType note = NoteType.byId(recipe.essence());
        int color = note == null ? 0xFFA0A0A0 : 0xFF000000 | note.color();
        widgets.addText(Component.translatable("gui.quasium.ml", recipe.ml()), 1, 25, color, true);
    }
}
