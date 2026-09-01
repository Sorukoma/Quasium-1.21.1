package net.sorukoma.quasium.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.sorukoma.quasium.block.ModBlocks;

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
            ModBlocks.HEATER);

    public ModBlockLootProvider(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        SELF_DROPPING.forEach(block -> dropSelf(block.get()));
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return SELF_DROPPING.stream().map(DeferredBlock<Block>::get).toList();
    }
}
