package net.sorukoma.quasium.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.block.ModBlocks;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, QuasiumMod.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> QUASUM_TAB =
            CREATIVE_MODE_TABS.register("quasium_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.quasium"))
                    .icon(() -> new ItemStack(ModItems.HOOKAH_PIPE.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.QUASIUMINGOT);
                        output.accept(ModItems.RAWQUASIUM);
                        output.accept(ModBlocks.UNDRIED_QUASIUM_BLOCK_ITEM);
                        output.accept(ModBlocks.QUASIUM_BLOCK_ITEM);
                        output.accept(ModBlocks.DEEPSLATE_QUASIUM_ORE_ITEM);
                        output.accept(ModItems.TALLOW);
                        output.accept(ModItems.BOTTLED_GHAST_ACID);
                        output.accept(ModItems.GLYCERIN_BOTTLE);
                        output.accept(ModItems.MOLASSES_BOTTLE);
                        output.accept(ModItems.TOBACCO_SEEDS);
                        output.accept(ModItems.MAKHORKA_SEEDS);
                        output.accept(ModItems.TOBACCO_LEAVES);
                        output.accept(ModItems.MAKHORKA_LEAVES);
                        output.accept(ModItems.MINT_TOBACCO);
                        output.accept(ModItems.CITRUS_TOBACCO);
                        output.accept(ModItems.BERRY_TOBACCO);
                        output.accept(ModItems.MIXED_TOBACCO);
                        output.accept(ModItems.SELF_MIXED_TOBACCO);
                        output.accept(ModItems.MIXING_BOWL);
                        output.accept(ModItems.HOOKAH_BOWL);
                        output.accept(ModItems.ONE_GRAM_TOBACCO);
                        output.accept(ModItems.ONE_GRAM_SELF_MIXED_TOBACCO);
                        output.accept(ModItems.BURNED_TOBACCO);
                        output.accept(ModItems.CHARCOAL_22);
                        output.accept(ModItems.CHARCOAL_25);
                        output.accept(ModItems.CHARCOAL_27);
                        output.accept(ModItems.CALLOUD);
                        output.accept(ModBlocks.HOOKAH_BLOCK_ITEM);
                        output.accept(ModItems.HOOKAH_PIPE);
                        output.accept(ModItems.BACKPACK_HOOKAH);
                        output.accept(ModBlocks.DRYING_SHELF_ITEM);
                        output.accept(ModBlocks.FERMENTATION_BARREL_ITEM);
                        output.accept(ModBlocks.SOAKING_BOWL_ITEM);
                        output.accept(ModBlocks.HEATER_ITEM);
                        output.accept(ModItems.NETHERITE_HAMMER);
                        output.accept(ModBlocks.QUASIUM_ANVIL_ITEM);
                        output.accept(ModItems.QUASIUM_HELMET);
                        output.accept(ModItems.QUASIUM_CHESTPLATE);
                        output.accept(ModItems.QUASIUM_LEGGINGS);
                        output.accept(ModItems.QUASIUM_BOOTS);
                        output.accept(ModItems.KAIFOMETER);
                        output.accept(ModItems.PROGRESSION_BOOK);
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}