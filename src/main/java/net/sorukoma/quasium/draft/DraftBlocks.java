package net.sorukoma.quasium.draft;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.sorukoma.quasium.QuasiumMod;

public final class DraftBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(QuasiumMod.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(QuasiumMod.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, QuasiumMod.MOD_ID);

    public static final DeferredBlock<SmokePipeBlock> CLAY_PIPE = BLOCKS.register("clay_pipe",
            () -> new SmokePipeBlock(PipeTier.CLAY, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE)
                    .instrument(NoteBlockInstrument.BASEDRUM).strength(1f).noOcclusion().forceSolidOn()));
    public static final DeferredBlock<SmokePipeBlock> COPPER_PIPE = BLOCKS.register("copper_pipe",
            () -> new SmokePipeBlock(PipeTier.COPPER, metal(MapColor.COLOR_ORANGE, 2.5f)));
    public static final DeferredBlock<SmokePipeBlock> GLASS_PIPE = BLOCKS.register("glass_pipe",
            () -> new SmokePipeBlock(PipeTier.GLASS, BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).strength(0.6f).noOcclusion().forceSolidOn()));
    public static final DeferredBlock<SmokePipeBlock> QUASIUM_PIPE = BLOCKS.register("quasium_pipe",
            () -> new SmokePipeBlock(PipeTier.QUASIUM, metal(MapColor.COLOR_PURPLE, 4f)));
    public static final DeferredBlock<SmokeValveBlock> SMOKE_VALVE = BLOCKS.register("smoke_valve",
            () -> new SmokeValveBlock(PipeTier.QUASIUM, metal(MapColor.COLOR_ORANGE, 2.5f)));
    public static final DeferredBlock<SmokeFilterBlock> NOTE_FILTER = BLOCKS.register("note_filter",
            () -> new SmokeFilterBlock(PipeTier.QUASIUM, metal(MapColor.COLOR_ORANGE, 2.5f)));
    public static final DeferredBlock<SmokeSplitterBlock> SMOKE_SPLITTER = BLOCKS.register("smoke_splitter",
            () -> new SmokeSplitterBlock(PipeTier.QUASIUM, metal(MapColor.COLOR_ORANGE, 2.5f)));
    public static final DeferredBlock<SmokeSorterBlock> SMOKE_SORTER = BLOCKS.register("smoke_sorter",
            () -> new SmokeSorterBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).strength(1f).sound(SoundType.GLASS)));
    public static final DeferredBlock<AutoDistillerBlock> AUTO_DISTILLER = BLOCKS.register("auto_distiller",
            () -> new AutoDistillerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(3f, 6f)
                    .sound(SoundType.COPPER).requiresCorrectToolForDrops()));
    public static final DeferredBlock<BellowsBlock> BELLOWS = BLOCKS.register("bellows",
            () -> new BellowsBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.5f).sound(SoundType.WOOD).ignitedByLava()
                    .noOcclusion()));

    public static final DeferredItem<BlockItem> CLAY_PIPE_ITEM = ITEMS.registerSimpleBlockItem(CLAY_PIPE);
    public static final DeferredItem<BlockItem> COPPER_PIPE_ITEM = ITEMS.registerSimpleBlockItem(COPPER_PIPE);
    public static final DeferredItem<BlockItem> GLASS_PIPE_ITEM = ITEMS.registerSimpleBlockItem(GLASS_PIPE);
    public static final DeferredItem<BlockItem> QUASIUM_PIPE_ITEM = ITEMS.registerSimpleBlockItem(QUASIUM_PIPE);
    public static final DeferredItem<BlockItem> SMOKE_VALVE_ITEM = ITEMS.registerSimpleBlockItem(SMOKE_VALVE);
    public static final DeferredItem<BlockItem> NOTE_FILTER_ITEM = ITEMS.registerSimpleBlockItem(NOTE_FILTER);
    public static final DeferredItem<BlockItem> SMOKE_SPLITTER_ITEM = ITEMS.registerSimpleBlockItem(SMOKE_SPLITTER);
    public static final DeferredItem<BlockItem> SMOKE_SORTER_ITEM = ITEMS.registerSimpleBlockItem(SMOKE_SORTER);
    public static final DeferredItem<BlockItem> AUTO_DISTILLER_ITEM = ITEMS.registerSimpleBlockItem(AUTO_DISTILLER);
    public static final DeferredItem<BlockItem> BELLOWS_ITEM = ITEMS.registerSimpleBlockItem(BELLOWS);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SmokeSorterBlockEntity>> SMOKE_SORTER_BE =
            BLOCK_ENTITIES.register("smoke_sorter_be",
                    () -> BlockEntityType.Builder.of(SmokeSorterBlockEntity::new, SMOKE_SORTER.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BellowsBlockEntity>> BELLOWS_BE =
            BLOCK_ENTITIES.register("bellows_be",
                    () -> BlockEntityType.Builder.of(BellowsBlockEntity::new, BELLOWS.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AutoDistillerBlockEntity>> AUTO_DISTILLER_BE =
            BLOCK_ENTITIES.register("auto_distiller_be",
                    () -> BlockEntityType.Builder.of(AutoDistillerBlockEntity::new, AUTO_DISTILLER.get()).build(null));

    private DraftBlocks() {
    }

    private static BlockBehaviour.Properties metal(MapColor color, float strength) {
        return BlockBehaviour.Properties.of().mapColor(color).strength(strength, 6f).sound(SoundType.COPPER)
                .requiresCorrectToolForDrops().noOcclusion().forceSolidOn();
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);
        BLOCK_ENTITIES.register(eventBus);
    }
}
