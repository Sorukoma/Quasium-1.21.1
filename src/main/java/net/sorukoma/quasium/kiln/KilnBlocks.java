package net.sorukoma.quasium.kiln;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.screen.SmokeKilnMenu;

public final class KilnBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(QuasiumMod.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(QuasiumMod.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, QuasiumMod.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, QuasiumMod.MOD_ID);

    public static final DeferredBlock<SmokeKilnBlock> SMOKE_KILN = BLOCKS.register("smoke_kiln",
            () -> new SmokeKilnBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BRICKS)
                    .lightLevel(state -> state.getValue(SmokeKilnBlock.LIT) ? 7 : 0)));
    public static final DeferredItem<BlockItem> SMOKE_KILN_ITEM = ITEMS.registerSimpleBlockItem(SMOKE_KILN);
    public static final DeferredItem<Item> EMBER_CHUNK = ITEMS.registerSimpleItem("ember_chunk");

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SmokeKilnBlockEntity>> SMOKE_KILN_BE =
            BLOCK_ENTITIES.register("smoke_kiln_be",
                    () -> BlockEntityType.Builder.of(SmokeKilnBlockEntity::new, SMOKE_KILN.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<SmokeKilnMenu>> SMOKE_KILN_MENU =
            MENUS.register("smoke_kiln_menu", () -> IMenuTypeExtension.create(SmokeKilnMenu::new));

    private KilnBlocks() {
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);
        BLOCK_ENTITIES.register(eventBus);
        MENUS.register(eventBus);
        eventBus.addListener(RegisterCapabilitiesEvent.class, event -> event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK, SMOKE_KILN_BE.get(), (kiln, side) -> kiln.automation()));
    }
}
