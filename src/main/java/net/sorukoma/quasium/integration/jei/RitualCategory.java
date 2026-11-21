package net.sorukoma.quasium.integration.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.item.EssenceBottleItem;
import net.sorukoma.quasium.magic.NoteType;
import net.sorukoma.quasium.mind.client.ResearchLocks;
import net.sorukoma.quasium.ritual.RitualRecipe;
import net.sorukoma.quasium.ritual.RitualRegistry;
import net.sorukoma.quasium.ritual.RitualSummary;
import net.sorukoma.quasium.ritual.brazier.RitualRun;

import java.util.List;
import java.util.Map;

public class RitualCategory implements IRecipeCategory<RitualCategory.Display> {
    public record Display(ResourceLocation id, RitualRecipe recipe) {
    }

    public static final RecipeType<Display> RECIPE_TYPE = RecipeType.create(QuasiumMod.MOD_ID, "ritual", Display.class);
    private static final int WIDTH = 162;
    private static final int HEIGHT = 128 + ResearchLocks.RESERVED_HEIGHT;
    private static final int SLOT = 18;
    private static final int PEDESTAL_Y = 22;
    private static final int NOTE_Y = 44;
    private static final int TEXT_Y = 66;
    private static final int LINE = 10;
    private static final int TEXT_COLOR = 0xFF404040;

    private final Component title = Component.translatable("quasium.jei.category.ritual");
    private final IDrawable icon;
    private final IDrawable arrow;

    public RitualCategory(IGuiHelper guiHelper) {
        icon = guiHelper.createDrawableItemLike(RitualRegistry.RITUAL_BRAZIER_ITEM.get());
        arrow = guiHelper.getRecipeArrow();
    }

    @Override
    public RecipeType<Display> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return title;
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Display display, IFocusGroup focuses) {
        RitualRecipe recipe = display.recipe();
        builder.addSlot(RecipeIngredientRole.CATALYST, 1, 1).setStandardSlotBackground()
                .addItemStack(RitualRegistry.RITUAL_BRAZIER_ITEM.get().getDefaultInstance());
        List<ItemStack> catalysts = List.of(recipe.catalyst().getItems());
        IRecipeSlotBuilder catalyst = builder.addSlot(RecipeIngredientRole.INPUT, 41, 1).setStandardSlotBackground().addItemStacks(catalysts);
        IRecipeSlotBuilder result = builder.addSlot(RecipeIngredientRole.OUTPUT, 101, 1).setOutputSlotBackground()
                .addItemStacks(catalysts.stream().map(recipe::previewFor).toList());
        builder.createFocusLink(catalyst, result);
        List<Ingredient> pedestals = recipe.pedestals();
        for (int index = 0; index < pedestals.size(); index++) {
            builder.addSlot(RecipeIngredientRole.INPUT, 1 + index * SLOT, PEDESTAL_Y).setStandardSlotBackground().addIngredients(pedestals.get(index));
        }
        int column = 0;
        for (Map.Entry<NoteType, Integer> note : recipe.notes().entrySet()) {
            builder.addSlot(RecipeIngredientRole.INPUT, 1 + column++ * SLOT, NOTE_Y).setStandardSlotBackground()
                    .addItemStack(EssenceBottleItem.of(note.getKey().id(), note.getValue()));
        }
    }

    @Override
    public void draw(Display display, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        arrow.draw(graphics, 66, 2);
        Font font = Minecraft.getInstance().font;
        graphics.drawString(font, RitualRun.displayName(display.id()), 0, TEXT_Y, TEXT_COLOR, false);
        List<Component> lines = RitualSummary.lines(display.recipe());
        for (int line = 0; line < lines.size(); line++) {
            graphics.drawString(font, lines.get(line), 0, TEXT_Y + (line + 1) * LINE, TEXT_COLOR, false);
        }
        display.recipe().research().ifPresent(node -> ResearchLocks.draw(graphics, node, 0, TEXT_Y + (lines.size() + 1) * LINE + 2, WIDTH));
    }
}
