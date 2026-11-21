package net.sorukoma.quasium.recipe;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.sorukoma.quasium.component.ModDataComponents;
import net.sorukoma.quasium.item.LeavesItem;
import net.sorukoma.quasium.item.ModItems;
import net.sorukoma.quasium.item.TobaccoVarieties;
import net.sorukoma.quasium.selection.variety.SeedSoaking;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared soaking recipe data used by both the JEI and EMI integrations.
 */
public class SoakingRecipes {

    /**
     * essences — список всех подходящих экстрактов (для JEI/EMI отображается циклически).
     * Пустой список означает, что экстракт не используется в этом рецепте.
     */
    public record SoakingRecipe(List<ItemStack> fermentedLeaves, List<ItemStack> essences,
                                @Nullable ItemStack molasses, ItemStack output) {}

    /** Все типы экстрактов, которые можно применить в миске (кроме "unknown"). */
    private static final String[] ESSENCE_TYPES = {
            "mint", "berry", "lemon", "lime", "orange", "grapefruit", "ice", "honey", "nether",
            "lavender", "cocoa", "apple", "watermelon", "grape", "cherry", "vanilla", "coffee",
            "cinnamon", "coconut", "rose", "anise"
    };

    private static List<ItemStack> createEssences() {
        return java.util.Arrays.stream(ESSENCE_TYPES).map(type -> {
            ItemStack essence = new ItemStack(ModItems.ESSENCE_BOTTLE.get());
            essence.set(ModDataComponents.ESSENCE_TYPE.get(), type);
            essence.set(ModDataComponents.ESSENCE_AMOUNT.get(), 20);
            return essence;
        }).toList();
    }

    private static ItemStack createOutput(String base, float damage, int molassesAmount) {
        ItemStack output = new ItemStack(ModItems.SELF_MIXED_TOBACCO.get());
        output.set(ModDataComponents.TOBACCO_BASE.get(), base);
        output.set(ModDataComponents.TOBACCO_DAMAGE.get(), damage);
        output.set(ModDataComponents.TOBACCO_WEIGHT.get(), 30);
        output.set(ModDataComponents.MOLASSES_AMOUNT.get(), molassesAmount);
        return output;
    }

    private static List<ItemStack> createFermentedLeaves(@Nullable String variety) {
        return BuiltInRegistries.ITEM.stream().filter(item -> item instanceof LeavesItem).map(item -> {
            ItemStack leaves = new ItemStack(item);
            leaves.set(ModDataComponents.FERMENTED.get(), true);
            return leaves;
        }).filter(leaves -> variety == null || variety.equals(TobaccoVarieties.getVariety(leaves))).toList();
    }

    public static List<SoakingRecipe> createRecipes() {
        List<SoakingRecipe> recipes = new ArrayList<>(List.of(
                new SoakingRecipe(createFermentedLeaves(TobaccoVarieties.DIKAR), createEssences(), null,
                        createOutput(TobaccoVarieties.DIKAR, 0.2f, 0)),
                new SoakingRecipe(createFermentedLeaves(TobaccoVarieties.DIKAR), createEssences(),
                        new ItemStack(ModItems.MOLASSES_BOTTLE.get()), createOutput(TobaccoVarieties.DIKAR, 0.0f, 5)),
                new SoakingRecipe(createFermentedLeaves(TobaccoVarieties.PESCHANIK), createEssences(), null,
                        createOutput(TobaccoVarieties.PESCHANIK, 1.0f, 0)),
                new SoakingRecipe(createFermentedLeaves(TobaccoVarieties.PESCHANIK), createEssences(),
                        new ItemStack(ModItems.MOLASSES_BOTTLE.get()), createOutput(TobaccoVarieties.PESCHANIK, 0.0f, 5))
        ));
        for (SeedSoaking.Recipe seed : SeedSoaking.RECIPES) {
            recipes.add(new SoakingRecipe(createFermentedLeaves(seed.variety()), List.of(new ItemStack(seed.catalyst())), null,
                    seed.shown()));
        }
        return recipes;
    }
}
