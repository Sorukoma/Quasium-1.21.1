package net.sorukoma.quasium.draft;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.screen.SmokeBoilerMenu;

public final class GeneratorBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(QuasiumMod.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(QuasiumMod.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, QuasiumMod.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, QuasiumMod.MOD_ID);

    public static final DeferredBlock<SmokeBoilerBlock> SMOKE_BOILER = BLOCKS.register("smoke_boiler",
            () -> new SmokeBoilerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(3.5f)
                    .sound(SoundType.STONE).requiresCorrectToolForDrops()
                    .lightLevel(state -> state.getValue(SmokeBoilerBlock.LIT) ? 13 : 0)));
    public static final DeferredBlock<QuasiumFanBlock> QUASIUM_FAN = BLOCKS.register("quasium_fan",
            () -> new QuasiumFanBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(4f, 6f)
                    .sound(SoundType.COPPER).requiresCorrectToolForDrops()));

    public static final DeferredItem<BlockItem> SMOKE_BOILER_ITEM = ITEMS.registerSimpleBlockItem(SMOKE_BOILER);
    public static final DeferredItem<BlockItem> QUASIUM_FAN_ITEM = ITEMS.registerSimpleBlockItem(QUASIUM_FAN);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SmokeBoilerBlockEntity>> SMOKE_BOILER_BE =
            BLOCK_ENTITIES.register("smoke_boiler_be",
                    () -> BlockEntityType.Builder.of(SmokeBoilerBlockEntity::new, SMOKE_BOILER.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<QuasiumFanBlockEntity>> QUASIUM_FAN_BE =
            BLOCK_ENTITIES.register("quasium_fan_be",
                    () -> BlockEntityType.Builder.of(QuasiumFanBlockEntity::new, QUASIUM_FAN.get()).build(null));

    public static final DeferredHolder<MenuType<?>, MenuType<SmokeBoilerMenu>> SMOKE_BOILER_MENU =
            MENUS.register("smoke_boiler_menu", () -> IMenuTypeExtension.create(SmokeBoilerMenu::new));

    private GeneratorBlocks() {
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);
        BLOCK_ENTITIES.register(eventBus);
        MENUS.register(eventBus);
    }
}
