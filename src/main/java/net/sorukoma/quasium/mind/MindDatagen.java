package net.sorukoma.quasium.mind;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.neoforge.common.data.AdvancementProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.sorukoma.quasium.datagen.SteppedRecipeProvider;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class MindDatagen {
    private MindDatagen() {
    }

    public static void gather(GatherDataEvent event, DataGenerator generator, PackOutput output,
                              CompletableFuture<HolderLookup.Provider> lookup) {
        generator.addProvider(event.includeServer(), named("Mind recipes", new Recipes(output, lookup)));
        generator.addProvider(event.includeServer(), named("Mind loot", new LootTableProvider(output, Set.of(),
                List.of(new LootTableProvider.SubProviderEntry(BlockLoot::new, LootContextParamSets.BLOCK)), lookup)));
        generator.addProvider(event.includeServer(), named("Mind advancements",
                new AdvancementProvider(output, lookup, event.getExistingFileHelper(), List.of(new MindAdvancements()))));
    }

    private static DataProvider named(String name, DataProvider provider) {
        return new DataProvider() {
            @Override
            public CompletableFuture<?> run(CachedOutput cache) {
                return provider.run(cache);
            }

            @Override
            public String getName() {
                return name;
            }
        };
    }

    private static final class Recipes extends SteppedRecipeProvider {
        private Recipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries);
        }

        @Override
        protected void buildRecipes(RecipeOutput output) {
            ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, MindRegistry.RESEARCH_TABLE_ITEM.get())
                    .pattern("FBI")
                    .pattern("PPP")
                    .pattern("P P")
                    .define('F', Items.FEATHER)
                    .define('B', Items.BOOK)
                    .define('I', Items.INK_SAC)
                    .define('P', ItemTags.PLANKS)
                    .unlockedBy(STEP, ON_STEP)
                    .save(output);
        }
    }

    private static final class BlockLoot extends BlockLootSubProvider {
        private BlockLoot(HolderLookup.Provider registries) {
            super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
        }

        @Override
        protected void generate() {
            dropSelf(MindRegistry.RESEARCH_TABLE.get());
        }

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return List.of(MindRegistry.RESEARCH_TABLE.get());
        }
    }
}
