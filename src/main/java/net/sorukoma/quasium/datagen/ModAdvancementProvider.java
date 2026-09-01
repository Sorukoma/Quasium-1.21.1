package net.sorukoma.quasium.datagen;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.PlayerTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.data.AdvancementProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.item.ModItems;
import net.sorukoma.quasium.block.ModBlocks;
import net.sorukoma.quasium.advancements.triggers.HookahOpenedTrigger;
import net.sorukoma.quasium.advancements.triggers.KaifLevelTrigger;
import net.sorukoma.quasium.advancements.triggers.QuasiumOreMinedTrigger;
import net.sorukoma.quasium.advancements.triggers.QuasiumModAdvancements;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class ModAdvancementProvider extends AdvancementProvider {

    public ModAdvancementProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, existingFileHelper, List.of(new ModAdvancementGenerator()));
    }

    private static class ModAdvancementGenerator implements AdvancementGenerator {
        @Override
        public void generate(HolderLookup.Provider registries, Consumer<AdvancementHolder> saver, ExistingFileHelper existingFileHelper) {
            // Root Advancement
            AdvancementHolder rootAdvancement = Advancement.Builder.advancement()
                    .display(
                            new ItemStack(ModItems.PROGRESSION_BOOK.get()),
                            Component.translatable("advancements.quasium.root.title"),
                            Component.translatable("advancements.quasium.root.description"),
                            ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "textures/item/progression_book.png"),
                            AdvancementType.TASK,
                            false,
                            false,
                            false
                    )
                    .addCriterion("tick", PlayerTrigger.TriggerInstance.tick())
                    .save(saver, ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "root"), existingFileHelper);

            // ─── Progression: Found Seeds ───
            Advancement.Builder.advancement()
                    .parent(rootAdvancement)
                    .display(
                            new ItemStack(ModItems.TOBACCO_SEEDS.get()),
                            Component.translatable("advancements.quasium.found_seeds.title"),
                            Component.translatable("advancements.quasium.found_seeds.description"),
                            null,
                            AdvancementType.TASK,
                            true,
                            true,
                            false
                    )
                    .addCriterion("has_tobacco_seeds", InventoryChangeTrigger.TriggerInstance.hasItems(
                            ItemPredicate.Builder.item().of(ModItems.TOBACCO_SEEDS.get()).build()
                    ))
                    .addCriterion("has_makhorka_seeds", InventoryChangeTrigger.TriggerInstance.hasItems(
                            ItemPredicate.Builder.item().of(ModItems.MAKHORKA_SEEDS.get()).build()
                    ))
                    .requirements(AdvancementRequirements.Strategy.OR)
                    .save(saver, ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "progression/found_seeds"), existingFileHelper);

            // ─── Progression: Fermented Leaves ───
            Advancement.Builder.advancement()
                    .parent(rootAdvancement)
                    .display(
                            new ItemStack(ModItems.TOBACCO_LEAVES.get()),
                            Component.translatable("advancements.quasium.fermented_leaves.title"),
                            Component.translatable("advancements.quasium.fermented_leaves.description"),
                            null,
                            AdvancementType.TASK,
                            true,
                            true,
                            false
                    )
                    .addCriterion("has_fermented_barrel", InventoryChangeTrigger.TriggerInstance.hasItems(
                            ItemPredicate.Builder.item().of(ModBlocks.FERMENTATION_BARREL_ITEM.get()).build()
                    ))
                    .save(saver, ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "progression/fermented_leaves"), existingFileHelper);

            // ─── Progression: Obtained Tallow ───
            Advancement.Builder.advancement()
                    .parent(rootAdvancement)
                    .display(
                            new ItemStack(ModItems.TALLOW.get()),
                            Component.translatable("advancements.quasium.obtained_tallow.title"),
                            Component.translatable("advancements.quasium.obtained_tallow.description"),
                            null,
                            AdvancementType.TASK,
                            true,
                            true,
                            false
                    )
                    .addCriterion("has_tallow", InventoryChangeTrigger.TriggerInstance.hasItems(
                            ItemPredicate.Builder.item().of(ModItems.TALLOW.get()).build()
                    ))
                    .addCriterion("has_drying_shelf", InventoryChangeTrigger.TriggerInstance.hasItems(
                            ItemPredicate.Builder.item().of(ModBlocks.DRYING_SHELF_ITEM.get()).build()
                    ))
                    .requirements(AdvancementRequirements.Strategy.OR)
                    .save(saver, ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "progression/obtained_tallow"), existingFileHelper);

            // ─── Progression: Obtained Glycerin ───
            Advancement.Builder.advancement()
                    .parent(rootAdvancement)
                    .display(
                            new ItemStack(ModItems.GLYCERIN_BOTTLE.get()),
                            Component.translatable("advancements.quasium.obtained_glycerin.title"),
                            Component.translatable("advancements.quasium.obtained_glycerin.description"),
                            null,
                            AdvancementType.TASK,
                            true,
                            true,
                            false
                    )
                    .addCriterion("has_glycerin", InventoryChangeTrigger.TriggerInstance.hasItems(
                            ItemPredicate.Builder.item().of(ModItems.GLYCERIN_BOTTLE.get()).build()
                    ))
                    .addCriterion("has_molasses", InventoryChangeTrigger.TriggerInstance.hasItems(
                            ItemPredicate.Builder.item().of(ModItems.MOLASSES_BOTTLE.get()).build()
                    ))
                    .requirements(AdvancementRequirements.Strategy.OR)
                    .save(saver, ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "progression/obtained_glycerin"), existingFileHelper);

            // ─── Progression: Obtained Charcoal ───
            Advancement.Builder.advancement()
                    .parent(rootAdvancement)
                    .display(
                            new ItemStack(ModItems.CHARCOAL_25.get()),
                            Component.translatable("advancements.quasium.obtained_charcoal.title"),
                            Component.translatable("advancements.quasium.obtained_charcoal.description"),
                            null,
                            AdvancementType.TASK,
                            true,
                            true,
                            false
                    )
                    .addCriterion("has_charcoal_22", InventoryChangeTrigger.TriggerInstance.hasItems(
                            ItemPredicate.Builder.item().of(ModItems.CHARCOAL_22.get()).build()
                    ))
                    .addCriterion("has_charcoal_25", InventoryChangeTrigger.TriggerInstance.hasItems(
                            ItemPredicate.Builder.item().of(ModItems.CHARCOAL_25.get()).build()
                    ))
                    .addCriterion("has_charcoal_27", InventoryChangeTrigger.TriggerInstance.hasItems(
                            ItemPredicate.Builder.item().of(ModItems.CHARCOAL_27.get()).build()
                    ))
                    .addCriterion("has_briquette", InventoryChangeTrigger.TriggerInstance.hasItems(
                            ItemPredicate.Builder.item().of(ModItems.RAW_BRIQUETTE.get()).build()
                    ))
                    .requirements(AdvancementRequirements.Strategy.OR)
                    .save(saver, ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "progression/obtained_charcoal"), existingFileHelper);

            // ─── Progression: Obtained Calloud ───
            Advancement.Builder.advancement()
                    .parent(rootAdvancement)
                    .display(
                            new ItemStack(ModItems.CALLOUD.get()),
                            Component.translatable("advancements.quasium.obtained_calloud.title"),
                            Component.translatable("advancements.quasium.obtained_calloud.description"),
                            null,
                            AdvancementType.TASK,
                            true,
                            true,
                            false
                    )
                    .addCriterion("has_calloud", InventoryChangeTrigger.TriggerInstance.hasItems(
                            ItemPredicate.Builder.item().of(ModItems.CALLOUD.get()).build()
                    ))
                    .save(saver, ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "progression/obtained_calloud"), existingFileHelper);

            // ─── Progression: Obtained Heater ───
            Advancement.Builder.advancement()
                    .parent(rootAdvancement)
                    .display(
                            new ItemStack(ModBlocks.HEATER_ITEM.get()),
                            Component.translatable("advancements.quasium.obtained_heater.title"),
                            Component.translatable("advancements.quasium.obtained_heater.description"),
                            null,
                            AdvancementType.TASK,
                            true,
                            true,
                            false
                    )
                    .addCriterion("has_heater", InventoryChangeTrigger.TriggerInstance.hasItems(
                            ItemPredicate.Builder.item().of(ModBlocks.HEATER_ITEM.get()).build()
                    ))
                    .save(saver, ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "progression/obtained_heater"), existingFileHelper);

            // ─── Progression: Obtained Bowl ───
            Advancement.Builder.advancement()
                    .parent(rootAdvancement)
                    .display(
                            new ItemStack(ModItems.HOOKAH_BOWL.get()),
                            Component.translatable("advancements.quasium.obtained_bowl.title"),
                            Component.translatable("advancements.quasium.obtained_bowl.description"),
                            null,
                            AdvancementType.TASK,
                            true,
                            true,
                            false
                    )
                    .addCriterion("has_bowl", InventoryChangeTrigger.TriggerInstance.hasItems(
                            ItemPredicate.Builder.item().of(ModItems.HOOKAH_BOWL.get()).build()
                    ))
                    .save(saver, ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "progression/obtained_bowl"), existingFileHelper);

            // ─── Progression: Flavored Tobacco ───
            Advancement.Builder.advancement()
                    .parent(rootAdvancement)
                    .display(
                            new ItemStack(ModItems.MINT_TOBACCO.get()),
                            Component.translatable("advancements.quasium.flavored_tobacco.title"),
                            Component.translatable("advancements.quasium.flavored_tobacco.description"),
                            null,
                            AdvancementType.TASK,
                            true,
                            true,
                            false
                    )
                    .addCriterion("has_mint", InventoryChangeTrigger.TriggerInstance.hasItems(
                            ItemPredicate.Builder.item().of(ModItems.MINT_TOBACCO.get()).build()
                    ))
                    .addCriterion("has_lemon", InventoryChangeTrigger.TriggerInstance.hasItems(
                            ItemPredicate.Builder.item().of(ModItems.LEMON_TOBACCO.get()).build()
                    ))
                    .addCriterion("has_lime", InventoryChangeTrigger.TriggerInstance.hasItems(
                            ItemPredicate.Builder.item().of(ModItems.LIME_TOBACCO.get()).build()
                    ))
                    .addCriterion("has_orange", InventoryChangeTrigger.TriggerInstance.hasItems(
                            ItemPredicate.Builder.item().of(ModItems.ORANGE_TOBACCO.get()).build()
                    ))
                    .addCriterion("has_grapefruit", InventoryChangeTrigger.TriggerInstance.hasItems(
                            ItemPredicate.Builder.item().of(ModItems.GRAPEFRUIT_TOBACCO.get()).build()
                    ))
                    .addCriterion("has_berry", InventoryChangeTrigger.TriggerInstance.hasItems(
                            ItemPredicate.Builder.item().of(ModItems.BERRY_TOBACCO.get()).build()
                    ))
                    .requirements(AdvancementRequirements.Strategy.OR)
                    .save(saver, ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "progression/flavored_tobacco"), existingFileHelper);

            // ─── Progression: Self-Mixed Tobacco ───
            Advancement.Builder.advancement()
                    .parent(rootAdvancement)
                    .display(
                            new ItemStack(ModItems.SELF_MIXED_TOBACCO.get()),
                            Component.translatable("advancements.quasium.self_mixed_obtained.title"),
                            Component.translatable("advancements.quasium.self_mixed_obtained.description"),
                            null,
                            AdvancementType.TASK,
                            true,
                            true,
                            false
                    )
                    .addCriterion("has_self_mixed", InventoryChangeTrigger.TriggerInstance.hasItems(
                            ItemPredicate.Builder.item().of(ModItems.SELF_MIXED_TOBACCO.get()).build()
                    ))
                    .addCriterion("has_soaking_bowl", InventoryChangeTrigger.TriggerInstance.hasItems(
                            ItemPredicate.Builder.item().of(ModBlocks.SOAKING_BOWL_ITEM.get()).build()
                    ))
                    .requirements(AdvancementRequirements.Strategy.OR)
                    .save(saver, ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "progression/self_mixed_obtained"), existingFileHelper);

            // ─── Progression: Obtained Kaifometer ───
            Advancement.Builder.advancement()
                    .parent(rootAdvancement)
                    .display(
                            new ItemStack(ModItems.KAIFOMETER.get()),
                            Component.translatable("advancements.quasium.obtained_kaifometer.title"),
                            Component.translatable("advancements.quasium.obtained_kaifometer.description"),
                            null,
                            AdvancementType.TASK,
                            true,
                            true,
                            false
                    )
                    .addCriterion("has_kaifometer", InventoryChangeTrigger.TriggerInstance.hasItems(
                            ItemPredicate.Builder.item().of(ModItems.KAIFOMETER.get()).build()
                    ))
                    .save(saver, ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "progression/obtained_kaifometer"), existingFileHelper);

            // ─── Progression: Obtained Anvil ───
            Advancement.Builder.advancement()
                    .parent(rootAdvancement)
                    .display(
                            new ItemStack(ModBlocks.QUASIUM_ANVIL_ITEM.get()),
                            Component.translatable("advancements.quasium.obtained_anvil.title"),
                            Component.translatable("advancements.quasium.obtained_anvil.description"),
                            null,
                            AdvancementType.TASK,
                            true,
                            true,
                            false
                    )
                    .addCriterion("has_anvil", InventoryChangeTrigger.TriggerInstance.hasItems(
                            ItemPredicate.Builder.item().of(ModBlocks.QUASIUM_ANVIL_ITEM.get()).build()
                    ))
                    .save(saver, ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "progression/obtained_anvil"), existingFileHelper);

            // ─── Progression: Obtained Armor ───
            Advancement.Builder.advancement()
                    .parent(rootAdvancement)
                    .display(
                            new ItemStack(ModItems.QUASIUM_CHESTPLATE.get()),
                            Component.translatable("advancements.quasium.obtained_armor.title"),
                            Component.translatable("advancements.quasium.obtained_armor.description"),
                            null,
                            AdvancementType.TASK,
                            true,
                            true,
                            false
                    )
                    .addCriterion("has_helmet", InventoryChangeTrigger.TriggerInstance.hasItems(
                            ItemPredicate.Builder.item().of(ModItems.QUASIUM_HELMET.get()).build()
                    ))
                    .addCriterion("has_chestplate", InventoryChangeTrigger.TriggerInstance.hasItems(
                            ItemPredicate.Builder.item().of(ModItems.QUASIUM_CHESTPLATE.get()).build()
                    ))
                    .addCriterion("has_leggings", InventoryChangeTrigger.TriggerInstance.hasItems(
                            ItemPredicate.Builder.item().of(ModItems.QUASIUM_LEGGINGS.get()).build()
                    ))
                    .addCriterion("has_boots", InventoryChangeTrigger.TriggerInstance.hasItems(
                            ItemPredicate.Builder.item().of(ModItems.QUASIUM_BOOTS.get()).build()
                    ))
                    .requirements(AdvancementRequirements.Strategy.OR)
                    .save(saver, ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "progression/obtained_armor"), existingFileHelper);

            // ─── Progression: Obtained Backpack Hookah ───
            Advancement.Builder.advancement()
                    .parent(rootAdvancement)
                    .display(
                            new ItemStack(ModItems.BACKPACK_HOOKAH.get()),
                            Component.translatable("advancements.quasium.obtained_backpack_hookah.title"),
                            Component.translatable("advancements.quasium.obtained_backpack_hookah.description"),
                            null,
                            AdvancementType.TASK,
                            true,
                            true,
                            false
                    )
                    .addCriterion("has_backpack_hookah", InventoryChangeTrigger.TriggerInstance.hasItems(
                            ItemPredicate.Builder.item().of(ModItems.BACKPACK_HOOKAH.get()).build()
                    ))
                    .save(saver, ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "progression/obtained_backpack_hookah"), existingFileHelper);

            // ─── Existing Advancements (keep) ───

            // Smelting Quasium
            Advancement.Builder.advancement()
                    .parent(rootAdvancement)
                    .display(
                            new ItemStack(ModItems.QUASIUMINGOT.get()),
                            Component.translatable("advancements.quasium.smelt_raw_quasium.title"),
                            Component.translatable("advancements.quasium.smelt_raw_quasium.description"),
                            null,
                            AdvancementType.TASK,
                            true,
                            true,
                            false
                    )
                    .addCriterion("smelted_quasium", InventoryChangeTrigger.TriggerInstance.hasItems(
                            ItemPredicate.Builder.item().of(ModItems.QUASIUMINGOT.get()).build()
                    ))
                    .save(saver, ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "smelt_raw_quasium"), existingFileHelper);

            // Kaif Level 30
            Advancement.Builder.advancement()
                    .parent(rootAdvancement)
                    .display(
                            new ItemStack(Items.EMERALD),
                            Component.translatable("advancements.quasium.kaif_level_30.title"),
                            Component.translatable("advancements.quasium.kaif_level_30.description"),
                            null,
                            AdvancementType.TASK,
                            true,
                            true,
                            false
                    )
                    .addCriterion("reached_kaif_30", new Criterion<>(
                            QuasiumModAdvancements.KAIF_LEVEL.get(),
                            new KaifLevelTrigger.TriggerInstance(Optional.empty(), 30)
                    ))
                    .rewards(net.minecraft.advancements.AdvancementRewards.Builder.experience(10))
                    .save(saver, ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "kaif/kaif_level_30"), existingFileHelper);

            // Hookah Opened
            Advancement.Builder.advancement()
                    .parent(rootAdvancement)
                    .display(
                            new ItemStack(ModItems.HOOKAH_PIPE.get()),
                            Component.translatable("advancements.quasium.hookah_opened.title"),
                            Component.translatable("advancements.quasium.hookah_opened.description"),
                            null,
                            AdvancementType.TASK,
                            true,
                            true,
                            false
                    )
                    .addCriterion("hookah_opened", new Criterion<>(
                            QuasiumModAdvancements.HOOKAH_OPENED.get(),
                            new HookahOpenedTrigger.TriggerInstance(Optional.empty())
                    ))
                    .save(saver, ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "hookah/opened_gui"), existingFileHelper);

            // Quasium Ore Mined
            Advancement.Builder.advancement()
                    .parent(rootAdvancement)
                    .display(
                            new ItemStack(ModItems.RAWQUASIUM.get()),
                            Component.translatable("advancements.quasium.quasium_ore_mined.title"),
                            Component.translatable("advancements.quasium.quasium_ore_mined.description"),
                            null,
                            AdvancementType.TASK,
                            true,
                            true,
                            false
                    )
                    .addCriterion("mined_quasium_ore", new Criterion<>(
                            QuasiumModAdvancements.QUASIUM_ORE_MINED.get(),
                            new QuasiumOreMinedTrigger.TriggerInstance(Optional.empty())
                    ))
                    .save(saver, ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "quasium_ore/mined_with_kaif"), existingFileHelper);
        }
    }
}
