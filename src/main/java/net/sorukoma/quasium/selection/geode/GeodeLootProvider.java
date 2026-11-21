package net.sorukoma.quasium.selection.geode;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.sorukoma.quasium.selection.SelectionRegistry;

import java.util.List;
import java.util.Set;

public class GeodeLootProvider extends BlockLootSubProvider {
    public GeodeLootProvider(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        dropSelf(SelectionRegistry.QUASIUM_CRUST.get());
        for (Block bud : List.of(SelectionRegistry.SMALL_QUASIUM_BUD.get(), SelectionRegistry.MEDIUM_QUASIUM_BUD.get(),
                SelectionRegistry.LARGE_QUASIUM_BUD.get())) {
            add(bud, createSilkTouchOnlyTable(bud));
        }
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return SelectionRegistry.BLOCKS.getEntries().stream().<Block>map(DeferredHolder::value).toList();
    }
}
