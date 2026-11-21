package net.sorukoma.quasium.selection.world;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.sorukoma.quasium.block.ModBlocks;
import net.sorukoma.quasium.component.ModDataComponents;
import net.sorukoma.quasium.item.ModItems;
import net.sorukoma.quasium.progression.ConfigArgs;
import net.sorukoma.quasium.selection.SelectionRegistry;
import net.sorukoma.quasium.selection.genetics.Genetics;
import net.sorukoma.quasium.selection.trade.TobaccoShelf;
import net.sorukoma.quasium.smokelands.datagen.SmokelandsChestLoot;
import net.sorukoma.quasium.smokelands.item.LorePageItem;
import net.sorukoma.quasium.smokelands.item.SmokelandsItems;

import java.util.List;
import java.util.stream.Stream;

public final class LootOnlyItems {
    public record Entry(String id, List<ItemStack> stacks, Component text) {
    }

    private LootOnlyItems() {
    }

    public static ItemStack ancientMix() {
        ItemStack mix = new ItemStack(ModItems.SELF_MIXED_TOBACCO.get());
        mix.set(ModDataComponents.TOBACCO_BASE.get(), SmokelandsChestLoot.ANCIENT_BASE);
        mix.set(ModDataComponents.TOBACCO_DAMAGE.get(), 1.0F);
        mix.set(ModDataComponents.TOBACCO_WEIGHT.get(), 30);
        mix.set(ModDataComponents.TOBACCO_STRENGTH_STARS.get(), 5);
        mix.set(ModDataComponents.TOBACCO_YIELD_STARS.get(), 5);
        mix.set(ModDataComponents.TOBACCO_QUALITY_STARS.get(), 4);
        mix.set(SelectionRegistry.LEAF_QUALITY.get(), Genetics.MAX_STARS);
        mix.set(SelectionRegistry.KAIF_PCT.get(), 200);
        mix.set(DataComponents.ITEM_NAME, Component.translatable("item.quasium.ancient_mix"));
        return mix;
    }

    public static List<Entry> entries() {
        return List.of(
                entry("rare_bulbs", stacks(ModBlocks.MODULAR_HOOKAH_JADE_ITEM.get(), ModBlocks.MODULAR_HOOKAH_VIOLET_ITEM.get(),
                        ModBlocks.MODULAR_HOOKAH_GOLD_ITEM.get())),
                entry("secret_recipe", stacks(SelectionRegistry.SECRET_RECIPE.get())),
                entry("lore_page", stacks(SmokelandsItems.LORE_PAGE.get()), LorePageItem.PAGES),
                entry("ancient_mix", List.of(ancientMix())),
                entry("quasium_crust", stacks(SelectionRegistry.QUASIUM_CRUST.get()), ConfigArgs.format("GEODE_REVEAL_KAIF")),
                entry("budding_quasium", stacks(SelectionRegistry.BUDDING_QUASIUM.get())),
                entry("quasium_buds", stacks(SelectionRegistry.SMALL_QUASIUM_BUD.get(), SelectionRegistry.MEDIUM_QUASIUM_BUD.get(),
                        SelectionRegistry.LARGE_QUASIUM_BUD.get())),
                entry("quasium_cluster", stacks(SelectionRegistry.QUASIUM_CLUSTER.get()), ConfigArgs.format("CLUSTER_DROP_MIN_KAIF")),
                entry("flavored_tobacco", stacks(TobaccoShelf.all().toArray(ItemLike[]::new)), ConfigArgs.format("TOBACCO_COMMON_PRICE"),
                        ConfigArgs.format("TOBACCO_RARE_PRICE")));
    }

    private static Entry entry(String id, List<ItemStack> stacks, Object... args) {
        return new Entry(id, stacks, Component.translatable("quasium.loot_info." + id, args));
    }

    private static List<ItemStack> stacks(ItemLike... items) {
        return Stream.of(items).map(ItemStack::new).toList();
    }
}
