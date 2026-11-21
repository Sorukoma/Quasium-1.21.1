package net.sorukoma.quasium.selection.variety;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.sorukoma.quasium.item.ModItems;
import net.sorukoma.quasium.item.TobaccoVarieties;
import net.sorukoma.quasium.magic.page.spirit.SpiritMix;
import net.sorukoma.quasium.mind.research.Research;
import net.sorukoma.quasium.selection.genetics.Genetics;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.IntFunction;
import java.util.function.Supplier;

public final class SeedSoaking {
    public record Recipe(Item catalyst, @Nullable String variety, IntFunction<ItemStack> result, @Nullable ResourceLocation research) {
        public boolean takes(ItemStack stack, String leafVariety) {
            return stack.is(catalyst) && (variety == null || variety.equals(leafVariety));
        }

        public ItemStack shown() {
            return result.apply(Genetics.MIN_STARS);
        }

        public boolean knownBy(Player player) {
            return research == null || Research.studied(player, research);
        }
    }

    public static final List<Recipe> RECIPES = List.of(
            seeds(Items.ECHO_SHARD, null, ModItems.ECHO_SEEDS),
            seeds(Items.BLAZE_POWDER, TobaccoVarieties.PESCHANIK, ModItems.ASHEN_SEEDS),
            seeds(Items.CHORUS_FRUIT, TobaccoVarieties.DIKAR, ModItems.CHORUS_SEEDS),
            new Recipe(Items.PHANTOM_MEMBRANE, TobaccoVarieties.ASHEN, SpiritMix::pack, SpiritMix.RESEARCH));

    private SeedSoaking() {
    }

    private static Recipe seeds(Item catalyst, @Nullable String variety, Supplier<Item> seeds) {
        return new Recipe(catalyst, variety, quality -> new ItemStack(seeds.get()), null);
    }

    public static boolean isCatalyst(ItemStack stack) {
        return RECIPES.stream().anyMatch(recipe -> stack.is(recipe.catalyst()));
    }

    public static boolean knows(Player player, ItemStack catalyst) {
        return RECIPES.stream().filter(recipe -> catalyst.is(recipe.catalyst())).allMatch(recipe -> recipe.knownBy(player));
    }

    public static ItemStack sprout(ItemStack catalyst, String leafVariety, int leafQuality) {
        return RECIPES.stream().filter(recipe -> recipe.takes(catalyst, leafVariety)).findFirst()
                .map(recipe -> recipe.result().apply(leafQuality)).orElse(ItemStack.EMPTY);
    }
}
