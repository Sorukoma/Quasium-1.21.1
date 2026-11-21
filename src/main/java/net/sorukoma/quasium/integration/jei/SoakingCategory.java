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
import net.sorukoma.quasium.recipe.SoakingRecipes;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.recipe.RecipeIngredientRole;
import org.jetbrains.annotations.Nullable;

public class SoakingCategory implements IRecipeCategory<SoakingRecipes.SoakingRecipe> {
    public static final RecipeType<SoakingRecipes.SoakingRecipe> RECIPE_TYPE =
            RecipeType.create(QuasiumMod.MOD_ID, "soaking", SoakingRecipes.SoakingRecipe.class);
    private static final int WIDTH = 130;
    private static final int HEIGHT = 60;
    private final Component title;
    private final IDrawable icon;
    private final IDrawable background;

    public SoakingCategory(IGuiHelper guiHelper) {
        this.title = Component.translatable("quasium.jei.category.soaking");
        this.icon = guiHelper.createDrawableItemLike(ModBlocks.SOAKING_BOWL_ITEM.get());
        this.background = guiHelper.createBlankDrawable(WIDTH, HEIGHT);
    }

    @Override
    public RecipeType<SoakingRecipes.SoakingRecipe> getRecipeType() {
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
    public void setRecipe(IRecipeLayoutBuilder builder, SoakingRecipes.SoakingRecipe recipe, mezz.jei.api.recipe.IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 0, 9)
                .addItemStacks(recipe.fermentedLeaves());
        if (recipe.essences() != null && !recipe.essences().isEmpty()) {
            builder.addSlot(RecipeIngredientRole.INPUT, 0, 31)
                    .addItemStacks(recipe.essences());
        }
        if (recipe.molasses() != null) {
            builder.addSlot(RecipeIngredientRole.INPUT, 22, 31)
                    .addItemStack(recipe.molasses());
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, 100, 9)
                .addItemStack(recipe.output());
    }

    @Override
    public void draw(SoakingRecipes.SoakingRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        int seconds = 10;
        String timeText = seconds + "s";
        guiGraphics.drawString(Minecraft.getInstance().font, timeText, 70, 22, 0xFF808080, false);
    }
}
