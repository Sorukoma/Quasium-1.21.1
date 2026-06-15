package net.sorukoma.quasium.item;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
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

    // Семена табака и махорки
    public static final DeferredItem<Item> TOBACCO_SEEDS = ITEMS.register("tobacco_seeds",
            () -> new TobaccoSeedsItem(() -> ModBlocks.TOBACCO_CROP.get(), new Item.Properties()));
    public static final DeferredItem<Item> MAKHORKA_SEEDS = ITEMS.register("makhorka_seeds",
            () -> new TobaccoSeedsItem(() -> ModBlocks.MAKHORKA_CROP.get(), new Item.Properties()));

    // Листья табака и махорки
    public static final DeferredItem<Item> TOBACCO_LEAVES = ITEMS.register("tobacco_leaves",
            () -> new LeavesItem(new Item.Properties()));
    public static final DeferredItem<Item> MAKHORKA_LEAVES = ITEMS.register("makhorka_leaves",
            () -> new LeavesItem(new Item.Properties()));

    // Жир, кислота гаста, глицерин и меласса
    public static final DeferredItem<Item> TALLOW = ITEMS.register("tallow",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> BOTTLED_GHAST_ACID = ITEMS.register("bottled_ghast_acid",
            () -> new Item(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> GLYCERIN_BOTTLE = ITEMS.register("glycerin_bottle",
            () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> MOLASSES_BOTTLE = ITEMS.register("molasses_bottle",
            () -> new MolassesBottleItem(new Item.Properties().stacksTo(1).component(ModDataComponents.MOLASSES_MASS, 5)));

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
    public static final DeferredItem<Item> CITRUS_TOBACCO = ITEMS.register("citrus_tobacco",
            () -> new TobaccoItem("citrus", new Item.Properties().stacksTo(1).component(ModDataComponents.TOBACCO_WEIGHT, 30)));
    public static final DeferredItem<Item> BERRY_TOBACCO = ITEMS.register("berry_tobacco",
            () -> new TobaccoItem("berry", new Item.Properties().stacksTo(1).component(ModDataComponents.TOBACCO_WEIGHT, 30)));

    // Новый предмет: мисочка для смешивания
    public static final DeferredItem<Item> MIXING_BOWL = ITEMS.register("mixing_bowl",
            () -> new MixingBowlItem(new Item.Properties().stacksTo(1)));

    // Новый предмет: смешанный табак
    public static final DeferredItem<Item> MIXED_TOBACCO = ITEMS.register("mixed_tobacco",
            () -> new MixedTobaccoItem(new Item.Properties().stacksTo(1)
                    .component(ModDataComponents.TOBACCO_WEIGHT, 0)
                    .component(ModDataComponents.TOBACCO_MIX, new ModDataComponents.TobaccoMixData(Map.of()))));

    // Самозамешанный табак
    public static final DeferredItem<Item> SELF_MIXED_TOBACCO = ITEMS.register("self_mixed_tobacco",
            () -> new SelfMixedTobaccoItem(new Item.Properties().stacksTo(1)));

    // Незеритовый молот
    public static final DeferredItem<Item> NETHERITE_HAMMER = ITEMS.register("netherite_hammer",
            () -> new NetheriteHammerItem(new Item.Properties()));

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

    // Угли разных размеров
    public static final DeferredItem<Item> CHARCOAL_22 = ITEMS.register("charcoal_22",
            () -> new CharcoalItem(ModDataComponents.CharcoalSize.MM_22, new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> CHARCOAL_25 = ITEMS.register("charcoal_25",
            () -> new CharcoalItem(ModDataComponents.CharcoalSize.MM_25, new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> CHARCOAL_27 = ITEMS.register("charcoal_27",
            () -> new CharcoalItem(ModDataComponents.CharcoalSize.MM_27, new Item.Properties().stacksTo(64)));

    // Каллауд
    public static final DeferredItem<Item> CALLOUD = ITEMS.register("calloud",
            () -> new CalloudItem(new Item.Properties().stacksTo(1)
                    .component(ModDataComponents.CALLOUD_CONTENTS, new ModDataComponents.CalloudData(List.of(), List.of()))));

    public static void register(IEventBus eventBus){
        ITEMS.register(eventBus);
    }
}