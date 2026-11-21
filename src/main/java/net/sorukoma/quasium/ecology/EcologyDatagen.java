package net.sorukoma.quasium.ecology;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.advancements.AdvancementProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.advancements.triggers.SmokecraftTrigger;
import net.sorukoma.quasium.item.ModItems;
import net.sorukoma.quasium.tea.SealBlocks;
import net.sorukoma.quasium.tea.TeaBlocks;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public final class EcologyDatagen {
    private EcologyDatagen() {
    }

    public static void gather(GatherDataEvent event, DataGenerator generator, PackOutput output,
                              CompletableFuture<HolderLookup.Provider> lookup) {
        generator.addProvider(event.includeServer(), named("Ecology recipes", new EcologyRecipes(output, lookup)));
        generator.addProvider(event.includeServer(), named("Ecology loot", new LootTableProvider(output, Set.of(),
                List.of(new LootTableProvider.SubProviderEntry(EcologyLoot::new, LootContextParamSets.BLOCK)), lookup)));
        generator.addProvider(event.includeServer(), named("Ecology advancements",
                new AdvancementProvider(output, lookup, List.of((registries, saver) -> achievements(saver)))));
    }

    private static void achievements(Consumer<AdvancementHolder> saver) {
        achievement(saver, "ecology/smoky_air", "dymnik_kill", SmogMobs.DYMNIK_SPAWN_EGG, AdvancementType.TASK);
        achievement(saver, "ecology/smoky_air", "garevshchik_kill", SmogMobs.GAREVSHCHIK_SPAWN_EGG, AdvancementType.GOAL);
        achievement(saver, "ecology/smoky_air", "smog_cleansed", ModItems.LAVENDER, AdvancementType.CHALLENGE);
        achievement(saver, "ecology/tea_path", "tea_cup", TeaBlocks.CUP_OF_TEA, AdvancementType.TASK);
        achievement(saver, "ecology/tea_cup", "tea_party", TeaBlocks.TEAPOT, AdvancementType.GOAL);
        achievement(saver, "ecology/tea_path", "sobriety_seal", SealBlocks.SOBRIETY_SEAL_ITEM, AdvancementType.GOAL);
    }

    private static void achievement(Consumer<AdvancementHolder> saver, String parent, String event, ItemLike icon, AdvancementType type) {
        saver.accept(Advancement.Builder.advancement()
                .parent(ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, parent))
                .display(new ItemStack(icon),
                        Component.translatable("advancements.quasium.ecology." + event + ".title"),
                        Component.translatable("advancements.quasium.ecology." + event + ".description"),
                        null, type, true, true, false)
                .addCriterion(event, SmokecraftTrigger.on(event))
                .build(ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "ecology/" + event)));
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
}
