package net.sorukoma.quasium.golem.program;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
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

public final class ProgramBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(QuasiumMod.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(QuasiumMod.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, QuasiumMod.MOD_ID);
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, QuasiumMod.MOD_ID);

    public static final DeferredBlock<ProgramTableBlock> PROGRAM_TABLE = BLOCKS.register("program_table",
            () -> new ProgramTableBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS)
                    .strength(2.5f).sound(SoundType.WOOD).ignitedByLava()));

    public static final DeferredItem<BlockItem> PROGRAM_TABLE_ITEM = ITEMS.registerSimpleBlockItem(PROGRAM_TABLE);
    public static final DeferredItem<Item> PLACE_MARKER = ITEMS.register("place_marker",
            () -> new PlaceMarkerItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> PROGRAM_CARD = ITEMS.register("program_card", () -> new ProgramCardItem(new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ProgramTableBlockEntity>> PROGRAM_TABLE_BE =
            BLOCK_ENTITIES.register("program_table", () -> BlockEntityType.Builder.of(ProgramTableBlockEntity::new, PROGRAM_TABLE.get()).build(null));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<MarkedPlace>> MARKED_PLACE =
            COMPONENTS.registerComponentType("marked_place", builder -> builder.persistent(MarkedPlace.CODEC)
                    .networkSynchronized(MarkedPlace.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Program>> PROGRAM =
            COMPONENTS.registerComponentType("program", builder -> builder.persistent(Program.CODEC).networkSynchronized(Program.STREAM_CODEC));

    private ProgramBlocks() {
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);
        BLOCK_ENTITIES.register(eventBus);
        COMPONENTS.register(eventBus);
    }
}
