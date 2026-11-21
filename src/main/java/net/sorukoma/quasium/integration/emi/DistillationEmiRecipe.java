package net.sorukoma.quasium.integration.emi;

import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.magic.NoteProfile;
import net.sorukoma.quasium.magic.NoteType;

import java.util.List;
import java.util.Map;

public class DistillationEmiRecipe extends BasicEmiRecipe {
    private static final int WIDTH = 150;
    private static final int HEIGHT = 44;
    private static final int OUTPUT_X = 52;
    private static final int OUTPUT_STEP = 24;

    private final NoteProfile profile;

    public DistillationEmiRecipe(ResourceLocation id, NoteProfile profile) {
        super(QuasiumEmiCategories.DISTILLATION,
                ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "/distillation/" + id.getPath()), WIDTH, HEIGHT);
        this.profile = profile;
        this.inputs = List.of(EmiIngredient.of(profile.sourceStacks().stream().map(EmiStack::of).toList()));
        this.outputs = profile.essenceStacks().stream().map(EmiStack::of).toList();
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        widgets.addSlot(inputs.get(0), 4, 10);
        widgets.addTexture(EmiTexture.EMPTY_ARROW, 24, 10);
        int i = 0;
        for (Map.Entry<NoteType, Integer> note : profile.notes().entrySet()) {
            widgets.addSlot(outputs.get(i), OUTPUT_X + i * OUTPUT_STEP, 10).recipeContext(this);
            widgets.addText(Component.translatable("gui.quasium.ml", note.getValue()), OUTPUT_X + i * OUTPUT_STEP, 30,
                    0xFF000000 | note.getKey().color(), true);
            i++;
        }
    }
}
