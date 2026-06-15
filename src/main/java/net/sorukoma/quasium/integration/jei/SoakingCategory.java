package net.sorukoma.quasium.integration.jei;

import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.component.ModDataComponents;
import net.sorukoma.quasium.item.ModItems;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.recipe.RecipeIngredientRole;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SoakingCategory implements IRecipeCategory<SoakingCategory.SoakingRecipe> {
    public static final RecipeType<SoakingRecipe> RECIPE_TYPE =
            RecipeType.create(QuasiumMod.MOD_ID, "soaking", SoakingRecipe.class);
    private static final int WIDTH = 130;
    private static final int HEIGHT = 60;
    private static final ResourceKey<net.minecraft.world.item.alchemy.Potion> WATER_KEY =
            ResourceKey.create(Registries.POTION, ResourceLocation.withDefaultNamespace("water"));
    private static final ResourceKey<net.minecraft.world.item.alchemy.Potion> EMPTY_KEY =
            ResourceKey.create(Registries.POTION, ResourceLocation.withDefaultNamespace("empty"));
    private final Component title;
    private final IDrawable icon;
    private final IDrawable background;

    public record SoakingRecipe(ItemStack fermentedLeaves, List<ItemStack> potions, @Nullable ItemStack molasses, ItemStack output) {}

    public SoakingCategory(IGuiHelper guiHelper) {
        this.title = Component.translatable("quasium.jei.category.soaking");
        this.icon = guiHelper.createDrawableItemLike(ModItems.MIXING_BOWL.get());
        this.background = guiHelper.createBlankDrawable(WIDTH, HEIGHT);
    }

    private static List<ItemStack> getAllVanillaPotions() {
        List<ItemStack> potions = new ArrayList<>();
        // Добавляем Water Bottle первым
        BuiltInRegistries.POTION.holders()
                .filter(holder -> holder.is(WATER_KEY))
                .findFirst()
                .ifPresent(holder -> potions.add(PotionContents.createItemStack(Items.POTION, holder)));
        // Добавляем все остальные зелья (исключаем water и empty)
        BuiltInRegistries.POTION.holders()
                .filter(holder -> holder.getRegisteredName().startsWith("minecraft:"))
                .filter(holder -> !holder.is(WATER_KEY) && !holder.is(EMPTY_KEY))
                .forEach(holder -> potions.add(PotionContents.createItemStack(Items.POTION, holder)));
        return potions;
    }

    private static ItemStack createOutput(String base, float damage, int molassesAmount) {
        ItemStack output = new ItemStack(ModItems.SELF_MIXED_TOBACCO.get());
        output.set(ModDataComponents.TOBACCO_BASE.get(), base);
        output.set(ModDataComponents.TOBACCO_DAMAGE.get(), damage);
        output.set(ModDataComponents.TOBACCO_WEIGHT.get(), 30);
        output.set(ModDataComponents.MOLASSES_AMOUNT.get(), molassesAmount);
        return output;
    }

    private static ItemStack createFermentedLeaves(boolean makhorka) {
        ItemStack leaves = new ItemStack(makhorka ? ModItems.MAKHORKA_LEAVES.get() : ModItems.TOBACCO_LEAVES.get());
        leaves.set(ModDataComponents.FERMENTED.get(), true);
        return leaves;
    }

    public static List<SoakingRecipe> createRecipes() {
        List<SoakingRecipe> recipes = new ArrayList<>();
        List<ItemStack> allPotions = getAllVanillaPotions();

        // Tobacco without molasses (damage 0.2)
        recipes.add(new SoakingRecipe(
                createFermentedLeaves(false), allPotions, null,
                createOutput("tobacco", 0.2f, 0)));
        // Tobacco with molasses (damage 0.0)
        recipes.add(new SoakingRecipe(
                createFermentedLeaves(false), allPotions, new ItemStack(ModItems.MOLASSES_BOTTLE.get()),
                createOutput("tobacco", 0.0f, 5)));

        // Makhorka without molasses (damage 1.0)
        recipes.add(new SoakingRecipe(
                createFermentedLeaves(true), allPotions, null,
                createOutput("makhorka", 1.0f, 0)));
        // Makhorka with molasses (damage 0.0)
        recipes.add(new SoakingRecipe(
                createFermentedLeaves(true), allPotions, new ItemStack(ModItems.MOLASSES_BOTTLE.get()),
                createOutput("makhorka", 0.0f, 5)));

        return recipes;
    }

    @Override
    public RecipeType<SoakingRecipe> getRecipeType() {
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
    public void setRecipe(IRecipeLayoutBuilder builder, SoakingRecipe recipe, mezz.jei.api.recipe.IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 0, 9)
                .addItemStack(recipe.fermentedLeaves());
        builder.addSlot(RecipeIngredientRole.INPUT, 0, 31)
                .addItemStacks(recipe.potions());
        if (recipe.molasses() != null) {
            builder.addSlot(RecipeIngredientRole.INPUT, 22, 31)
                    .addItemStack(recipe.molasses());
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, 100, 9)
                .addItemStack(recipe.output());
    }

    @Override
    public void draw(SoakingRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        int seconds = 10;
        String timeText = seconds + "s";
        guiGraphics.drawString(Minecraft.getInstance().font, timeText, 70, 22, 0xFF808080, false);
    }
}