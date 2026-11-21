package net.sorukoma.quasium.datagen;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.PlayerTrigger;
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger;
import net.minecraft.core.component.DataComponentPredicate;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.advancements.triggers.HookahOpenedTrigger;
import net.sorukoma.quasium.advancements.triggers.KaifLevelTrigger;
import net.sorukoma.quasium.advancements.triggers.QuasiumOreMinedTrigger;
import net.sorukoma.quasium.advancements.triggers.SmokecraftTrigger;
import net.sorukoma.quasium.block.ModBlocks;
import net.sorukoma.quasium.cart.HookahCarts;
import net.sorukoma.quasium.data.PlayerFlavorPreferenceData;
import net.sorukoma.quasium.component.ModDataComponents;
import net.sorukoma.quasium.draft.DraftBlocks;
import net.sorukoma.quasium.item.ModItemTags;
import net.sorukoma.quasium.item.ModItems;
import net.sorukoma.quasium.mind.MindRegistry;
import net.sorukoma.quasium.ritual.RitualRegistry;
import net.sorukoma.quasium.smokelands.block.SmokelandsBlocks;
import net.sorukoma.quasium.smokelands.item.SmokelandsItems;
import net.sorukoma.quasium.tea.TeaBlocks;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ProgressionSteps {
    private ProgressionSteps() {
    }

    public static List<Step> all() {
        return Index.STEPS;
    }

    @Nullable
    public static Step teaching(ResourceLocation recipe) {
        return Index.BY_RECIPE.get(recipe);
    }

    public static RecipeOutput gate(RecipeOutput output) {
        return new RecipeOutput() {
            @Override
            public void accept(ResourceLocation id, Recipe<?> recipe, @Nullable AdvancementHolder advancement, ICondition... conditions) {
                Step step = teaching(id);
                if (step == null) throw new IllegalStateException("no progression step teaches " + id);
                if (advancement == null) throw new IllegalStateException(id + " comes without a recipe advancement");
                Advancement.Builder unlock = output.advancement()
                        .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(id))
                        .rewards(AdvancementRewards.Builder.recipe(id))
                        .requirements(AdvancementRequirements.Strategy.OR);
                step.criteria.forEach(unlock::addCriterion);
                output.accept(id, recipe, unlock.build(advancement.id()), conditions);
            }

            @Override
            public Advancement.Builder advancement() {
                return output.advancement();
            }
        };
    }

    private static List<Step> build() {
        return List.of(
                step("root").on("tick", PlayerTrigger.TriggerInstance.tick())
                        .teaches("progression_book_from_book_and_tobacco_seeds")
                        .opens("root", ModItems.PROGRESSION_BOOK, "root"),
                step("found_seeds").on("has_tobacco_seeds", has(ModItemTags.TOBACCO_SEEDS))
                        .teaches("wooden_tobacco_pole")
                        .opens("progression/found_seeds", ModItems.TOBACCO_SEEDS, "found_seeds"),
                step("wooden_pole").on("has_wooden_pole", has(ModBlocks.WOODEN_TOBACCO_POLE_ITEM)).teaches("reinforced_tobacco_pole"),
                step("reinforced_pole").on("has_reinforced_pole", has(ModBlocks.REINFORCED_TOBACCO_POLE_ITEM)).teaches("golden_tobacco_pole"),
                step("golden_pole").on("has_golden_pole", has(ModBlocks.GOLDEN_TOBACCO_POLE_ITEM)).teaches("diamond_tobacco_pole"),
                step("drying").on("has_leaves", has(ModItemTags.TOBACCO_LEAVES)).on("has_tallow", has(ModItems.TALLOW))
                        .on("has_drying_shelf", has(ModBlocks.DRYING_SHELF_ITEM))
                        .teaches("drying_shelf_from_planks_and_sticks", "fries_from_potatoes_and_tallow")
                        .opens("progression/obtained_tallow", ModItems.TALLOW, "obtained_tallow"),
                step("fermentation").on("has_dried_leaves", leavesDried(2)).on("has_overdried_leaves", leavesDried(3))
                        .on("has_fermented_barrel", has(ModBlocks.FERMENTATION_BARREL_ITEM))
                        .teaches("fermentation_barrel_from_planks_and_iron", "netherite_hammer_from_netherite_ingot")
                        .opens("progression/fermented_leaves", ModItems.TOBACCO_LEAVES, "fermented_leaves"),
                step("molasses").on("has_tallow", has(ModItems.TALLOW)).on("has_glycerin", has(ModItems.GLYCERIN_BOTTLE))
                        .on("has_molasses", has(ModItems.MOLASSES_BOTTLE))
                        .teaches("bottled_ghast_acid_from_ghast_tear", "molasses_bottle_from_glycerin")
                        .opens("progression/obtained_glycerin", ModItems.GLYCERIN_BOTTLE, "obtained_glycerin"),
                step("garden").on("has_fermented_leaves", leavesFermented()).on("has_harvest", has(harvest()))
                        .on("has_planting", has(ModBlocks.BONSAI_POT_ITEM, ModItems.LEMON_SAPLING, ModItems.LIME_SAPLING,
                                ModItems.ORANGE_SAPLING, ModItems.GRAPEFRUIT_SAPLING, ModItems.GRAPE_SAPLING, ModItems.COCONUT_SAPLING,
                                ModItems.MINT_CROP_SEEDS, ModItems.ANISE_SEEDS, ModItems.LAVENDER_SEEDS, ModItems.COCOA_CROP_SEEDS))
                        .teaches("lemon_sapling_from_oak_sapling", "lime_sapling_from_oak_sapling", "orange_sapling_from_oak_sapling",
                                "grapefruit_sapling_from_oak_sapling", "grape_sapling_from_azalea", "coconut_sapling_from_jungle_sapling",
                                "mint_seeds_from_fern", "anise_seeds_from_wheat_seeds", "lavender_seeds_from_lilac",
                                "cocoa_seeds_from_cocoa_beans", "bonsai_pot_from_bricks_and_dirt")
                        .teaches(PlayerFlavorPreferenceData.FLAVORS.stream().map(FlavorRecipeProvider::vialRecipe).toArray(String[]::new))
                        .opens("progression/obtained_fruit", ModItems.LEMON, "obtained_fruit"),
                step("essence").on("has_harvest", has(harvest()))
                        .on("has_essence", has(ModBlocks.ESSENCE_PRESS_ITEM, ModItems.ESSENCE_BOTTLE))
                        .teaches("essence_press")
                        .opens("progression/obtained_essence", ModItems.ESSENCE_BOTTLE, "obtained_essence"),
                step("soaking").on("has_essence_or_molasses", has(ModItems.ESSENCE_BOTTLE, ModItems.MOLASSES_BOTTLE))
                        .on("has_self_mixed", has(ModItems.SELF_MIXED_TOBACCO)).on("has_soaking_bowl", has(ModBlocks.SOAKING_BOWL_ITEM))
                        .teaches("soaking_bowl_from_brick_and_clay")
                        .opens("progression/self_mixed_obtained", ModItems.SELF_MIXED_TOBACCO, "self_mixed_obtained"),
                step("first_tobacco").on("has_flavored_tobacco", has(ModItemTags.FLAVORED_TOBACCOS))
                        .on("has_self_mixed", has(ModItems.SELF_MIXED_TOBACCO))
                        .on("has_tobacco_gram", has(ModItems.ONE_GRAM_TOBACCO, ModItems.ONE_GRAM_SELF_MIXED_TOBACCO))
                        .on("has_briquette", has(ModItems.RAW_BRIQUETTE))
                        .on("has_charcoal", has(ModItems.CHARCOAL_22, ModItems.CHARCOAL_25, ModItems.CHARCOAL_27))
                        .on("has_heater", has(ModBlocks.HEATER_ITEM)).on("has_calloud", has(ModItems.CALLOUD))
                        .on("has_bowl", has(ModItems.UNFIRED_HOOKAH_BOWL, ModItems.HOOKAH_BOWL))
                        .teaches("raw_briquette_from_charcoal_and_clay", "charcoal_22_from_briquette_stonecutting",
                                "charcoal_25_from_briquette_stonecutting", "charcoal_27_from_briquette_stonecutting",
                                "heater_from_brick_copper_iron", "calloud_from_iron_and_copper", "unfired_hookah_bowl_from_clay",
                                "hookah_bowl_from_smelting_unfired_hookah_bowl", "hookah_block", "hookah_pipe")
                        .opens("progression/first_tobacco", ModItems.MINT_TOBACCO, "first_tobacco"),
                step("hookah").on("hookah_opened", HookahOpenedTrigger.opened())
                        .on("has_hookah", has(ModBlocks.HOOKAH_BLOCK_ITEM, ModBlocks.MODULAR_HOOKAH_AMBER_ITEM, ModBlocks.MODULAR_HOOKAH_BLACK_ITEM,
                                ModBlocks.MODULAR_HOOKAH_BLUE_ITEM, ModBlocks.MODULAR_HOOKAH_RED_ITEM, ModBlocks.MODULAR_HOOKAH_WHITE_ITEM,
                                HookahCarts.HOOKAH_MINECART_ITEM))
                        .teaches("hookah_diffuser", "hookah_cooler", "hookah_ice_chamber", "modular_hookah_amber", "modular_hookah_black",
                                "modular_hookah_blue", "modular_hookah_red", "modular_hookah_white", "modular_shaft_amber",
                                "modular_shaft_black", "modular_shaft_blue", "modular_shaft_red", "modular_shaft_white", "hookah_minecart")
                        .opens("hookah/opened_gui", ModItems.HOOKAH_PIPE, "hookah_opened"),
                step("backpack_hookah").on("has_anvil", has(ModBlocks.QUASIUM_ANVIL_ITEM))
                        .on("has_backpack_hookah", has(ModItems.BACKPACK_HOOKAH))
                        .opens("progression/obtained_backpack_hookah", ModItems.BACKPACK_HOOKAH, "obtained_backpack_hookah"),
                step("kaifometer").on("reached_kaif_1", KaifLevelTrigger.reached(1)).on("has_kaifometer", has(ModItems.KAIFOMETER))
                        .teaches("kaifometer_from_iron_and_redstone")
                        .opens("progression/obtained_kaifometer", ModItems.KAIFOMETER, "obtained_kaifometer"),
                step("kaif_20").on("reached_kaif_20", KaifLevelTrigger.reached(20))
                        .opens("kaif/kaif_level_20", Items.EXPERIENCE_BOTTLE, "kaif_level_20"),
                step("kaif_30").on("reached_kaif_30", KaifLevelTrigger.reached(30))
                        .opens("kaif/kaif_level_30", Items.EMERALD, "kaif_level_30").rewarding(10),
                step("overdose").on("overdose", SmokecraftTrigger.on("overdose"))
                        .opens("smokecraft/overdose", ModItems.BURNED_TOBACCO, "smokecraft.overdose"),
                step("first_scan").on("scan", SmokecraftTrigger.on("scan"))
                        .teaches("distillation_cube", "note_jar", "spell_tetrad")
                        .opens("smokecraft/first_scan", ModItems.KAIFOMETER, "smokecraft.first_scan"),
                step("first_distill").on("distill", SmokecraftTrigger.on("distill"))
                        .teaches("bellows", "clay_pipe", "copper_pipe", "glass_pipe", "smoke_valve", "smoke_splitter")
                        .opens("smokecraft/first_distill", ModBlocks.DISTILLATION_CUBE_ITEM, "smokecraft.first_distill")
                        .under("smokecraft/first_scan"),
                step("first_cast").on("cast", SmokecraftTrigger.on("cast"))
                        .teaches("mixing_table")
                        .opens("smokecraft/first_cast", ModItems.HOOKAH_PIPE, "smokecraft.first_cast"),
                step("zone_1").on("zone_1", SmokecraftTrigger.on("zone_1"))
                        .teaches("lounge_cushion", "low_table", "aroma_lamp", "glowing_bulb_amber", "glowing_bulb_black",
                                "glowing_bulb_blue", "glowing_bulb_red", "glowing_bulb_white")
                        .opens("smokecraft/zone_1", ModBlocks.SMOKE_JAR_ITEM, "smokecraft.zone_1")
                        .under("smokecraft/first_cast"),
                step("bellows").on("bellows", SmokecraftTrigger.on("bellows"))
                        .teaches("flask_charger", "pneumatic_hammer")
                        .opens("smokecraft/bellows", DraftBlocks.BELLOWS_ITEM, "smokecraft.bellows"),
                step("quasium_ore").on("reached_kaif_10", KaifLevelTrigger.reached(10))
                        .on("mined_quasium_ore", QuasiumOreMinedTrigger.mined())
                        .opens("quasium_ore/mined_with_kaif", ModItems.RAWQUASIUM, "quasium_ore_mined"),
                step("smelting").on("has_raw_quasium", has(ModItems.RAWQUASIUM)).on("smelted_quasium", has(ModItems.QUASIUMINGOT))
                        .teaches("quasiumingot_from_smelting_rawquasium", "quasiumingot_from_blasting_rawquasium",
                                "undried_quasium_block_from_quasium_ingots")
                        .opens("smelt_raw_quasium", ModItems.QUASIUMINGOT, "smelt_raw_quasium"),
                step("anvil").on("has_quasium_block", has(ModBlocks.QUASIUM_BLOCK_ITEM)).on("has_anvil", has(ModBlocks.QUASIUM_ANVIL_ITEM))
                        .teaches("quasium_anvil_from_quasium_blocks", "harvest_mark", "fill_mark", "guard_mark", "heat_mark", "zone_mark",
                                "golem_staff", "golem_feeder", "warden_blank", "program_table", "place_marker", "program_card")
                        .opens("progression/obtained_anvil", ModBlocks.QUASIUM_ANVIL_ITEM, "obtained_anvil"),
                step("smoke_portal").on("has_quasium_block", has(ModBlocks.QUASIUM_BLOCK_ITEM)).on("has_anvil", has(ModBlocks.QUASIUM_ANVIL_ITEM))
                        .on("ruined_portal", SmokecraftTrigger.on("ruined_portal"))
                        .opens("progression/smoke_portal", SmokelandsBlocks.PETRIFIED_SMOKE, "smoke_portal"),
                step("armor").on("has_armor", has(ModItems.QUASIUM_HELMET, ModItems.QUASIUM_CHESTPLATE, ModItems.QUASIUM_LEGGINGS,
                                ModItems.QUASIUM_BOOTS))
                        .opens("progression/obtained_armor", ModItems.QUASIUM_CHESTPLATE, "obtained_armor"),
                step("smoky_air").on("density_40", SmokecraftTrigger.on("density_40")).on("smog", SmokecraftTrigger.on("smog"))
                        .opens("ecology/smoky_air", Items.CAMPFIRE, "ecology.smoky_air"),
                step("tea_path").on("density_40", SmokecraftTrigger.on("density_40")).on("smog", SmokecraftTrigger.on("smog"))
                        .on("overdose", SmokecraftTrigger.on("overdose"))
                        .on("has_tea", has(TeaBlocks.TEA_SEEDS, TeaBlocks.TEA_LEAF, TeaBlocks.DRIED_TEA, TeaBlocks.TEA_CUP,
                                TeaBlocks.CUP_OF_TEA, TeaBlocks.TEAPOT, TeaBlocks.SAMOVAR_ITEM))
                        .teaches("tea_seeds_from_leaves", "samovar", "teapot", "tea_cup")
                        .opens("ecology/tea_path", TeaBlocks.TEA_LEAF, "ecology.tea_path"),
                step("research_table").on("scan", SmokecraftTrigger.on("scan"))
                        .on("has_research_table", has(MindRegistry.RESEARCH_TABLE_ITEM))
                        .teaches("research_table"),
                researched("research_notes_sorting", "note_filter", "smoke_sorter"),
                researched("research_notes_pump", "essence_pump", "auto_distiller"),
                researched("research_notes_kiln", "smoke_kiln"),
                researched("research_draft_boiler", "smoke_boiler"),
                researched("research_draft_quasium_pipes", "quasium_pipe", "quasium_fan"),
                researched("research_smog_ventilation", "ventilation"),
                researched("research_smoke_cannon", "smoke_cannon"),
                step("ritual_brazier").on("has_quasium_anvil", has(ModBlocks.QUASIUM_ANVIL_ITEM)).on("zone_2", SmokecraftTrigger.on("zone_2"))
                        .on("has_brazier", has(RitualRegistry.RITUAL_BRAZIER_ITEM))
                        .teaches("ritual_brazier", "ritual_pedestal", "signal_pipe")
                        .opens("ritual/brazier", RitualRegistry.RITUAL_BRAZIER_ITEM, "ritual.brazier")
                        .under("smokecraft/zone_1"),
                step("first_ritual").on("ritual", SmokecraftTrigger.on("ritual"))
                        .opens("ritual/first_ritual", RitualRegistry.DREAM_QUARTZ, "ritual.first_ritual")
                        .under("ritual/brazier"),
                step("ash_stone").on("has_ash_stone", has(SmokelandsBlocks.ASH_STONE)).teaches("ash_bricks"),
                step("ash_of_time").on("has_ash_of_time", has(SmokelandsItems.ASH_OF_TIME)).teaches("ash_sachet"));
    }

    private static Step step(String name) {
        return new Step(name);
    }

    private static Step researched(String node, String... recipes) {
        return step(node).on("researched", SmokecraftTrigger.on("research:" + node)).teaches(recipes);
    }

    private static ItemLike[] harvest() {
        return new ItemLike[]{ModItems.LEMON, ModItems.LIME, ModItems.ORANGE, ModItems.GRAPEFRUIT, ModItems.GRAPE, ModItems.COCONUT,
                ModItems.ANISE, ModItems.MINT_LEAVES, ModItems.LAVENDER, ModItems.COCOA_NIBS};
    }

    static Criterion<InventoryChangeTrigger.TriggerInstance> has(ItemLike... items) {
        return InventoryChangeTrigger.TriggerInstance.hasItems(ItemPredicate.Builder.item().of(items));
    }

    static Criterion<InventoryChangeTrigger.TriggerInstance> has(TagKey<Item> tag) {
        return InventoryChangeTrigger.TriggerInstance.hasItems(ItemPredicate.Builder.item().of(tag));
    }

    private static Criterion<InventoryChangeTrigger.TriggerInstance> leavesDried(int stage) {
        return InventoryChangeTrigger.TriggerInstance.hasItems(ItemPredicate.Builder.item().of(ModItemTags.TOBACCO_LEAVES)
                .hasComponents(DataComponentPredicate.builder().expect(ModDataComponents.DRYING_STAGE.get(), stage).build()));
    }

    private static Criterion<InventoryChangeTrigger.TriggerInstance> leavesFermented() {
        return InventoryChangeTrigger.TriggerInstance.hasItems(ItemPredicate.Builder.item().of(ModItemTags.TOBACCO_LEAVES)
                .hasComponents(DataComponentPredicate.builder().expect(ModDataComponents.FERMENTED.get(), true).build()));
    }

    public static final class Step {
        private final String name;
        private final Map<String, Criterion<?>> criteria = new LinkedHashMap<>();
        private final List<ResourceLocation> recipes = new ArrayList<>();
        @Nullable
        private String advancement;
        private String parent = "root";
        private ItemLike icon;
        private String key;
        private int experience;

        private Step(String name) {
            this.name = name;
        }

        public String name() {
            return name;
        }

        public Map<String, Criterion<?>> criteria() {
            return Collections.unmodifiableMap(criteria);
        }

        public List<ResourceLocation> recipes() {
            return Collections.unmodifiableList(recipes);
        }

        public Optional<String> advancement() {
            return Optional.ofNullable(advancement);
        }

        public String parent() {
            return parent;
        }

        public ItemLike icon() {
            return icon;
        }

        public String key() {
            return key;
        }

        public int experience() {
            return experience;
        }

        private Step on(String criterion, Criterion<?> trigger) {
            criteria.put(criterion, trigger);
            return this;
        }

        private Step teaches(String... paths) {
            for (String path : paths) recipes.add(ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, path));
            return this;
        }

        private Step opens(String advancement, ItemLike icon, String key) {
            this.advancement = advancement;
            this.icon = icon;
            this.key = key;
            return this;
        }

        private Step under(String parent) {
            this.parent = parent;
            return this;
        }

        private Step rewarding(int experience) {
            this.experience = experience;
            return this;
        }
    }

    private static final class Index {
        static final List<Step> STEPS = build();
        static final Map<ResourceLocation, Step> BY_RECIPE = index(STEPS);

        private static Map<ResourceLocation, Step> index(List<Step> steps) {
            Map<ResourceLocation, Step> byRecipe = new HashMap<>();
            for (Step step : steps) {
                for (ResourceLocation recipe : step.recipes) {
                    Step other = byRecipe.put(recipe, step);
                    if (other != null) throw new IllegalStateException(recipe + " is taught by " + other.name + " and " + step.name);
                }
            }
            return Map.copyOf(byRecipe);
        }
    }
}
