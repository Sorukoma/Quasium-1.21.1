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

import java.util.List;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(QuasiumMod.MOD_ID);

    public static final DeferredBlock<Block> HOOKAH_BLOCK = BLOCKS.register("hookah_block",
            () -> new HookahBlock());

    // Кусты табака
    public static final DeferredBlock<Block> TOBACCO_CROP = BLOCKS.register("tobacco_crop",
            () -> new RegularTobaccoCropBlock(
                    () -> ModItems.TOBACCO_SEEDS.get(),
                    () -> ModItems.TOBACCO_LEAVES.get(),
                    () -> ModItems.MAKHORKA_SEEDS.get(),
                    BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT)));

    public static final DeferredBlock<Block> MAKHORKA_CROP = BLOCKS.register("makhorka_crop",
            () -> new MakhorkaCropBlock(
                    () -> ModItems.MAKHORKA_SEEDS.get(),
                    () -> ModItems.MAKHORKA_LEAVES.get(),
                    BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT)));

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
                    .strength(5.0F, 1200.0F)
                    .sound(net.minecraft.world.level.block.SoundType.ANVIL)
                    .noOcclusion()));

    // Печка для кальяна
    public static final DeferredBlock<Block> HEATER = BLOCKS.register("heater",
            () -> new HeaterBlock(BlockBehaviour.Properties.of()
                    .strength(3.5F, 1200.0F)
                    .sound(net.minecraft.world.level.block.SoundType.METAL)));

    // BlockItem для кальян (кусты табака не имеют BlockItem — посадка через семена)
    public static final DeferredItem<BlockItem> HOOKAH_BLOCK_ITEM = ModItems.ITEMS.register("hookah_block",
            () -> new BlockItem(HOOKAH_BLOCK.get(), new Item.Properties()));

    // BlockItem для сушильной полки
    public static final DeferredItem<BlockItem> DRYING_SHELF_ITEM = ModItems.ITEMS.register("drying_shelf",
            () -> new BlockItem(DRYING_SHELF.get(), new Item.Properties()));

    // BlockItem для бочки ферментации
    public static final DeferredItem<BlockItem> FERMENTATION_BARREL_ITEM = ModItems.ITEMS.register("fermentation_barrel",
            () -> new BlockItem(FERMENTATION_BARREL.get(), new Item.Properties()));

    // BlockItem для миски пропитывания
    public static final DeferredItem<BlockItem> SOAKING_BOWL_ITEM = ModItems.ITEMS.register("soaking_bowl",
            () -> new BlockItem(SOAKING_BOWL.get(), new Item.Properties()));

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

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
