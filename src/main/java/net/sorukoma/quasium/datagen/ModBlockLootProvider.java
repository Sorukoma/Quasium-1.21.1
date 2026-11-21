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
import net.neoforged.neoforge.registries.DeferredBlock;
import net.sorukoma.quasium.block.ModBlocks;
import net.sorukoma.quasium.component.ModDataComponents;

import java.util.List;
import java.util.Set;

/**
 * Generates the "break the block, get the block back" loot tables.
 *
 * Crops ({@code VarietyTobaccoCropBlock}, {@code EssenceCropBlock}) and the quasium ore override
 * {@code Block#getDrops} directly, so their loot tables are never consulted and are deliberately
 * absent here.
 */
public class ModBlockLootProvider extends BlockLootSubProvider {

    private static final List<DeferredBlock<Block>> SELF_DROPPING = List.of(
            ModBlocks.HOOKAH_BLOCK,
            ModBlocks.MODULAR_HOOKAH_WHITE,
            ModBlocks.MODULAR_HOOKAH_BLACK,
            ModBlocks.MODULAR_HOOKAH_RED,
            ModBlocks.MODULAR_HOOKAH_BLUE,
            ModBlocks.MODULAR_HOOKAH_AMBER,
            ModBlocks.MODULAR_HOOKAH_JADE,
            ModBlocks.MODULAR_HOOKAH_VIOLET,
            ModBlocks.MODULAR_HOOKAH_GOLD,
            ModBlocks.WOODEN_TOBACCO_POLE,
            ModBlocks.REINFORCED_TOBACCO_POLE,
            ModBlocks.GOLDEN_TOBACCO_POLE,
            ModBlocks.DIAMOND_TOBACCO_POLE,
            ModBlocks.BONSAI_POT,
            ModBlocks.DRYING_SHELF,
            ModBlocks.FERMENTATION_BARREL,
            ModBlocks.SOAKING_BOWL,
            ModBlocks.ESSENCE_PRESS,
            ModBlocks.UNDRIED_QUASIUM_BLOCK,
            ModBlocks.QUASIUM_BLOCK,
            ModBlocks.QUASIUM_ANVIL,
            ModBlocks.HEATER,
            ModBlocks.DISTILLATION_CUBE);

    public ModBlockLootProvider(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        SELF_DROPPING.forEach(block -> dropSelf(block.get()));
        add(ModBlocks.NOTE_JAR.get(), LootTable.lootTable().withPool(applyExplosionCondition(ModBlocks.NOTE_JAR.get(),
                LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                        .add(LootItem.lootTableItem(ModBlocks.NOTE_JAR.get())
                                .apply(CopyComponentsFunction.copyComponents(CopyComponentsFunction.Source.BLOCK_ENTITY)
                                        .include(ModDataComponents.ESSENCE_TYPE.get())
                                        .include(ModDataComponents.ESSENCE_AMOUNT.get()))))));
        dropOther(ModBlocks.SMOKE_JAR.get(), net.sorukoma.quasium.item.ModItems.EMPTY_SMOKE_JAR.get());
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return java.util.stream.Stream.concat(SELF_DROPPING.stream(), java.util.stream.Stream.of(ModBlocks.NOTE_JAR, ModBlocks.SMOKE_JAR))
                .map(DeferredBlock<Block>::get).toList();
    }
}
