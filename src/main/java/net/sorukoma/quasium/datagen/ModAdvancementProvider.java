package net.sorukoma.quasium.datagen;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
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
import net.sorukoma.quasium.advancements.triggers.HookahOpenedTrigger;
import net.sorukoma.quasium.advancements.triggers.KaifLevelTrigger;
import net.sorukoma.quasium.advancements.triggers.QuasiumOreMinedTrigger;
import net.sorukoma.quasium.advancements.triggers.QuasiumModAdvancements; // Добавлен импорт QuasiumModAdvancements
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;

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
                            new ItemStack(ModItems.QUASIUMINGOT.get()),
                            Component.translatable("advancements.quasium.root.title"),
                            Component.translatable("advancements.quasium.root.description"),
                            ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "textures/block/quasium_block.png"), // Пример фоновой текстуры
                            AdvancementType.TASK,
                            false, // show_toast
                            false, // announce_to_chat
                            false // hidden
                    )
                    .addCriterion("tick", PlayerTrigger.TriggerInstance.tick())
                    .save(saver, ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "root"), existingFileHelper);

            // Smelting Quasium Advancement
            Advancement.Builder.advancement()
                    .parent(rootAdvancement)
                    .display(
                            new ItemStack(ModItems.QUASIUMINGOT.get()),
                            Component.translatable("advancements.quasium.smelt_raw_quasium.title"),
                            Component.translatable("advancements.quasium.smelt_raw_quasium.description"),
                            null, // No background for child advancements
                            AdvancementType.TASK,
                            true, // show_toast
                            true, // announce_to_chat
                            false // hidden
                    )
                    .addCriterion("smelted_quasium", InventoryChangeTrigger.TriggerInstance.hasItems(
                            ItemPredicate.Builder.item().of(ModItems.QUASIUMINGOT.get()).build()
                    ))
                    .save(saver, ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "smelt_raw_quasium"), existingFileHelper);

            // Kaif Level 30 Advancement
            Advancement.Builder.advancement()
                    .parent(rootAdvancement) // Теперь родитель - основное корневое достижение
                    .display(
                            new ItemStack(Items.EMERALD), // Используем emerald как иконку
                            Component.translatable("advancements.quasium.kaif_level_30.title"),
                            Component.translatable("advancements.quasium.kaif_level_30.description"),
                            null, // No background for child advancements
                            AdvancementType.TASK,
                            true, // show_toast
                            true, // announce_to_chat
                            false // hidden
                    )
                    .addCriterion("reached_kaif_30", new Criterion<>(
                            QuasiumModAdvancements.KAIF_LEVEL.get(), // Получаем зарегистрированный триггер
                            new KaifLevelTrigger.TriggerInstance(Optional.empty(), 30)
                    ))
                    .rewards(net.minecraft.advancements.AdvancementRewards.Builder.experience(10)) // Награда в 10 опыта
                    .save(saver, ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "kaif/kaif_level_30"), existingFileHelper);

            // Hookah Opened Advancement
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

            // Quasium Ore Mined Advancement
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
