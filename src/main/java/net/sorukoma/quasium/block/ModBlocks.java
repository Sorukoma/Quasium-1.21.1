package net.sorukoma.quasium.block;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.item.ModItems;


public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(QuasiumMod.MOD_ID);

    public static final DeferredBlock<Block> HOOKAH_BLOCK = BLOCKS.register("hookah_block",
            () -> new HookahBlock());

    // Модульный кальян — 5 вариантов колбы (база), различаются только цветом/рендером.
    public static final DeferredBlock<Block> MODULAR_HOOKAH_WHITE = registerModularHookah("white");
    public static final DeferredBlock<Block> MODULAR_HOOKAH_BLACK = registerModularHookah("black");
    public static final DeferredBlock<Block> MODULAR_HOOKAH_RED = registerModularHookah("red");
    public static final DeferredBlock<Block> MODULAR_HOOKAH_BLUE = registerModularHookah("blue");
    public static final DeferredBlock<Block> MODULAR_HOOKAH_AMBER = registerModularHookah("amber");

    private static DeferredBlock<Block> registerModularHookah(String color) {
        return BLOCKS.register("modular_hookah_" + color, () -> new ModularHookahBlock(
                BlockBehaviour.Properties.ofFullCopy(Blocks.BREWING_STAND)
                        .sound(net.minecraft.world.level.block.SoundType.GLASS)
                        .noOcclusion()));
    }

    // Кусты четырёх диких сортов
    public static final DeferredBlock<Block> DIKAR_CROP = registerTobaccoCrop("dikar", () -> ModItems.DIKAR_SEEDS.get(), () -> ModItems.DIKAR_LEAVES.get());
    public static final DeferredBlock<Block> PESCHANIK_CROP = registerTobaccoCrop("peschanik", () -> ModItems.PESCHANIK_SEEDS.get(), () -> ModItems.PESCHANIK_LEAVES.get());
    public static final DeferredBlock<Block> BOLOTNIK_CROP = registerTobaccoCrop("bolotnik", () -> ModItems.BOLOTNIK_SEEDS.get(), () -> ModItems.BOLOTNIK_LEAVES.get());
    public static final DeferredBlock<Block> LEDYANOY_CROP = registerTobaccoCrop("ledyanoy", () -> ModItems.LEDYANOY_SEEDS.get(), () -> ModItems.LEDYANOY_LEAVES.get());
    public static final DeferredBlock<Block> HYBRID_TOBACCO_CROP = registerTobaccoCrop("hybrid_tobacco", "hybrid", () -> ModItems.HYBRID_TOBACCO_SEEDS.get(), () -> ModItems.HYBRID_TOBACCO_LEAVES.get());

    public static final DeferredBlock<Block> TOBACCO_CROP = DIKAR_CROP;
    public static final DeferredBlock<Block> MAKHORKA_CROP = PESCHANIK_CROP;

    private static DeferredBlock<Block> registerTobaccoCrop(String name,
                                                               java.util.function.Supplier<Item> seeds,
                                                               java.util.function.Supplier<Item> leaves) {
        return registerTobaccoCrop(name, name, seeds, leaves);
    }

    private static DeferredBlock<Block> registerTobaccoCrop(String name,
                                                               String variety,
                                                               java.util.function.Supplier<Item> seeds,
                                                               java.util.function.Supplier<Item> leaves) {
        return BLOCKS.register(name + "_crop", () -> new VarietyTobaccoCropBlock(
                seeds, leaves, variety, BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT)));
    }

    public static final DeferredBlock<Block> WOODEN_TOBACCO_POLE = registerPole("wooden_tobacco_pole", 0.70f, 0.05f);
    public static final DeferredBlock<Block> REINFORCED_TOBACCO_POLE = registerPole("reinforced_tobacco_pole", 0.85f, 0.08f);
    public static final DeferredBlock<Block> GOLDEN_TOBACCO_POLE = registerPole("golden_tobacco_pole", 0.95f, 0.13f);
    public static final DeferredBlock<Block> DIAMOND_TOBACCO_POLE = registerPole("diamond_tobacco_pole", 1.00f, 0.15f);

    private static DeferredBlock<Block> registerPole(String name, float success, float mutation) {
        return BLOCKS.register(name, () -> new TobaccoPoleBlock(success, mutation,
                BlockBehaviour.Properties.ofFullCopy(Blocks.BAMBOO).noOcclusion()
                        .offsetType(BlockBehaviour.OffsetType.NONE)));
    }
    public static final DeferredBlock<Block> MINT_CROP = registerEssenceCrop("mint_crop", ModItems.MINT_CROP_SEEDS, ModItems.MINT_LEAVES);
    public static final DeferredBlock<Block> ANISE_CROP = registerEssenceCrop("anise_crop", ModItems.ANISE_SEEDS, ModItems.ANISE);

    public static final DeferredBlock<Block> BONSAI_POT = BLOCKS.register("bonsai_pot",
            () -> new BonsaiPotBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.FLOWER_POT).noOcclusion()));

    // Сушильная полка
    public static final DeferredBlock<Block> DRYING_SHELF = BLOCKS.register("drying_shelf",
            () -> new DryingShelfBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BARREL)
                    .noOcclusion()));

    // Бочка для ферментации
    public static final DeferredBlock<Block> FERMENTATION_BARREL = BLOCKS.register("fermentation_barrel",
            () -> new FermentationBarrelBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BARREL)));

    // Миска для пропитывания
    public static final DeferredBlock<Block> SOAKING_BOWL = BLOCKS.register("soaking_bowl",
            () -> new SoakingBowlBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BREWING_STAND)));

    // Пресс для экстрактов
    public static final DeferredBlock<Block> ESSENCE_PRESS = BLOCKS.register("essence_press",
            () -> new EssencePressBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BARREL)));

    // Руда квазиума (дипслейт-вариант)
    public static final DeferredBlock<Block> DEEPSLATE_QUASIUM_ORE = BLOCKS.register("deepslate_quasium_ore",
            () -> new QuasiumOreBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_IRON_ORE)));

    // Не сушенный квазиумовый блок
    public static final DeferredBlock<Block> UNDRIED_QUASIUM_BLOCK = BLOCKS.register("undried_quasium_block",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)));

    // Квазиумовый блок (просушенный)
    public static final DeferredBlock<Block> QUASIUM_BLOCK = BLOCKS.register("quasium_block",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)));

    // Квазиумовая наковальня
    public static final DeferredBlock<Block> QUASIUM_ANVIL = BLOCKS.register("quasium_anvil",
            () -> new QuasiumAnvilBlock(BlockBehaviour.Properties.of()
                    .requiresCorrectToolForDrops()
                    .strength(5.0F, 1200.0F)
                    .sound(net.minecraft.world.level.block.SoundType.ANVIL)
                    .noOcclusion()));

    // Печка для кальяна
    public static final DeferredBlock<Block> HEATER = BLOCKS.register("heater",
            () -> new HeaterBlock(BlockBehaviour.Properties.of()
                    .requiresCorrectToolForDrops()
                    .strength(3.5F, 1200.0F)
                    .sound(net.minecraft.world.level.block.SoundType.METAL)));

    public static final DeferredItem<BlockItem> WOODEN_TOBACCO_POLE_ITEM = registerPoleItem("wooden_tobacco_pole", WOODEN_TOBACCO_POLE);
    public static final DeferredItem<BlockItem> REINFORCED_TOBACCO_POLE_ITEM = registerPoleItem("reinforced_tobacco_pole", REINFORCED_TOBACCO_POLE);
    public static final DeferredItem<BlockItem> GOLDEN_TOBACCO_POLE_ITEM = registerPoleItem("golden_tobacco_pole", GOLDEN_TOBACCO_POLE);
    public static final DeferredItem<BlockItem> DIAMOND_TOBACCO_POLE_ITEM = registerPoleItem("diamond_tobacco_pole", DIAMOND_TOBACCO_POLE);

    private static DeferredItem<BlockItem> registerPoleItem(String name, DeferredBlock<Block> block) {
        return ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }
    public static final DeferredItem<BlockItem> HOOKAH_BLOCK_ITEM = ModItems.ITEMS.register("hookah_block",
            () -> new BlockItem(HOOKAH_BLOCK.get(), new Item.Properties()));

    // Предметы-колбы модульного кальяна (BlockItem с вариантом цвета).
    public static final DeferredItem<BlockItem> MODULAR_HOOKAH_WHITE_ITEM = registerBulbItem("white", MODULAR_HOOKAH_WHITE, net.sorukoma.quasium.hookah.HookahBulbVariant.WHITE);
    public static final DeferredItem<BlockItem> MODULAR_HOOKAH_BLACK_ITEM = registerBulbItem("black", MODULAR_HOOKAH_BLACK, net.sorukoma.quasium.hookah.HookahBulbVariant.BLACK);
    public static final DeferredItem<BlockItem> MODULAR_HOOKAH_RED_ITEM = registerBulbItem("red", MODULAR_HOOKAH_RED, net.sorukoma.quasium.hookah.HookahBulbVariant.RED);
    public static final DeferredItem<BlockItem> MODULAR_HOOKAH_BLUE_ITEM = registerBulbItem("blue", MODULAR_HOOKAH_BLUE, net.sorukoma.quasium.hookah.HookahBulbVariant.BLUE);
    public static final DeferredItem<BlockItem> MODULAR_HOOKAH_AMBER_ITEM = registerBulbItem("amber", MODULAR_HOOKAH_AMBER, net.sorukoma.quasium.hookah.HookahBulbVariant.AMBER);

    private static DeferredItem<BlockItem> registerBulbItem(String color, DeferredBlock<Block> block, net.sorukoma.quasium.hookah.HookahBulbVariant variant) {
        return ModItems.ITEMS.register("modular_hookah_" + color,
                () -> new net.sorukoma.quasium.item.HookahBulbItem(block.get(), variant, new Item.Properties()));
    }

    public static final DeferredItem<BlockItem> BONSAI_POT_ITEM = ModItems.ITEMS.register("bonsai_pot",
            () -> new BlockItem(BONSAI_POT.get(), new Item.Properties()));

    // BlockItem для сушильной полки
    public static final DeferredItem<BlockItem> DRYING_SHELF_ITEM = ModItems.ITEMS.register("drying_shelf",
            () -> new BlockItem(DRYING_SHELF.get(), new Item.Properties()));

    // BlockItem для бочки ферментации
    public static final DeferredItem<BlockItem> FERMENTATION_BARREL_ITEM = ModItems.ITEMS.register("fermentation_barrel",
            () -> new BlockItem(FERMENTATION_BARREL.get(), new Item.Properties()));

    // BlockItem для миски пропитывания
    public static final DeferredItem<BlockItem> SOAKING_BOWL_ITEM = ModItems.ITEMS.register("soaking_bowl",
            () -> new BlockItem(SOAKING_BOWL.get(), new Item.Properties()));

    // BlockItem для пресса экстрактов
    public static final DeferredItem<BlockItem> ESSENCE_PRESS_ITEM = ModItems.ITEMS.register("essence_press",
            () -> new BlockItem(ESSENCE_PRESS.get(), new Item.Properties()));

    // BlockItem для руды квазиума
    public static final DeferredItem<BlockItem> DEEPSLATE_QUASIUM_ORE_ITEM = ModItems.ITEMS.register("deepslate_quasium_ore",
            () -> new BlockItem(DEEPSLATE_QUASIUM_ORE.get(), new Item.Properties()));

    // BlockItem для не сушенного квазиумового блока
    public static final DeferredItem<BlockItem> UNDRIED_QUASIUM_BLOCK_ITEM = ModItems.ITEMS.register("undried_quasium_block",
            () -> new BlockItem(UNDRIED_QUASIUM_BLOCK.get(), new Item.Properties()));

    // BlockItem для квазиумового блока
    public static final DeferredItem<BlockItem> QUASIUM_BLOCK_ITEM = ModItems.ITEMS.register("quasium_block",
            () -> new BlockItem(QUASIUM_BLOCK.get(), new Item.Properties()));

    // BlockItem для квазиумовой наковальни
    public static final DeferredItem<BlockItem> QUASIUM_ANVIL_ITEM = ModItems.ITEMS.register("quasium_anvil",
            () -> new BlockItem(QUASIUM_ANVIL.get(), new Item.Properties()));

    // BlockItem для печки
    public static final DeferredItem<BlockItem> HEATER_ITEM = ModItems.ITEMS.register("heater",
            () -> new BlockItem(HEATER.get(), new Item.Properties()));

    private static DeferredBlock<Block> registerEssenceCrop(String name, DeferredItem<Item> seeds, DeferredItem<Item> output) {
        return BLOCKS.register(name, () -> new EssenceCropBlock(
                seeds::get, output::get, BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT)));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
