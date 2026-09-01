package net.sorukoma.quasium.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.sorukoma.quasium.component.ModDataComponents;
import net.sorukoma.quasium.item.ModItems;

import java.util.List;

public final class EssenceRecipes {
    private EssenceRecipes() {}

    public record EssenceRecipe(List<ItemStack> ingredients, ItemStack bottle, ItemStack output) {}

    public static List<EssenceRecipe> createRecipes() {
        return List.of(
                create("mint", Items.FERN, ModItems.MINT_LEAVES.get()),
                create("berry", Items.SWEET_BERRIES),
                create("lemon", ModItems.LEMON.get()),
                create("lime", ModItems.LIME.get()),
                create("orange", ModItems.ORANGE.get()),
                create("grapefruit", ModItems.GRAPEFRUIT.get()),
                create("grape", ModItems.GRAPE.get()),
                create("lavender", ModItems.LAVENDER.get()),
                create("cocoa", ModItems.COCOA_NIBS.get()),
                create("apple", Items.APPLE),
                create("watermelon", Items.MELON_SLICE),
                create("cherry", Items.GLOW_BERRIES),
                create("vanilla", Items.SUGAR),
                create("coffee", Items.COCOA_BEANS),
                create("cinnamon", Items.BLAZE_POWDER),
                create("coconut", ModItems.COCONUT.get()),
                create("rose", Items.PINK_PETALS),
                create("anise", ModItems.ANISE.get()),
                create("ice", Items.ICE),
                create("honey", Items.HONEYCOMB),
                create("nether", Items.NETHER_WART)
        );
    }

    private static EssenceRecipe create(String type, ItemLike... ingredients) {
        ItemStack ingredient = new ItemStack(ingredients[0], 4);
        ItemStack output = new ItemStack(ModItems.ESSENCE_BOTTLE.get());
        output.set(ModDataComponents.ESSENCE_TYPE.get(), type);
        output.set(ModDataComponents.ESSENCE_AMOUNT.get(), 20);
        return new EssenceRecipe(List.of(ingredient), new ItemStack(Items.GLASS_BOTTLE), output);
    }

    public static List<ItemStack> ingredientAlternatives(EssenceRecipe recipe) {
        return switch (recipe.output().getOrDefault(ModDataComponents.ESSENCE_TYPE.get(), "")) {
            case "mint" -> List.of(new ItemStack(Items.FERN, 4), new ItemStack(ModItems.MINT_LEAVES.get(), 4));
            default -> recipe.ingredients();
        };
    }
}
