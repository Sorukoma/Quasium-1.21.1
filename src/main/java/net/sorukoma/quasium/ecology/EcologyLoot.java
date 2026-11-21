package net.sorukoma.quasium.ecology;

import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.sorukoma.quasium.tea.SealBlocks;
import net.sorukoma.quasium.tea.TeaBlocks;
import net.sorukoma.quasium.tea.TeaBushBlock;

import java.util.Set;

public class EcologyLoot extends BlockLootSubProvider {
    public EcologyLoot(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        add(TeaBlocks.TEA_BUSH.get(), createCropDrops(TeaBlocks.TEA_BUSH.get(), TeaBlocks.TEA_LEAF.get(), TeaBlocks.TEA_SEEDS.get(),
                LootItemBlockStatePropertyCondition.hasBlockStateProperties(TeaBlocks.TEA_BUSH.get())
                        .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(TeaBushBlock.AGE, TeaBushBlock.MAX_AGE))));
        dropSelf(TeaBlocks.SAMOVAR.get());
        dropSelf(SealBlocks.SOBRIETY_SEAL.get());
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return EcologyRegistry.BLOCKS.getEntries().stream().<Block>map(DeferredHolder::value).toList();
    }
}
