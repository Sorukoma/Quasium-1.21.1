package net.sorukoma.quasium.block;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.block.entity.AromaLampBlockEntity;
import net.sorukoma.quasium.block.entity.LowTableBlockEntity;
import net.sorukoma.quasium.entity.SeatEntity;

public final class LoungeBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(QuasiumMod.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(QuasiumMod.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, QuasiumMod.MOD_ID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, QuasiumMod.MOD_ID);

    public static final DeferredBlock<LoungeCushionBlock> LOUNGE_CUSHION = BLOCKS.registerBlock("lounge_cushion",
            LoungeCushionBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
                    .strength(0.8f).sound(SoundType.WOOL).noOcclusion().ignitedByLava());

    public static final DeferredBlock<LowTableBlock> LOW_TABLE = BLOCKS.registerBlock("low_table",
            LowTableBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN)
                    .strength(2f, 3f).sound(SoundType.WOOD).noOcclusion().ignitedByLava());

    public static final DeferredBlock<AromaLampBlock> AROMA_LAMP = BLOCKS.registerBlock("aroma_lamp",
            AromaLampBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_WHITE)
                    .strength(1f).sound(SoundType.DECORATED_POT).noOcclusion()
                    .lightLevel(state -> state.getValue(AromaLampBlock.LIT) ? AromaLampBlock.LIT_LIGHT : 0));

    public static final DeferredBlock<GlowingBulbBlock> GLOWING_BULB_WHITE = bulb("white");
    public static final DeferredBlock<GlowingBulbBlock> GLOWING_BULB_BLACK = bulb("black");
    public static final DeferredBlock<GlowingBulbBlock> GLOWING_BULB_RED = bulb("red");
    public static final DeferredBlock<GlowingBulbBlock> GLOWING_BULB_BLUE = bulb("blue");
    public static final DeferredBlock<GlowingBulbBlock> GLOWING_BULB_AMBER = bulb("amber");

    public static final DeferredItem<BlockItem> LOUNGE_CUSHION_ITEM = ITEMS.registerSimpleBlockItem(LOUNGE_CUSHION);
    public static final DeferredItem<BlockItem> LOW_TABLE_ITEM = ITEMS.registerSimpleBlockItem(LOW_TABLE);
    public static final DeferredItem<BlockItem> AROMA_LAMP_ITEM = ITEMS.registerSimpleBlockItem(AROMA_LAMP);
    public static final DeferredItem<BlockItem> GLOWING_BULB_WHITE_ITEM = ITEMS.registerSimpleBlockItem(GLOWING_BULB_WHITE);
    public static final DeferredItem<BlockItem> GLOWING_BULB_BLACK_ITEM = ITEMS.registerSimpleBlockItem(GLOWING_BULB_BLACK);
    public static final DeferredItem<BlockItem> GLOWING_BULB_RED_ITEM = ITEMS.registerSimpleBlockItem(GLOWING_BULB_RED);
    public static final DeferredItem<BlockItem> GLOWING_BULB_BLUE_ITEM = ITEMS.registerSimpleBlockItem(GLOWING_BULB_BLUE);
    public static final DeferredItem<BlockItem> GLOWING_BULB_AMBER_ITEM = ITEMS.registerSimpleBlockItem(GLOWING_BULB_AMBER);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LowTableBlockEntity>> LOW_TABLE_BE =
            BLOCK_ENTITIES.register("low_table_be",
                    () -> BlockEntityType.Builder.of(LowTableBlockEntity::new, LOW_TABLE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AromaLampBlockEntity>> AROMA_LAMP_BE =
            BLOCK_ENTITIES.register("aroma_lamp_be",
                    () -> BlockEntityType.Builder.of(AromaLampBlockEntity::new, AROMA_LAMP.get()).build(null));

    public static final DeferredHolder<EntityType<?>, EntityType<SeatEntity>> SEAT = ENTITY_TYPES.register("lounge_seat",
            () -> EntityType.Builder.<SeatEntity>of(SeatEntity::new, MobCategory.MISC)
                    .sized(0.5f, 0.1f)
                    .noSave()
                    .noSummon()
                    .clientTrackingRange(10)
                    .updateInterval(20)
                    .build("lounge_seat"));

    private LoungeBlocks() {
    }

    private static DeferredBlock<GlowingBulbBlock> bulb(String color) {
        return BLOCKS.registerBlock("glowing_bulb_" + color, GlowingBulbBlock::new,
                BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).lightLevel(state -> GlowingBulbBlock.LIGHT).forceSolidOn());
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);
        BLOCK_ENTITIES.register(eventBus);
        ENTITY_TYPES.register(eventBus);
    }
}
