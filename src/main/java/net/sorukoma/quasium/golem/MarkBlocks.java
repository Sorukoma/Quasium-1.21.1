package net.sorukoma.quasium.golem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.golem.program.ProgramBlocks;

import java.util.UUID;

public final class MarkBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(QuasiumMod.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(QuasiumMod.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, QuasiumMod.MOD_ID);
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, QuasiumMod.MOD_ID);

    public static final DeferredBlock<SmokeMarkBlock> HARVEST_MARK = mark(MarkType.HARVEST);
    public static final DeferredBlock<SmokeMarkBlock> FILL_MARK = mark(MarkType.FILL);
    public static final DeferredBlock<SmokeMarkBlock> GUARD_MARK = mark(MarkType.GUARD);
    public static final DeferredBlock<SmokeMarkBlock> HEAT_MARK = mark(MarkType.HEAT);
    public static final DeferredBlock<SmokeMarkBlock> ZONE_MARK = mark(MarkType.ZONE);
    public static final DeferredBlock<GolemFeederBlock> GOLEM_FEEDER = BLOCKS.register("golem_feeder",
            () -> new GolemFeederBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2f).sound(SoundType.WOOD)
                    .noOcclusion().ignitedByLava()));

    public static final DeferredItem<BlockItem> HARVEST_MARK_ITEM = ITEMS.registerSimpleBlockItem(HARVEST_MARK);
    public static final DeferredItem<BlockItem> FILL_MARK_ITEM = ITEMS.registerSimpleBlockItem(FILL_MARK);
    public static final DeferredItem<BlockItem> GUARD_MARK_ITEM = ITEMS.registerSimpleBlockItem(GUARD_MARK);
    public static final DeferredItem<BlockItem> HEAT_MARK_ITEM = ITEMS.registerSimpleBlockItem(HEAT_MARK);
    public static final DeferredItem<BlockItem> ZONE_MARK_ITEM = ITEMS.registerSimpleBlockItem(ZONE_MARK);
    public static final DeferredItem<BlockItem> GOLEM_FEEDER_ITEM = ITEMS.registerSimpleBlockItem(GOLEM_FEEDER);
    public static final DeferredItem<Item> GOLEM_STAFF = ITEMS.register("golem_staff",
            () -> new GolemStaffItem(new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SmokeMarkBlockEntity>> MARK_BE =
            BLOCK_ENTITIES.register("smoke_mark", () -> BlockEntityType.Builder.of(SmokeMarkBlockEntity::new,
                    HARVEST_MARK.get(), FILL_MARK.get(), GUARD_MARK.get(), HEAT_MARK.get(), ZONE_MARK.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GolemFeederBlockEntity>> FEEDER_BE =
            BLOCK_ENTITIES.register("golem_feeder", () -> BlockEntityType.Builder.of(GolemFeederBlockEntity::new, GOLEM_FEEDER.get()).build(null));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UUID>> STAFF_GOLEM =
            COMPONENTS.registerComponentType("staff_golem", builder -> builder.persistent(UUIDUtil.CODEC)
                    .networkSynchronized(UUIDUtil.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BlockPos>> STAFF_MARK =
            COMPONENTS.registerComponentType("staff_mark", builder -> builder.persistent(BlockPos.CODEC)
                    .networkSynchronized(BlockPos.STREAM_CODEC));

    private MarkBlocks() {
    }

    public static Item item(MarkType type) {
        return switch (type) {
            case HARVEST -> HARVEST_MARK_ITEM.get();
            case FILL -> FILL_MARK_ITEM.get();
            case GUARD -> GUARD_MARK_ITEM.get();
            case HEAT -> HEAT_MARK_ITEM.get();
            case ZONE -> ZONE_MARK_ITEM.get();
            case FEED -> GOLEM_FEEDER_ITEM.get();
            case PROGRAM -> ProgramBlocks.PROGRAM_TABLE_ITEM.get();
        };
    }

    private static DeferredBlock<SmokeMarkBlock> mark(MarkType type) {
        return BLOCKS.register(type.id() + "_mark", () -> new SmokeMarkBlock(type, BlockBehaviour.Properties.of()
                .noCollission().instabreak().sound(SoundType.WOOL).noOcclusion().pushReaction(PushReaction.DESTROY)
                .lightLevel(state -> 3)));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);
        BLOCK_ENTITIES.register(eventBus);
        COMPONENTS.register(eventBus);
        SmokeMarks.register(eventBus);
        eventBus.addListener(RegisterCapabilitiesEvent.class, event -> event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK, FEEDER_BE.get(), (feeder, side) -> feeder.food()));
    }
}
