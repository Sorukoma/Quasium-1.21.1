package net.sorukoma.quasium.smokelands.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.functions.ExplorationMapFunction;
import net.minecraft.world.level.storage.loot.functions.SetComponentsFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.functions.SetNameFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.sorukoma.quasium.magic.page.PageRegistry;
import net.sorukoma.quasium.magic.page.form.FirstPages;
import net.sorukoma.quasium.selection.world.WorldLootProvider;
import net.sorukoma.quasium.smokelands.block.PrimordialTobaccoBlock;
import net.sorukoma.quasium.smokelands.block.SmokelandsBlocks;
import net.sorukoma.quasium.smokelands.item.SmokelandsItems;
import net.sorukoma.quasium.smokelands.item.UnreadPageFunction;
import net.sorukoma.quasium.smokelands.sultan.SultanRewards;
import net.sorukoma.quasium.smokelands.world.OptionalItemEntry;
import net.sorukoma.quasium.smokelands.world.SmokelandsWorldgen;

import java.util.function.BiConsumer;

public class SmokelandsChestLoot implements LootTableSubProvider {
    public static final String ANCIENT_BASE = "ancient";

    public SmokelandsChestLoot(HolderLookup.Provider registries) {
    }

    @Override
    public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
        output.accept(SmokelandsWorldgen.RUINS_CHEST, LootTable.lootTable()
                .withPool(chance(0.6f, LootItem.lootTableItem(SmokelandsItems.LORE_PAGE.get()).apply(UnreadPageFunction.unread())))
                .withPool(LootPool.lootPool().add(OptionalItemEntry.of(SultanRewards.DREAM_QUARTZ)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(2f, 8f)))))
                .withPool(chance(0.25f, OptionalItemEntry.of(PrimordialTobaccoBlock.SEEDS)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1f, 2f)))))
                .withPool(chance(0.15f, WorldLootProvider.ancientMix()))
                .withPool(chance(0.05f, LootItem.lootTableItem(SmokelandsItems.MUSIC_DISC_ASH_LOFI.get())))
                .withPool(chance(0.20f, LootItem.lootTableItem(SmokelandsBlocks.ANCIENT_FLASK.get())))
                .withPool(chance(0.25f, sultanMap()))
                .withPool(firstPages(0.2f))
                .withPool(LootPool.lootPool().setRolls(UniformGenerator.between(1f, 3f))
                        .add(LootItem.lootTableItem(SmokelandsBlocks.ASH_SAND.get()).apply(SetItemCountFunction.setCount(UniformGenerator.between(2f, 6f))))
                        .add(LootItem.lootTableItem(Items.BONE).apply(SetItemCountFunction.setCount(UniformGenerator.between(1f, 3f))))
                        .add(LootItem.lootTableItem(Items.CHARCOAL).apply(SetItemCountFunction.setCount(UniformGenerator.between(1f, 4f))))));
    }

    private static LootPool.Builder chance(float probability, LootPoolEntryContainer.Builder<?> entry) {
        return LootPool.lootPool().when(LootItemRandomChanceCondition.randomChance(probability)).add(entry);
    }

    private static LootPool.Builder firstPages(float probability) {
        LootPool.Builder pool = LootPool.lootPool().when(LootItemRandomChanceCondition.randomChance(probability));
        FirstPages.ALL.forEach(page -> pool.add(LootItem.lootTableItem(PageRegistry.TORN_PAGE.get())
                .apply(SetComponentsFunction.setComponent(PageRegistry.PAGE.get(), page))));
        return pool;
    }

    private static LootPoolEntryContainer.Builder<?> sultanMap() {
        return LootItem.lootTableItem(Items.MAP)
                .apply(ExplorationMapFunction.makeExplorationMap().setDestination(SmokelandsWorldgen.ON_SULTAN_MAPS)
                        .setMapDecoration(MapDecorationTypes.RED_X).setZoom((byte) 2).setSkipKnownStructures(false))
                .apply(SetNameFunction.setName(Component.translatable("item.quasium.sultan_map"), SetNameFunction.Target.ITEM_NAME));
    }
}
