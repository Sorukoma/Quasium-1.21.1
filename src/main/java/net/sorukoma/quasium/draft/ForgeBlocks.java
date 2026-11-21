package net.sorukoma.quasium.draft;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
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

public final class ForgeBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(QuasiumMod.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(QuasiumMod.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, QuasiumMod.MOD_ID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, QuasiumMod.MOD_ID);

    public static final DeferredBlock<PneumaticHammerBlock> PNEUMATIC_HAMMER = BLOCKS.register("pneumatic_hammer",
            () -> new PneumaticHammerBlock(machine(MapColor.METAL, SoundType.METAL)));
    public static final DeferredBlock<SmokeCannonBlock> SMOKE_CANNON = BLOCKS.register("smoke_cannon",
            () -> new SmokeCannonBlock(machine(MapColor.COLOR_ORANGE, SoundType.COPPER)));

    public static final DeferredItem<BlockItem> PNEUMATIC_HAMMER_ITEM = ITEMS.registerSimpleBlockItem(PNEUMATIC_HAMMER);
    public static final DeferredItem<BlockItem> SMOKE_CANNON_ITEM = ITEMS.registerSimpleBlockItem(SMOKE_CANNON);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PneumaticHammerBlockEntity>> PNEUMATIC_HAMMER_BE =
            BLOCK_ENTITIES.register("pneumatic_hammer_be",
                    () -> BlockEntityType.Builder.of(PneumaticHammerBlockEntity::new, PNEUMATIC_HAMMER.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SmokeCannonBlockEntity>> SMOKE_CANNON_BE =
            BLOCK_ENTITIES.register("smoke_cannon_be",
                    () -> BlockEntityType.Builder.of(SmokeCannonBlockEntity::new, SMOKE_CANNON.get()).build(null));

    public static final DeferredHolder<EntityType<?>, EntityType<SmokeShotEntity>> SMOKE_SHOT = ENTITY_TYPES.register("smoke_shot",
            () -> EntityType.Builder.<SmokeShotEntity>of(SmokeShotEntity::new, MobCategory.MISC).sized(0.3125f, 0.3125f)
                    .noSave().clientTrackingRange(4).updateInterval(10).build("smoke_shot"));

    private ForgeBlocks() {
    }

    private static BlockBehaviour.Properties machine(MapColor color, SoundType sound) {
        return BlockBehaviour.Properties.of().mapColor(color).strength(3.5f, 6f).sound(sound)
                .requiresCorrectToolForDrops().noOcclusion();
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);
        BLOCK_ENTITIES.register(eventBus);
        ENTITY_TYPES.register(eventBus);
    }
}
