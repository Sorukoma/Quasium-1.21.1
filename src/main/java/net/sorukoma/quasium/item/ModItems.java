package net.sorukoma.quasium.item;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.block.ModBlocks;
import net.sorukoma.quasium.component.ModDataComponents;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class ModItems {
    public static final DeferredRegister.Items ITEMS =DeferredRegister.createItems(QuasiumMod.MOD_ID);

    public static final DeferredItem<Item> QUASIUMINGOT = ITEMS.register("quasiumingot",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> RAWQUASIUM = ITEMS.register("rawquasium",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> KAIFOMETER = ITEMS.register("kaifometer",
            () -> new KaifometerItem(new Item.Properties()));
    public static final DeferredItem<Item> PROGRESSION_BOOK = ITEMS.register("progression_book",
            () -> new ProgressionBookItem(new Item.Properties().stacksTo(1)));

    // Трубка кальяна
    public static final DeferredItem<Item> HOOKAH_PIPE = ITEMS.register("hookah_pipe",
            () -> new HookahPipeItem(new Item.Properties().stacksTo(1)));

    // Наспинный кальян
    public static final DeferredItem<Item> BACKPACK_HOOKAH = ITEMS.register("backpack_hookah",
            () -> new BackpackHookahItem(new Item.Properties().stacksTo(1)));

    // Четыре диких сорта табака
    public static final DeferredItem<Item> DIKAR_SEEDS = registerVarietySeeds("dikar", () -> ModBlocks.DIKAR_CROP.get());
    public static final DeferredItem<Item> PESCHANIK_SEEDS = registerVarietySeeds("peschanik", () -> ModBlocks.PESCHANIK_CROP.get());
    public static final DeferredItem<Item> BOLOTNIK_SEEDS = registerVarietySeeds("bolotnik", () -> ModBlocks.BOLOTNIK_CROP.get());
    public static final DeferredItem<Item> LEDYANOY_SEEDS = registerVarietySeeds("ledyanoy", () -> ModBlocks.LEDYANOY_CROP.get());
    public static final DeferredItem<Item> HYBRID_TOBACCO_SEEDS = registerVarietySeeds("hybrid_tobacco", () -> ModBlocks.HYBRID_TOBACCO_CROP.get());

    public static final DeferredItem<Item> DIKAR_LEAVES = registerVarietyLeaves("dikar");
    public static final DeferredItem<Item> PESCHANIK_LEAVES = registerVarietyLeaves("peschanik");
    public static final DeferredItem<Item> BOLOTNIK_LEAVES = registerVarietyLeaves("bolotnik");
    public static final DeferredItem<Item> LEDYANOY_LEAVES = registerVarietyLeaves("ledyanoy");
    public static final DeferredItem<Item> HYBRID_TOBACCO_LEAVES = registerVarietyLeaves("hybrid_tobacco");

    private static DeferredItem<Item> registerVarietySeeds(String name, java.util.function.Supplier<net.minecraft.world.level.block.Block> crop) {
        return ITEMS.register(name + "_seeds", () -> new TobaccoSeedsItem(crop, new Item.Properties()));
    }

    private static DeferredItem<Item> registerVarietyLeaves(String name) {
        return ITEMS.register(name + "_leaves", () -> new LeavesItem(new Item.Properties()));
    }

    // Старые ID оставлены для совместимости с уже созданными мирами.
    public static final DeferredItem<Item> TOBACCO_SEEDS = DIKAR_SEEDS;
    public static final DeferredItem<Item> MAKHORKA_SEEDS = PESCHANIK_SEEDS;
    public static final DeferredItem<Item> TOBACCO_LEAVES = DIKAR_LEAVES;
    public static final DeferredItem<Item> MAKHORKA_LEAVES = PESCHANIK_LEAVES;

    public static final DeferredItem<Item> LEMON_SAPLING = ITEMS.register("lemon_sapling",
            () -> new BonsaiSaplingItem(() -> ModBlocks.BONSAI_POT.get(), "lemon", new Item.Properties()));
    public static final DeferredItem<Item> LIME_SAPLING = ITEMS.register("lime_sapling",
            () -> new BonsaiSaplingItem(() -> ModBlocks.BONSAI_POT.get(), "lime", new Item.Properties()));
    public static final DeferredItem<Item> ORANGE_SAPLING = ITEMS.register("orange_sapling",
            () -> new BonsaiSaplingItem(() -> ModBlocks.BONSAI_POT.get(), "orange", new Item.Properties()));
    public static final DeferredItem<Item> GRAPEFRUIT_SAPLING = ITEMS.register("grapefruit_sapling",
            () -> new BonsaiSaplingItem(() -> ModBlocks.BONSAI_POT.get(), "grapefruit", new Item.Properties()));
    public static final DeferredItem<Item> GRAPE_SAPLING = ITEMS.register("grape_sapling",
            () -> new BonsaiSaplingItem(() -> ModBlocks.BONSAI_POT.get(), "grape", new Item.Properties()));
    public static final DeferredItem<Item> COCONUT_SAPLING = ITEMS.register("coconut_sapling",
            () -> new BonsaiSaplingItem(() -> ModBlocks.BONSAI_POT.get(), "coconut", new Item.Properties()));
    public static final DeferredItem<Item> LEMON = ITEMS.register("lemon", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> LIME = ITEMS.register("lime", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ORANGE = ITEMS.register("orange", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> GRAPEFRUIT = ITEMS.register("grapefruit", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> GRAPE = ITEMS.register("grape", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> COCONUT = ITEMS.register("coconut", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ANISE = ITEMS.register("anise", () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MINT_CROP_SEEDS = ITEMS.register("mint_crop_seeds",
            () -> new EssenceSeedsItem(() -> ModBlocks.MINT_CROP.get(), new Item.Properties()));
    public static final DeferredItem<Item> MINT_LEAVES = ITEMS.register("mint_leaves", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> LAVENDER = ITEMS.register("lavender", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> COCOA_NIBS = ITEMS.register("cocoa_nibs", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ANISE_SEEDS = ITEMS.register("anise_seeds",
            () -> new EssenceSeedsItem(() -> ModBlocks.ANISE_CROP.get(), new Item.Properties()));
    public static final DeferredItem<Item> LAVENDER_SEEDS = ITEMS.register("lavender_seeds",
            () -> new BonsaiSaplingItem(() -> ModBlocks.BONSAI_POT.get(), "lavender", new Item.Properties()));
    public static final DeferredItem<Item> COCOA_CROP_SEEDS = ITEMS.register("cocoa_crop_seeds",
            () -> new BonsaiSaplingItem(() -> ModBlocks.BONSAI_POT.get(), "cocoa", new Item.Properties()));

    // Жир, кислота гаста, глицерин и меласса
    public static final DeferredItem<Item> TALLOW = ITEMS.register("tallow",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> BOTTLED_GHAST_ACID = ITEMS.register("bottled_ghast_acid",
            () -> new Item(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> GLYCERIN_BOTTLE = ITEMS.register("glycerin_bottle",
            () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> MOLASSES_BOTTLE = ITEMS.register("molasses_bottle",
            () -> new MolassesBottleItem(new Item.Properties().stacksTo(1).component(ModDataComponents.MOLASSES_MASS, 5)));

    public static final DeferredItem<Item> ESSENCE_BOTTLE = ITEMS.register("essence_bottle",
            () -> new EssenceBottleItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> BEEF_JERKY = registerJerky("beef_jerky", 6, 0.8f, 2400);
    public static final DeferredItem<Item> PORK_JERKY = registerJerky("pork_jerky", 7, 0.9f, 2400);
    public static final DeferredItem<Item> CHICKEN_JERKY = registerJerky("chicken_jerky", 5, 0.7f, 2000);
    public static final DeferredItem<Item> MUTTON_JERKY = registerJerky("mutton_jerky", 6, 0.8f, 2200);
    public static final DeferredItem<Item> RABBIT_JERKY = registerJerky("rabbit_jerky", 5, 0.7f, 2000);
    public static final DeferredItem<Item> COD_JERKY = registerJerky("cod_jerky", 5, 0.7f, 1800);
    public static final DeferredItem<Item> SALMON_JERKY = registerJerky("salmon_jerky", 6, 0.8f, 2000);

    public static final DeferredItem<Item> QUASIUM_HELMET = ITEMS.register("quasium_helmet",
            () -> new ArmorItem(ModArmorMaterials.QUASIUM, ArmorItem.Type.HELMET, new Item.Properties().durability(330)));
    public static final DeferredItem<Item> QUASIUM_CHESTPLATE = ITEMS.register("quasium_chestplate",
            () -> new ArmorItem(ModArmorMaterials.QUASIUM, ArmorItem.Type.CHESTPLATE, new Item.Properties().durability(480)));
    public static final DeferredItem<Item> QUASIUM_LEGGINGS = ITEMS.register("quasium_leggings",
            () -> new ArmorItem(ModArmorMaterials.QUASIUM, ArmorItem.Type.LEGGINGS, new Item.Properties().durability(450)));
    public static final DeferredItem<Item> QUASIUM_BOOTS = ITEMS.register("quasium_boots",
            () -> new ArmorItem(ModArmorMaterials.QUASIUM, ArmorItem.Type.BOOTS, new Item.Properties().durability(390)));

    // Новые предметы табака
    public static final DeferredItem<Item> MINT_TOBACCO = ITEMS.register("mint_tobacco",
            () -> new TobaccoItem("mint", new Item.Properties().stacksTo(1).component(ModDataComponents.TOBACCO_WEIGHT, 30)));
    public static final DeferredItem<Item> BERRY_TOBACCO = ITEMS.register("berry_tobacco",
            () -> new TobaccoItem("berry", new Item.Properties().stacksTo(1).component(ModDataComponents.TOBACCO_WEIGHT, 30)));
    public static final DeferredItem<Item> LEMON_TOBACCO = registerFlavoredTobacco("lemon");
    public static final DeferredItem<Item> LIME_TOBACCO = registerFlavoredTobacco("lime");
    public static final DeferredItem<Item> ORANGE_TOBACCO = registerFlavoredTobacco("orange");
    public static final DeferredItem<Item> GRAPEFRUIT_TOBACCO = registerFlavoredTobacco("grapefruit");
    public static final DeferredItem<Item> APPLE_TOBACCO = registerFlavoredTobacco("apple");
    public static final DeferredItem<Item> WATERMELON_TOBACCO = registerFlavoredTobacco("watermelon");
    public static final DeferredItem<Item> GRAPE_TOBACCO = registerFlavoredTobacco("grape");
    public static final DeferredItem<Item> CHERRY_TOBACCO = registerFlavoredTobacco("cherry");
    public static final DeferredItem<Item> VANILLA_TOBACCO = registerFlavoredTobacco("vanilla");
    public static final DeferredItem<Item> COFFEE_TOBACCO = registerFlavoredTobacco("coffee");
    public static final DeferredItem<Item> CINNAMON_TOBACCO = registerFlavoredTobacco("cinnamon");
    public static final DeferredItem<Item> COCONUT_TOBACCO = registerFlavoredTobacco("coconut");
    public static final DeferredItem<Item> ROSE_TOBACCO = registerFlavoredTobacco("rose");
    public static final DeferredItem<Item> ANISE_TOBACCO = registerFlavoredTobacco("anise");

    private static DeferredItem<Item> registerFlavoredTobacco(String flavor) {
        return ITEMS.register(flavor + "_tobacco",
                () -> new TobaccoItem(flavor, new Item.Properties().stacksTo(1)
                        .component(ModDataComponents.TOBACCO_WEIGHT, 30)));
    }

    // Самозамешанный табак
    public static final DeferredItem<Item> SELF_MIXED_TOBACCO = ITEMS.register("self_mixed_tobacco",
            () -> new SelfMixedTobaccoItem(new Item.Properties().stacksTo(1)));

    // Незеритовый молот
    public static final DeferredItem<Item> NETHERITE_HAMMER = ITEMS.register("netherite_hammer",
            () -> new NetheriteHammerItem(new Item.Properties()));

    public static final DeferredItem<Item> UNFIRED_HOOKAH_BOWL = ITEMS.register("unfired_hookah_bowl",
            () -> new Item(new Item.Properties()));

    // Чаша кальяна (Hookah Bowl) — контейнер для забивки табака
    public static final DeferredItem<Item> HOOKAH_BOWL = ITEMS.register("hookah_bowl",
            () -> new HookahBowlItem(new Item.Properties().stacksTo(1)
                    .component(ModDataComponents.HOOKAH_BOWL_CONTENTS,
                            new ModDataComponents.HookahBowlContents(List.of(), Map.of(), Set.of(), false))));

    // 1 грамм обычного табака — отщипывается от пачки
    public static final DeferredItem<Item> ONE_GRAM_TOBACCO = ITEMS.register("one_gram_tobacco",
            () -> new OneGramTobaccoItem(new Item.Properties().stacksTo(64)));

    // 1 грамм самозамеса — отщипывается от SelfMixedTobaccoItem
    public static final DeferredItem<Item> ONE_GRAM_SELF_MIXED_TOBACCO = ITEMS.register("one_gram_self_mixed_tobacco",
            () -> new OneGramSelfMixedTobaccoItem(new Item.Properties().stacksTo(64)));

    // Сгоревший табак — остаток после курения, нужно вытащить из чаши
    public static final DeferredItem<Item> BURNED_TOBACCO = ITEMS.register("burned_tobacco",
            () -> new BurnedTobaccoItem(new Item.Properties().stacksTo(64)));

    // Сырой брикет — нарезается в камнерезе на угли разных размеров
    public static final DeferredItem<Item> RAW_BRIQUETTE = ITEMS.register("raw_briquette",
            () -> new Item(new Item.Properties().stacksTo(64)));

    // Угли разных размеров
    public static final DeferredItem<Item> CHARCOAL_22 = ITEMS.register("charcoal_22",
            () -> new CharcoalItem(ModDataComponents.CharcoalSize.MM_22, new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> CHARCOAL_25 = ITEMS.register("charcoal_25",
            () -> new CharcoalItem(ModDataComponents.CharcoalSize.MM_25, new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> CHARCOAL_27 = ITEMS.register("charcoal_27",
            () -> new CharcoalItem(ModDataComponents.CharcoalSize.MM_27, new Item.Properties().stacksTo(64)));

    // Аксессуары кальяна
    public static final DeferredItem<Item> HOOKAH_DIFFUSER = ITEMS.register("hookah_diffuser",
            () -> new Item(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> HOOKAH_COOLER = ITEMS.register("hookah_cooler",
            () -> new Item(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> HOOKAH_ICE_CHAMBER = ITEMS.register("hookah_ice_chamber",
            () -> new Item(new Item.Properties().stacksTo(1)));

    // Шахты модульного кальяна (5 вариантов цвета)
    public static final DeferredItem<Item> MODULAR_SHAFT_WHITE = registerShaft("white", net.sorukoma.quasium.hookah.HookahShaftVariant.WHITE);
    public static final DeferredItem<Item> MODULAR_SHAFT_BLACK = registerShaft("black", net.sorukoma.quasium.hookah.HookahShaftVariant.BLACK);
    public static final DeferredItem<Item> MODULAR_SHAFT_RED = registerShaft("red", net.sorukoma.quasium.hookah.HookahShaftVariant.RED);
    public static final DeferredItem<Item> MODULAR_SHAFT_BLUE = registerShaft("blue", net.sorukoma.quasium.hookah.HookahShaftVariant.BLUE);
    public static final DeferredItem<Item> MODULAR_SHAFT_AMBER = registerShaft("amber", net.sorukoma.quasium.hookah.HookahShaftVariant.AMBER);

    private static DeferredItem<Item> registerShaft(String color, net.sorukoma.quasium.hookah.HookahShaftVariant variant) {
        return ITEMS.register("modular_shaft_" + color,
                () -> new HookahShaftItem(variant, new Item.Properties().stacksTo(16)));
    }

    /** Возвращает ItemStack шахты для заданного варианта (для дропа при разрушении блока). */
    public static ItemStack shaftItemFor(net.sorukoma.quasium.hookah.HookahShaftVariant variant) {
        return switch (variant) {
            case WHITE -> new ItemStack(MODULAR_SHAFT_WHITE.get());
            case BLACK -> new ItemStack(MODULAR_SHAFT_BLACK.get());
            case RED -> new ItemStack(MODULAR_SHAFT_RED.get());
            case BLUE -> new ItemStack(MODULAR_SHAFT_BLUE.get());
            case AMBER -> new ItemStack(MODULAR_SHAFT_AMBER.get());
        };
    }

    // Каллауд
    public static final DeferredItem<Item> CALLOUD = ITEMS.register("calloud",
            () -> new CalloudItem(new Item.Properties().stacksTo(1)
                    .component(ModDataComponents.CALLOUD_CONTENTS, new ModDataComponents.CalloudData(List.of(), List.of()))));

    private static DeferredItem<Item> registerJerky(String name, int nutrition, float saturation, int dryingTime) {
        return ITEMS.register(name, () -> new JerkyItem(dryingTime, new Item.Properties().food(
                new FoodProperties.Builder().nutrition(nutrition).saturationModifier(saturation).build())));
    }

    public static void register(IEventBus eventBus){
        ITEMS.register(eventBus);
    }
}
