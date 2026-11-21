package net.sorukoma.quasium.smokelands.datagen;

import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.sorukoma.quasium.smokelands.SmokelandsDimension;
import net.sorukoma.quasium.smokelands.SmokelandsRegistry;
import net.sorukoma.quasium.smokelands.block.NoteClotBlock;
import net.sorukoma.quasium.smokelands.block.PrimordialTobaccoBlock;
import net.sorukoma.quasium.smokelands.block.SmokelandsBlocks;
import net.sorukoma.quasium.smokelands.item.SmokelandsItems;
import net.sorukoma.quasium.smokelands.sultan.SultanRewards;
import net.sorukoma.quasium.smokelands.world.OptionalItemEntry;

import java.util.List;
import java.util.Set;

public class SmokelandsBlockLoot extends BlockLootSubProvider {
    private static final float FRUIT_CHANCE = 0.05f;
    private static final float ETERNAL_COAL_CHANCE = 0.01f;

    public SmokelandsBlockLoot(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        List.of(SmokelandsBlocks.ASH_SAND, SmokelandsBlocks.ASH_STONE, SmokelandsBlocks.ASH_BRICKS, SmokelandsBlocks.CRACKED_ASH_BRICKS,
                SmokelandsBlocks.PETRIFIED_SMOKE, SmokelandsBlocks.ASH_CACTUS, SmokelandsBlocks.SOOT_MUD,
                SmokelandsBlocks.NICOTINE_MOSS, SmokelandsBlocks.HOOKAH_LOG, SmokelandsBlocks.ANCIENT_FLASK).forEach(block -> dropSelf(block.get()));
        SmokelandsBlocks.MOSAICS.values().forEach(block -> dropSelf(block.get()));
        add(SmokelandsBlocks.SMOLDER.get(), createSingleItemTable(SmokelandsBlocks.SMOLDER.get())
                .withPool(LootPool.lootPool().add(OptionalItemEntry.of(SultanRewards.ETERNAL_COAL)
                        .when(LootItemRandomChanceCondition.randomChance(ETERNAL_COAL_CHANCE)))));
        SmokelandsBlocks.CLOTS.values().forEach(block -> add(block.get(), whole(block.get())));
        add(SmokelandsBlocks.NICOTINE_LEAVES.get(), leaves(SmokelandsBlocks.NICOTINE_LEAVES.get()));
        add(SmokelandsBlocks.HOSE_VINE.get(), createShearsOnlyDrop(SmokelandsBlocks.HOSE_VINE.get()));
        add(SmokelandsBlocks.BUDDING_DREAM_QUARTZ.get(), noDrop());
        add(SmokelandsBlocks.DREAM_QUARTZ_BUD.get(), createSilkTouchOnlyTable(SmokelandsBlocks.DREAM_QUARTZ_BUD.get()));
        add(SmokelandsBlocks.DREAM_QUARTZ_CLUSTER.get(), createSilkTouchDispatchTable(SmokelandsBlocks.DREAM_QUARTZ_CLUSTER.get(),
                applyExplosionDecay(SmokelandsBlocks.DREAM_QUARTZ_CLUSTER.get(), OptionalItemEntry.of(SmokelandsDimension.id("dream_quartz"))
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1f, 2f))))));
        add(SmokelandsBlocks.PRIMORDIAL_TOBACCO.get(), LootTable.lootTable().withPool(LootPool.lootPool()
                .when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(SmokelandsBlocks.PRIMORDIAL_TOBACCO.get())
                        .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(PrimordialTobaccoBlock.AGE, PrimordialTobaccoBlock.RIPE)))
                .add(OptionalItemEntry.of(PrimordialTobaccoBlock.SEEDS))));
    }

    private LootTable.Builder whole(Block clot) {
        return LootTable.lootTable().withPool(applyExplosionCondition(clot, LootPool.lootPool()
                .when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(clot)
                        .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(NoteClotBlock.CHARGES, NoteClotBlock.FULL)))
                .add(LootItem.lootTableItem(clot))));
    }

    private LootTable.Builder leaves(Block block) {
        return createSilkTouchOrShearsDispatchTable(block, applyExplosionDecay(block, LootItem.lootTableItem(Items.STICK)
                .apply(SetItemCountFunction.setCount(UniformGenerator.between(0f, 2f)))))
                .withPool(LootPool.lootPool().when(HAS_SHEARS.or(hasSilkTouch()).invert())
                        .add(LootItem.lootTableItem(SmokelandsItems.NICOTINE_FRUIT.get())
                                .when(LootItemRandomChanceCondition.randomChance(FRUIT_CHANCE))));
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return SmokelandsRegistry.BLOCKS.getEntries().stream().<Block>map(DeferredHolder::get).toList();
    }
}
