package net.sorukoma.quasium.selection.world;

import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.SetComponentsFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.block.ModBlocks;
import net.sorukoma.quasium.component.ModDataComponents;
import net.sorukoma.quasium.item.ModItems;
import net.sorukoma.quasium.magic.NoteType;
import net.sorukoma.quasium.selection.SelectionRegistry;
import net.sorukoma.quasium.selection.geode.QuasiumGeodeFeature;
import net.sorukoma.quasium.smokelands.item.SmokelandsItems;

import java.util.Comparator;
import java.util.function.BiConsumer;

public class WorldLootProvider implements LootTableSubProvider {
    public static final ResourceKey<LootTable> ABANDONED_LOUNGE =
            ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "chests/abandoned_lounge"));
    public static final ResourceKey<LootTable> RUINED_SMOKE_PORTAL =
            ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "chests/ruined_smoke_portal"));
    public static final int NOTE_BOTTLE_ML = 20;
    public static final float GEODE_RECIPE_CHANCE = 0.25F;

    @Override
    public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
        output.accept(QuasiumGeodeFeature.CHEST_LOOT, LootTable.lootTable().withPool(noteBottles()).withPool(lorePage(2))
                .withPool(LootPool.lootPool().when(LootItemRandomChanceCondition.randomChance(GEODE_RECIPE_CHANCE))
                        .add(LootItem.lootTableItem(SelectionRegistry.SECRET_RECIPE.get()))));
        output.accept(ABANDONED_LOUNGE, LootTable.lootTable()
                .withPool(lorePage(1))
                .withPool(lorePage(5))
                .withPool(LootPool.lootPool().add(ancientMix()))
                .withPool(rareBulbs()
                        .add(LootItem.lootTableItem(SelectionRegistry.SECRET_RECIPE.get()).setWeight(2))
                        .add(EmptyLootItem.emptyItem().setWeight(3)))
                .withPool(LootPool.lootPool().setRolls(UniformGenerator.between(1, 3))
                        .add(LootItem.lootTableItem(Items.STRING).setWeight(3)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3))))
                        .add(LootItem.lootTableItem(Items.BONE).setWeight(2)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 2))))));
        output.accept(RUINED_SMOKE_PORTAL, LootTable.lootTable()
                .withPool(noteBottles(NoteType.ASH, NoteType.EMBER))
                .withPool(LootPool.lootPool().add(LootItem.lootTableItem(Items.CHARCOAL)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 5))))));
    }

    public static LootPool.Builder noteBottles() {
        return noteBottles(NoteType.values());
    }

    public static LootPool.Builder noteBottles(NoteType... notes) {
        LootPool.Builder bottles = LootPool.lootPool().setRolls(UniformGenerator.between(1, 3));
        for (NoteType note : notes) {
            bottles.add(LootItem.lootTableItem(ModItems.ESSENCE_BOTTLE.get())
                    .apply(SetComponentsFunction.setComponent(ModDataComponents.ESSENCE_TYPE.get(), note.id()))
                    .apply(SetComponentsFunction.setComponent(ModDataComponents.ESSENCE_AMOUNT.get(), NOTE_BOTTLE_ML)));
        }
        return bottles;
    }

    public static LootPool.Builder rareBulbs() {
        return LootPool.lootPool()
                .add(LootItem.lootTableItem(ModBlocks.MODULAR_HOOKAH_JADE_ITEM.get()))
                .add(LootItem.lootTableItem(ModBlocks.MODULAR_HOOKAH_VIOLET_ITEM.get()))
                .add(LootItem.lootTableItem(ModBlocks.MODULAR_HOOKAH_GOLD_ITEM.get()));
    }

    private static LootPool.Builder lorePage(int page) {
        return LootPool.lootPool().add(LootItem.lootTableItem(SmokelandsItems.LORE_PAGE.get())
                .apply(SetComponentsFunction.setComponent(SmokelandsItems.LORE_PAGE_NUMBER.get(), page)));
    }

    public static LootPoolSingletonContainer.Builder<?> ancientMix() {
        LootPoolSingletonContainer.Builder<?> mix = LootItem.lootTableItem(ModItems.SELF_MIXED_TOBACCO.get());
        LootOnlyItems.ancientMix().getComponentsPatch().split().added().stream()
                .sorted(Comparator.comparing(component -> BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(component.type()).toString()))
                .forEach(component -> mix.apply(setComponent(component)));
        return mix;
    }

    private static <T> LootItemConditionalFunction.Builder<?> setComponent(TypedDataComponent<T> component) {
        return SetComponentsFunction.setComponent(component.type(), component.value());
    }
}
