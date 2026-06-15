package net.sorukoma.quasium.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.neoforged.neoforge.common.data.GlobalLootModifierProvider;
import net.neoforged.neoforge.common.loot.LootTableIdCondition;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.item.ModItems;
import net.sorukoma.quasium.loot.modifier.AddItemLootModifier;

import java.util.concurrent.CompletableFuture;

/**
 * Datagen-провайдер для Global Loot Modifiers.
 *
 * ШАНСЫ ДРОПА — меняйте значения float здесь для регулировки:
 *   - TOBACCO_SEEDS_CHANCE (0.1F) — шанс дропа семян табака с травы
 */
public class ModGlobalLootModifiersProvider extends GlobalLootModifierProvider {

    /** Шанс дропа семян обычного табака с травы. Ванильные семена пшеницы ~12.5% (1/8). */
    private static final float TOBACCO_SEEDS_CHANCE = 0.1F;

    public ModGlobalLootModifiersProvider(net.minecraft.data.PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, QuasiumMod.MOD_ID);
    }

    @Override
    protected void start() {
        // Семена обычного табака из короткой травы
        add("tobacco_seeds_from_short_grass",
                new AddItemLootModifier(
                        new LootItemCondition[]{
                                LootTableIdCondition.builder(ResourceLocation.fromNamespaceAndPath("minecraft", "blocks/short_grass")).build(),
                                LootItemRandomChanceCondition.randomChance(TOBACCO_SEEDS_CHANCE).build()
                        },
                        new ItemStack(ModItems.TOBACCO_SEEDS.get())
                )
        );

        // Семена обычного табака из высокой травы
        add("tobacco_seeds_from_tall_grass",
                new AddItemLootModifier(
                        new LootItemCondition[]{
                                LootTableIdCondition.builder(ResourceLocation.fromNamespaceAndPath("minecraft", "blocks/tall_grass")).build(),
                                LootItemRandomChanceCondition.randomChance(TOBACCO_SEEDS_CHANCE).build()
                        },
                        new ItemStack(ModItems.TOBACCO_SEEDS.get())
                )
        );
    }
}