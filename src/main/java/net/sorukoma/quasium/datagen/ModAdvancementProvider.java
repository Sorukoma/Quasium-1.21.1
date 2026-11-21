package net.sorukoma.quasium.datagen;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPredicate;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.neoforged.neoforge.common.data.AdvancementProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.advancements.triggers.SmokecraftTrigger;
import net.sorukoma.quasium.block.ModBlocks;
import net.sorukoma.quasium.golem.MarkBlocks;
import net.sorukoma.quasium.item.EssenceBottleItem;
import net.sorukoma.quasium.item.ModItemTags;
import net.sorukoma.quasium.item.ModItems;
import net.sorukoma.quasium.magic.CozyEvening;
import net.sorukoma.quasium.magic.NoteScanner;
import net.sorukoma.quasium.magic.NoteType;
import net.sorukoma.quasium.progression.BookGift;
import net.sorukoma.quasium.selection.SelectionRegistry;
import net.sorukoma.quasium.selection.genetics.Genetics;
import net.sorukoma.quasium.selection.genetics.HybridCollection;
import net.sorukoma.quasium.selection.quality.Aphids;
import net.sorukoma.quasium.selection.variety.EchoMuffle;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ModAdvancementProvider extends AdvancementProvider {
    private static final ResourceLocation BACKGROUND =
            ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "textures/gui/advancements/backgrounds/quasium.png");

    public static final ResourceKey<LootTable> BOOK_GIFT = ResourceKey.create(Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "gifts/progression_book"));

    public ModAdvancementProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, existingFileHelper, List.of(new ModAdvancementGenerator(), new AllTobaccosAdvancement(),
                new SmokeTricksAdvancement(), new TetradAdvancements()));
    }

    static void bookGift(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
        output.accept(BOOK_GIFT, LootTable.lootTable().withPool(LootPool.lootPool().add(LootItem.lootTableItem(ModItems.PROGRESSION_BOOK.get()))));
    }

    private static class ModAdvancementGenerator implements AdvancementGenerator {
        @Override
        public void generate(HolderLookup.Provider registries, Consumer<AdvancementHolder> saver, ExistingFileHelper existingFileHelper) {
            Map<String, AdvancementHolder> steps = new HashMap<>();
            for (ProgressionSteps.Step step : ProgressionSteps.all()) {
                step.advancement().ifPresent(id -> steps.put(id, stepAdvancement(step, id, steps, saver, existingFileHelper)));
            }

            AdvancementHolder root = steps.get("root");
            AdvancementHolder firstCast = steps.get("smokecraft/first_cast");
            smokecraft(saver, existingFileHelper, firstCast, "frost_creeper",
                    new ItemStack(Items.SNOWBALL), AdvancementType.GOAL, "kill:frost_breath:creeper");
            AdvancementHolder firstZone = steps.get("smokecraft/zone_1");
            smokecraft(saver, existingFileHelper, firstZone, "zone_3",
                    new ItemStack(ModItems.MUSIC_DISC_LOFI_KAIF_BEATS.get()), AdvancementType.GOAL, "zone_3");
            smokecraft(firstZone, CozyEvening.EVENT, new ItemStack(Items.LANTERN), AdvancementType.GOAL, true)
                    .addCriterion(CozyEvening.EVENT, SmokecraftTrigger.on(CozyEvening.EVENT))
                    .save(saver, smokecraftId(CozyEvening.EVENT), existingFileHelper);
            smokecraft(saver, existingFileHelper, firstCast, "ring_combo_5",
                    new ItemStack(Items.FIREWORK_STAR), AdvancementType.GOAL, "ring_combo_5");
            smokecraft(saver, existingFileHelper, firstCast, "smoke_step",
                    new ItemStack(Items.ENDER_PEARL), AdvancementType.TASK, "smoke_step");
            Advancement.Builder allUpgrades = smokecraft(firstCast, "all_upgrades",
                    new ItemStack(ModItems.FOCUS_UPGRADE_RESONATOR.get()), AdvancementType.CHALLENGE);
            for (var upgrade : List.of(ModItems.FOCUS_UPGRADE_RESONATOR, ModItems.FOCUS_UPGRADE_COLLIMATOR, ModItems.FOCUS_UPGRADE_COOLER,
                    ModItems.FOCUS_UPGRADE_SPLITTER, ModItems.FOCUS_UPGRADE_CONDENSER, ModItems.FOCUS_UPGRADE_FILTER)) {
                allUpgrades.addCriterion(upgrade.getId().getPath(), InventoryChangeTrigger.TriggerInstance.hasItems(upgrade.get()));
            }
            allUpgrades.save(saver, smokecraftId("all_upgrades"), existingFileHelper);

            AdvancementHolder bellows = steps.get("smokecraft/bellows");
            smokecraft(saver, existingFileHelper, bellows, "hammer_craft",
                    new ItemStack(Items.PISTON), AdvancementType.GOAL, "hammer_craft");
            smokecraft(saver, existingFileHelper, bellows, "cannon_kill",
                    new ItemStack(Items.DISPENSER), AdvancementType.GOAL, "cannon_kill");
            smokecraft(saver, existingFileHelper, bellows, "cart_cast",
                    new ItemStack(Items.MINECART), AdvancementType.CHALLENGE, "cart_cast");
            AdvancementHolder firstMark = smokecraft(saver, existingFileHelper, root, "mark_place",
                    new ItemStack(MarkBlocks.HEAT_MARK_ITEM.get()), AdvancementType.TASK, "mark_place");
            smokecraft(saver, existingFileHelper, firstMark, "golem_heat",
                    new ItemStack(Items.CHARCOAL), AdvancementType.GOAL, "golem_heat");
            smokecraft(saver, existingFileHelper, firstMark, "golem_harvest",
                    new ItemStack(Items.WHEAT), AdvancementType.GOAL, "golem_harvest");
            smokecraft(saver, existingFileHelper, firstMark, "warden_kill",
                    new ItemStack(Items.ZOMBIE_HEAD), AdvancementType.GOAL, "warden_kill");
            smokecraft(saver, existingFileHelper, root, "kiln_infuse",
                    new ItemStack(Items.IRON_SWORD), AdvancementType.TASK, "kiln_infuse");

            Advancement.Builder.advancement()
                    .addCriterion("has_tobacco_seeds", ProgressionSteps.has(ModItemTags.TOBACCO_SEEDS))
                    .addCriterion("has_herb_seeds", ProgressionSteps.has(ModItems.MINT_CROP_SEEDS, ModItems.ANISE_SEEDS,
                            ModItems.LAVENDER_SEEDS, ModItems.COCOA_CROP_SEEDS))
                    .addCriterion("has_mod_item", ProgressionSteps.has(ModItemTags.FIRST_CONTACT))
                    .addCriterion(BookGift.CONTACT, SmokecraftTrigger.on(BookGift.CONTACT))
                    .requirements(AdvancementRequirements.Strategy.OR)
                    .rewards(AdvancementRewards.Builder.loot(BOOK_GIFT))
                    .save(saver, BookGift.ADVANCEMENT, existingFileHelper);
            selection(steps, saver, existingFileHelper);
        }

        private static void selection(Map<String, AdvancementHolder> steps, Consumer<AdvancementHolder> saver,
                                      ExistingFileHelper existingFileHelper) {
            AdvancementHolder seeds = steps.get("progression/found_seeds");
            AdvancementHolder firstHybrid = smokecraft(saver, existingFileHelper, seeds, "hybrid_first",
                    new ItemStack(ModItems.HYBRID_TOBACCO_SEEDS.get()), AdvancementType.TASK, HybridCollection.FIRST_HYBRID_EVENT);
            smokecraft(saver, existingFileHelper, firstHybrid, "hybrid_wild_pairs",
                    new ItemStack(ModItems.HYBRID_TOBACCO_LEAVES.get()), AdvancementType.CHALLENGE, HybridCollection.WILD_PAIRS_EVENT);
            smokecraft(saver, existingFileHelper, seeds, "aphid_cure",
                    EssenceBottleItem.of(NoteType.EMBER.id(), 20), AdvancementType.TASK, Aphids.CURE_EVENT);
            smokecraft(saver, existingFileHelper, steps.get("kaif/kaif_level_30"), "geode",
                    new ItemStack(SelectionRegistry.QUASIUM_CLUSTER.get()), AdvancementType.GOAL, "geode");
            smokecraft(saver, existingFileHelper, steps.get("progression/self_mixed_obtained"), "echo_warden",
                    new ItemStack(ModItems.ECHO_LEAVES.get()), AdvancementType.GOAL, EchoMuffle.WARDEN_EVENT);
            smokecraft(saver, existingFileHelper, steps.get("smokecraft/first_scan"), "notes_100",
                    new ItemStack(ModBlocks.NOTE_JAR_ITEM.get()), AdvancementType.GOAL, NoteScanner.SCHOLAR_EVENT);

            AdvancementHolder fiveStars = smokecraft(seeds, "leaf_five_star", new ItemStack(ModItems.DIKAR_LEAVES.get()), AdvancementType.TASK)
                    .addCriterion("leaf_quality_5", fiveStarLeaves(ItemPredicate.Builder.item().of(ModItemTags.TOBACCO_LEAVES)))
                    .save(saver, smokecraftId("leaf_five_star"), existingFileHelper);
            Advancement.Builder allWild = smokecraft(fiveStars, "wild_five_star", new ItemStack(Items.NETHER_STAR), AdvancementType.GOAL);
            for (var leaves : List.of(ModItems.DIKAR_LEAVES, ModItems.PESCHANIK_LEAVES, ModItems.BOLOTNIK_LEAVES, ModItems.LEDYANOY_LEAVES)) {
                allWild.addCriterion(leaves.getId().getPath(), fiveStarLeaves(ItemPredicate.Builder.item().of(leaves.get())));
            }
            allWild.save(saver, smokecraftId("wild_five_star"), existingFileHelper);
        }

        private static Criterion<InventoryChangeTrigger.TriggerInstance> fiveStarLeaves(ItemPredicate.Builder leaves) {
            return InventoryChangeTrigger.TriggerInstance.hasItems(leaves
                    .hasComponents(DataComponentPredicate.builder().expect(SelectionRegistry.LEAF_QUALITY.get(), Genetics.MAX_STARS).build()));
        }

        private static AdvancementHolder stepAdvancement(ProgressionSteps.Step step, String id, Map<String, AdvancementHolder> saved,
                                                         Consumer<AdvancementHolder> saver, ExistingFileHelper existingFileHelper) {
            boolean root = id.equals("root");
            Advancement.Builder builder = Advancement.Builder.advancement()
                    .display(new ItemStack(step.icon()),
                            Component.translatable("advancements.quasium." + step.key() + ".title"),
                            Component.translatable("advancements.quasium." + step.key() + ".description"),
                            root ? BACKGROUND : null, AdvancementType.TASK, !root, !root, false)
                    .requirements(AdvancementRequirements.Strategy.OR);
            if (!root) {
                AdvancementHolder parent = saved.get(step.parent());
                if (parent == null) throw new IllegalStateException(id + " is listed before its parent " + step.parent());
                builder.parent(parent);
            }
            step.criteria().forEach(builder::addCriterion);
            if (step.experience() > 0) builder.rewards(AdvancementRewards.Builder.experience(step.experience()));
            return builder.save(saver, ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, id), existingFileHelper);
        }

        private static AdvancementHolder smokecraft(Consumer<AdvancementHolder> saver, ExistingFileHelper existingFileHelper,
                                                    AdvancementHolder parent, String name, ItemStack icon,
                                                    AdvancementType type, String event) {
            return smokecraft(parent, name, icon, type)
                    .addCriterion(event, SmokecraftTrigger.on(event))
                    .save(saver, smokecraftId(name), existingFileHelper);
        }

        private static Advancement.Builder smokecraft(AdvancementHolder parent, String name, ItemStack icon, AdvancementType type) {
            return smokecraft(parent, name, icon, type, false);
        }

        private static Advancement.Builder smokecraft(AdvancementHolder parent, String name, ItemStack icon, AdvancementType type,
                                                      boolean hidden) {
            return Advancement.Builder.advancement()
                    .parent(parent)
                    .display(icon,
                            Component.translatable("advancements.quasium.smokecraft." + name + ".title"),
                            Component.translatable("advancements.quasium.smokecraft." + name + ".description"),
                            null, type, true, true, hidden);
        }

        private static ResourceLocation smokecraftId(String name) {
            return ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "smokecraft/" + name);
        }
    }
}
