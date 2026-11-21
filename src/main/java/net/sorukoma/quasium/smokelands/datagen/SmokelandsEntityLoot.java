package net.sorukoma.quasium.smokelands.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.EntityLootSubProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetComponentsFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemKilledByPlayerCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.sorukoma.quasium.component.ModDataComponents;
import net.sorukoma.quasium.item.ModItems;
import net.sorukoma.quasium.magic.NoteType;
import net.sorukoma.quasium.smokelands.SmokelandsDimension;
import net.sorukoma.quasium.smokelands.SmokelandsRegistry;
import net.sorukoma.quasium.smokelands.item.SmokelandsItems;
import net.sorukoma.quasium.smokelands.mob.SmokelandsEntities;
import net.sorukoma.quasium.smokelands.sultan.Sultans;
import net.sorukoma.quasium.smokelands.world.OptionalItemEntry;

import java.util.stream.Stream;

public class SmokelandsEntityLoot extends EntityLootSubProvider {
    private static final float DREAM_QUARTZ_CHANCE = 0.10f;
    private static final float ASH_OF_TIME_CHANCE = 0.05f;
    private static final int TAR_RESIN_ML = 2;

    public SmokelandsEntityLoot(HolderLookup.Provider registries) {
        super(FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    public void generate() {
        add(SmokelandsEntities.MIST_PHANTOM.get(), LootTable.lootTable()
                .withPool(LootPool.lootPool().when(LootItemKilledByPlayerCondition.killedByPlayer())
                        .add(OptionalItemEntry.of(SmokelandsDimension.id("dream_quartz")).when(LootItemRandomChanceCondition.randomChance(DREAM_QUARTZ_CHANCE))))
                .withPool(LootPool.lootPool().when(LootItemKilledByPlayerCondition.killedByPlayer())
                        .add(LootItem.lootTableItem(SmokelandsItems.ASH_OF_TIME.get()).when(LootItemRandomChanceCondition.randomChance(ASH_OF_TIME_CHANCE)))));
        add(SmokelandsEntities.TAR_SLIME.get(), LootTable.lootTable().withPool(LootPool.lootPool()
                .add(LootItem.lootTableItem(ModItems.ESSENCE_BOTTLE.get())
                        .apply(SetComponentsFunction.setComponent(ModDataComponents.ESSENCE_TYPE.get(), NoteType.RESIN.id()))
                        .apply(SetComponentsFunction.setComponent(ModDataComponents.ESSENCE_AMOUNT.get(), TAR_RESIN_ML)))));
        add(SmokelandsEntities.DRAFT_WISP.get(), LootTable.lootTable());
        add(Sultans.SULTAN.get(), LootTable.lootTable());
    }

    @Override
    protected Stream<EntityType<?>> getKnownEntityTypes() {
        return SmokelandsRegistry.ENTITY_TYPES.getEntries().stream().map(DeferredHolder::get);
    }
}
