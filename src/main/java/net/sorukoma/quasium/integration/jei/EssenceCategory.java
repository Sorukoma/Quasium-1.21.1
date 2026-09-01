package net.sorukoma.quasium.integration.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.block.ModBlocks;
import net.sorukoma.quasium.recipe.EssenceRecipes;
import org.jetbrains.annotations.Nullable;

public class EssenceCategory implements IRecipeCategory<EssenceRecipes.EssenceRecipe> {
    public static final RecipeType<EssenceRecipes.EssenceRecipe> RECIPE_TYPE =
            RecipeType.create(QuasiumMod.MOD_ID, "essence", EssenceRecipes.EssenceRecipe.class);
    private static final int WIDTH = 120;
    private static final int HEIGHT = 54;
    private final Component title;
    private final IDrawable icon;
    private final IDrawable background;
    private final IDrawable arrow;

    public EssenceCategory(IGuiHelper guiHelper) {
        title = Component.translatable("quasium.jei.category.essence");
        icon = guiHelper.createDrawableItemLike(ModBlocks.ESSENCE_PRESS_ITEM.get());
        background = guiHelper.createBlankDrawable(WIDTH, HEIGHT);
        arrow = guiHelper.getRecipeArrow();
    }

    @Override
    public RecipeType<EssenceRecipes.EssenceRecipe> getRecipeType() {
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

    @Override
    @Nullable
    @SuppressWarnings("removal")
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, EssenceRecipes.EssenceRecipe recipe,
                          mezz.jei.api.recipe.IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 2, 18)
                .addItemStacks(EssenceRecipes.ingredientAlternatives(recipe));
        builder.addSlot(RecipeIngredientRole.INPUT, 24, 18)
                .addItemStack(recipe.bottle());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 92, 18)
                .addItemStack(recipe.output());
    }

    @Override
    public void draw(EssenceRecipes.EssenceRecipe recipe, IRecipeSlotsView recipeSlotsView,
                     GuiGraphics guiGraphics, double mouseX, double mouseY) {
        arrow.draw(guiGraphics, 58, 20);
        guiGraphics.drawString(Minecraft.getInstance().font, "×4", 7, 38, 0xFF808080, false);
    }
}
