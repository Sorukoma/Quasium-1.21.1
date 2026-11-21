package net.sorukoma.quasium.block;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.block.entity.MixingTableBlockEntity;
import net.sorukoma.quasium.screen.MixingTableMenu;

public final class MixingBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(QuasiumMod.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(QuasiumMod.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, QuasiumMod.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, QuasiumMod.MOD_ID);

    public static final DeferredBlock<MixingTableBlock> MIXING_TABLE = BLOCKS.register("mixing_table",
            () -> new MixingTableBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CRAFTING_TABLE)));
    public static final DeferredItem<BlockItem> MIXING_TABLE_ITEM = ITEMS.registerSimpleBlockItem(MIXING_TABLE);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MixingTableBlockEntity>> MIXING_TABLE_BE =
            BLOCK_ENTITIES.register("mixing_table_be",
                    () -> BlockEntityType.Builder.of(MixingTableBlockEntity::new, MIXING_TABLE.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<MixingTableMenu>> MIXING_TABLE_MENU =
            MENUS.register("mixing_table_menu", () -> new MenuType<>(MixingTableMenu::new, FeatureFlags.VANILLA_SET));

    private MixingBlocks() {
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);
        BLOCK_ENTITIES.register(eventBus);
        MENUS.register(eventBus);
    }
}
