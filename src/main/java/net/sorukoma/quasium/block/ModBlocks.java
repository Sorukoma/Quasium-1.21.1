package net.sorukoma.quasium.block;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.item.ModItems;

import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(QuasiumMod.MOD_ID);

    public static final DeferredBlock<Block> HOOKAH_BLOCK = BLOCKS.register("hookah_block",
            () -> new HookahBlock());

    // Регистрируем BlockItem напрямую
    public static final DeferredItem<BlockItem> HOOKAH_BLOCK_ITEM = ModItems.ITEMS.register("hookah_block",
            () -> new BlockItem(HOOKAH_BLOCK.get(), new Item.Properties()));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
