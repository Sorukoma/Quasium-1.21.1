package net.sorukoma.quasium.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.sorukoma.quasium.golem.MarkBlocks;
import net.sorukoma.quasium.golem.program.ProgramBlocks;

import java.util.Set;
import java.util.stream.Stream;

public class MarkLootProvider extends BlockLootSubProvider {
    public MarkLootProvider(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        getKnownBlocks().forEach(this::dropSelf);
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return Stream.concat(MarkBlocks.BLOCKS.getEntries().stream(), ProgramBlocks.BLOCKS.getEntries().stream())
                .map(DeferredHolder::get).map(Block.class::cast).toList();
    }
}
