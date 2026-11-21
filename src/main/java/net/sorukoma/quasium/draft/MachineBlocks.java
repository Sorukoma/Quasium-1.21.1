package net.sorukoma.quasium.draft;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.screen.EssencePumpMenu;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID)
public final class MachineBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(QuasiumMod.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(QuasiumMod.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, QuasiumMod.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, QuasiumMod.MOD_ID);

    public static final DeferredBlock<EssencePumpBlock> ESSENCE_PUMP = BLOCKS.register("essence_pump",
            () -> new EssencePumpBlock(machine(MapColor.COLOR_ORANGE)));
    public static final DeferredBlock<FlaskChargerBlock> FLASK_CHARGER = BLOCKS.register("flask_charger",
            () -> new FlaskChargerBlock(machine(MapColor.COLOR_PURPLE)));
    public static final DeferredBlock<VentilationBlock> VENTILATION = BLOCKS.register("ventilation",
            () -> new VentilationBlock(machine(MapColor.COLOR_ORANGE)));

    public static final DeferredItem<BlockItem> ESSENCE_PUMP_ITEM = ITEMS.registerSimpleBlockItem(ESSENCE_PUMP);
    public static final DeferredItem<BlockItem> FLASK_CHARGER_ITEM = ITEMS.registerSimpleBlockItem(FLASK_CHARGER);
    public static final DeferredItem<BlockItem> VENTILATION_ITEM = ITEMS.registerSimpleBlockItem(VENTILATION);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EssencePumpBlockEntity>> ESSENCE_PUMP_BE =
            BLOCK_ENTITIES.register("essence_pump_be",
                    () -> BlockEntityType.Builder.of(EssencePumpBlockEntity::new, ESSENCE_PUMP.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FlaskChargerBlockEntity>> FLASK_CHARGER_BE =
            BLOCK_ENTITIES.register("flask_charger_be",
                    () -> BlockEntityType.Builder.of(FlaskChargerBlockEntity::new, FLASK_CHARGER.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VentilationBlockEntity>> VENTILATION_BE =
            BLOCK_ENTITIES.register("ventilation_be",
                    () -> BlockEntityType.Builder.of(VentilationBlockEntity::new, VENTILATION.get()).build(null));

    public static final DeferredHolder<MenuType<?>, MenuType<EssencePumpMenu>> ESSENCE_PUMP_MENU =
            MENUS.register("essence_pump_menu", () -> new MenuType<>(EssencePumpMenu::new, FeatureFlags.VANILLA_SET));

    private MachineBlocks() {
    }

    private static BlockBehaviour.Properties machine(MapColor color) {
        return BlockBehaviour.Properties.of().mapColor(color).strength(3f, 6f).sound(SoundType.COPPER).requiresCorrectToolForDrops();
    }

    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ESSENCE_PUMP_BE.get(), (pump, side) -> pump.automation());
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);
        BLOCK_ENTITIES.register(eventBus);
        MENUS.register(eventBus);
    }
}
