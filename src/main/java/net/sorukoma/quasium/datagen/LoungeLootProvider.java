package net.sorukoma.quasium.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.sorukoma.quasium.block.AromaLampBlock;
import net.sorukoma.quasium.block.LoungeBlocks;
import net.sorukoma.quasium.component.ModDataComponents;

import java.util.Set;

public class LoungeLootProvider extends BlockLootSubProvider {
    public LoungeLootProvider(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        getKnownBlocks().forEach(block -> {
            if (block instanceof AromaLampBlock lamp) {
                add(lamp, keepingEssence(lamp));
            } else {
                dropSelf(block);
            }
        });
    }

    private LootTable.Builder keepingEssence(Block lamp) {
        return LootTable.lootTable().withPool(applyExplosionCondition(lamp, LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1))
                .add(LootItem.lootTableItem(lamp)
                        .apply(CopyComponentsFunction.copyComponents(CopyComponentsFunction.Source.BLOCK_ENTITY)
                                .include(ModDataComponents.ESSENCE_TYPE.get())
                                .include(ModDataComponents.ESSENCE_AMOUNT.get())))));
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return LoungeBlocks.BLOCKS.getEntries().stream().<Block>map(DeferredHolder::value).toList();
    }
}
