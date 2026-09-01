package net.sorukoma.quasium.integration.jei;

import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.block.ModBlocks;
import net.sorukoma.quasium.recipe.FermentationRecipes;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.recipe.RecipeIngredientRole;
import org.jetbrains.annotations.Nullable;

public class FermentationCategory implements IRecipeCategory<FermentationRecipes.FermentationRecipe> {
    public static final RecipeType<FermentationRecipes.FermentationRecipe> RECIPE_TYPE =
            RecipeType.create(QuasiumMod.MOD_ID, "fermentation", FermentationRecipes.FermentationRecipe.class);
    private static final int WIDTH = 90;
    private static final int HEIGHT = 36;
    private final Component title;
    private final IDrawable icon;
    private final IDrawable background;
    private final IDrawable arrow;

    public FermentationCategory(IGuiHelper guiHelper) {
        this.title = Component.translatable("quasium.jei.category.fermentation");
        this.icon = guiHelper.createDrawableItemLike(ModBlocks.FERMENTATION_BARREL_ITEM.get());
        this.background = guiHelper.createBlankDrawable(WIDTH, HEIGHT);
        this.arrow = guiHelper.getRecipeArrow();
    }

    @Override
    public RecipeType<FermentationRecipes.FermentationRecipe> getRecipeType() {
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
    @Nullable
    public IDrawable getIcon() {
        return icon;
    }

    @SuppressWarnings("removal")
    @Override
    @Nullable
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, FermentationRecipes.FermentationRecipe recipe, mezz.jei.api.recipe.IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 0, 9)
                .addItemStack(recipe.input());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 60, 9)
                .addItemStack(recipe.output());
    }

    @Override
    public void draw(FermentationRecipes.FermentationRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        int seconds = recipe.time() / 20;
        String timeText = seconds + " s";
        guiGraphics.drawString(Minecraft.getInstance().font, timeText, 30, 22, 0xFF808080, false);
    }
}
