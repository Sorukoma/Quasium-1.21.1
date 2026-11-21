package net.sorukoma.quasium.integration.emi;

import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.sorukoma.quasium.item.EssenceBottleItem;
import net.sorukoma.quasium.ritual.RitualRecipe;
import net.sorukoma.quasium.ritual.RitualSummary;
import net.sorukoma.quasium.ritual.brazier.RitualRun;

import java.util.ArrayList;
import java.util.List;

public class RitualEmiRecipe extends BasicEmiRecipe {
    private static final int WIDTH = 162;
    private static final int HEIGHT = 128;
    private static final int SLOT = 18;
    private static final int PEDESTAL_Y = 22;
    private static final int NOTE_Y = 44;
    private static final int TEXT_Y = 66;
    private static final int LINE = 10;
    private static final int TEXT_COLOR = 0xFF404040;

    private final ResourceLocation ritual;
    private final RitualRecipe recipe;
    private final List<EmiIngredient> pedestals;
    private final List<EmiIngredient> notes;

    public RitualEmiRecipe(ResourceLocation ritual, RitualRecipe recipe) {
        super(RitualEmiPlugin.CATEGORY, ResourceLocation.fromNamespaceAndPath(ritual.getNamespace(), "/ritual/" + ritual.getPath()), WIDTH, HEIGHT);
        this.ritual = ritual;
        this.recipe = recipe;
        this.pedestals = recipe.pedestals().stream().map(EmiIngredient::of).toList();
        this.notes = recipe.notes().entrySet().stream()
                .<EmiIngredient>map(note -> EmiStack.of(EssenceBottleItem.of(note.getKey().id(), note.getValue()))).toList();
        List<EmiIngredient> all = new ArrayList<>();
        all.add(EmiIngredient.of(recipe.catalyst()));
        all.addAll(pedestals);
        all.addAll(notes);
        this.inputs = all;
        this.outputs = List.of(EmiStack.of(recipe.preview()));
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        widgets.addSlot(inputs.get(0), 40, 0);
        widgets.addTexture(EmiTexture.EMPTY_ARROW, 66, 1);
        widgets.addSlot(outputs.get(0), 96, 0).recipeContext(this);
        for (int index = 0; index < pedestals.size(); index++) widgets.addSlot(pedestals.get(index), index * SLOT, PEDESTAL_Y);
        for (int index = 0; index < notes.size(); index++) widgets.addSlot(notes.get(index), index * SLOT, NOTE_Y);
        widgets.addText(RitualRun.displayName(ritual), 0, TEXT_Y, TEXT_COLOR, false);
        List<Component> lines = RitualSummary.lines(recipe);
        for (int line = 0; line < lines.size(); line++) widgets.addText(lines.get(line), 0, TEXT_Y + (line + 1) * LINE, TEXT_COLOR, false);
    }
}
