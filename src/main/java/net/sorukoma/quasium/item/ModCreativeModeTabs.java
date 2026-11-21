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
                        output.accept(ModItems.FRIES);
                        output.accept(ModItems.BOTTLED_GHAST_ACID);
                        output.accept(ModItems.GLYCERIN_BOTTLE);
                        output.accept(ModItems.MOLASSES_BOTTLE);
                        output.accept(ModItems.BEEF_JERKY);
                        output.accept(ModItems.PORK_JERKY);
                        output.accept(ModItems.CHICKEN_JERKY);
                        output.accept(ModItems.MUTTON_JERKY);
                        output.accept(ModItems.RABBIT_JERKY);
                        output.accept(ModItems.COD_JERKY);
                        output.accept(ModItems.SALMON_JERKY);
                        output.accept(ModItems.MINT_CROP_SEEDS);
                        output.accept(ModItems.ANISE_SEEDS);
                        output.accept(ModItems.LEMON_SAPLING);
                        output.accept(ModItems.LIME_SAPLING);
                        output.accept(ModItems.ORANGE_SAPLING);
                        output.accept(ModItems.GRAPEFRUIT_SAPLING);
                        output.accept(ModItems.GRAPE_SAPLING);
                        output.accept(ModItems.COCONUT_SAPLING);
                        output.accept(ModItems.LAVENDER_SEEDS);
                        output.accept(ModItems.COCOA_CROP_SEEDS);
                        output.accept(ModItems.MINT_LEAVES);
                        output.accept(ModItems.ANISE);
                        output.accept(ModItems.LEMON);
                        output.accept(ModItems.LIME);
                        output.accept(ModItems.ORANGE);
                        output.accept(ModItems.GRAPEFRUIT);
                        output.accept(ModItems.GRAPE);
                        output.accept(ModItems.COCONUT);
                        output.accept(ModItems.LAVENDER);
                        output.accept(ModItems.COCOA_NIBS);
                        for (String type : new String[]{"mint", "berry", "lemon", "lime", "orange", "grapefruit", "ice", "honey", "nether", "lavender", "cocoa",
                                "apple", "watermelon", "grape", "cherry", "vanilla", "coffee", "cinnamon", "coconut", "rose", "anise"}) {
                            ItemStack essence = new ItemStack(ModItems.ESSENCE_BOTTLE.get());
                            essence.set(net.sorukoma.quasium.component.ModDataComponents.ESSENCE_TYPE.get(), type);
                            essence.set(net.sorukoma.quasium.component.ModDataComponents.ESSENCE_AMOUNT.get(), 20);
                            output.accept(essence);
                        }
                        for (net.sorukoma.quasium.magic.NoteType note : net.sorukoma.quasium.magic.NoteType.values()) {
                            output.accept(EssenceBottleItem.of(note.id(), 20));
                        }
                        net.sorukoma.quasium.data.PlayerFlavorPreferenceData.FLAVORS.forEach(flavor -> output.accept(AromaVialItem.of(flavor)));
                        output.accept(ModItems.DIKAR_SEEDS);
                        output.accept(ModItems.PESCHANIK_SEEDS);
                        output.accept(ModItems.BOLOTNIK_SEEDS);
                        output.accept(ModItems.LEDYANOY_SEEDS);
                        output.accept(ModItems.HYBRID_TOBACCO_SEEDS);
                        output.accept(ModItems.ECHO_SEEDS);
                        output.accept(ModItems.ASHEN_SEEDS);
                        output.accept(ModItems.CHORUS_SEEDS);
                        output.accept(ModItems.PRIMORDIAL_SEEDS);
                        output.accept(ModItems.DIKAR_LEAVES);
                        output.accept(ModItems.PESCHANIK_LEAVES);
                        output.accept(ModItems.BOLOTNIK_LEAVES);
                        output.accept(ModItems.LEDYANOY_LEAVES);
                        output.accept(ModItems.HYBRID_TOBACCO_LEAVES);
                        output.accept(ModItems.ECHO_LEAVES);
                        output.accept(ModItems.ASHEN_LEAVES);
                        output.accept(ModItems.CHORUS_LEAVES);
                        output.accept(ModItems.PRIMORDIAL_LEAVES);
                        output.accept(ModItems.MINT_TOBACCO);
                        output.accept(ModItems.BERRY_TOBACCO);
                        output.accept(ModItems.LEMON_TOBACCO);
                        output.accept(ModItems.LIME_TOBACCO);
                        output.accept(ModItems.ORANGE_TOBACCO);
                        output.accept(ModItems.GRAPEFRUIT_TOBACCO);
                        output.accept(ModItems.APPLE_TOBACCO);
                        output.accept(ModItems.WATERMELON_TOBACCO);
                        output.accept(ModItems.GRAPE_TOBACCO);
                        output.accept(ModItems.CHERRY_TOBACCO);
                        output.accept(ModItems.VANILLA_TOBACCO);
                        output.accept(ModItems.COFFEE_TOBACCO);
                        output.accept(ModItems.CINNAMON_TOBACCO);
                        output.accept(ModItems.COCONUT_TOBACCO);
                        output.accept(ModItems.ROSE_TOBACCO);
                        output.accept(ModItems.ANISE_TOBACCO);
                        output.accept(ModBlocks.WOODEN_TOBACCO_POLE_ITEM);
                        output.accept(ModBlocks.REINFORCED_TOBACCO_POLE_ITEM);
                        output.accept(ModBlocks.GOLDEN_TOBACCO_POLE_ITEM);
                        output.accept(ModBlocks.DIAMOND_TOBACCO_POLE_ITEM);
                        output.accept(ModItems.SELF_MIXED_TOBACCO);
                        output.accept(ModItems.UNFIRED_HOOKAH_BOWL);
                        output.accept(ModItems.HOOKAH_BOWL);
                        output.accept(ModItems.ONE_GRAM_TOBACCO);
                        output.accept(ModItems.ONE_GRAM_SELF_MIXED_TOBACCO);
                        output.accept(ModItems.BURNED_TOBACCO);
                        output.accept(ModItems.RAW_BRIQUETTE);
                        output.accept(ModItems.CHARCOAL_22);
                        output.accept(ModItems.CHARCOAL_25);
                        output.accept(ModItems.CHARCOAL_27);
                        output.accept(ModItems.CALLOUD);
                        output.accept(ModBlocks.HOOKAH_BLOCK_ITEM);
                        output.accept(ModBlocks.MODULAR_HOOKAH_WHITE_ITEM);
                        output.accept(ModBlocks.MODULAR_HOOKAH_BLACK_ITEM);
                        output.accept(ModBlocks.MODULAR_HOOKAH_RED_ITEM);
                        output.accept(ModBlocks.MODULAR_HOOKAH_BLUE_ITEM);
                        output.accept(ModBlocks.MODULAR_HOOKAH_AMBER_ITEM);
                        output.accept(ModBlocks.MODULAR_HOOKAH_JADE_ITEM);
                        output.accept(ModBlocks.MODULAR_HOOKAH_VIOLET_ITEM);
                        output.accept(ModBlocks.MODULAR_HOOKAH_GOLD_ITEM);
                        output.accept(ModItems.MODULAR_SHAFT_WHITE);
                        output.accept(ModItems.MODULAR_SHAFT_BLACK);
                        output.accept(ModItems.MODULAR_SHAFT_RED);
                        output.accept(ModItems.MODULAR_SHAFT_BLUE);
                        output.accept(ModItems.MODULAR_SHAFT_AMBER);
                        output.accept(ModItems.HOOKAH_PIPE);
                        output.accept(ModItems.BACKPACK_HOOKAH);
                        output.accept(ModBlocks.DRYING_SHELF_ITEM);
                        output.accept(ModBlocks.FERMENTATION_BARREL_ITEM);
                        output.accept(ModBlocks.SOAKING_BOWL_ITEM);
                        output.accept(ModBlocks.ESSENCE_PRESS_ITEM);
                        output.accept(ModBlocks.BONSAI_POT_ITEM);
                        output.accept(ModItems.HOOKAH_DIFFUSER);
                        output.accept(ModItems.HOOKAH_COOLER);
                        output.accept(ModItems.HOOKAH_ICE_CHAMBER);
                        output.accept(ModBlocks.HEATER_ITEM);
                        output.accept(ModBlocks.DISTILLATION_CUBE_ITEM);
                        output.accept(ModBlocks.NOTE_JAR_ITEM);
                        net.sorukoma.quasium.block.LoungeBlocks.ITEMS.getEntries().forEach(item -> output.accept(item.get()));
                        net.sorukoma.quasium.block.MixingBlocks.ITEMS.getEntries().forEach(item -> output.accept(item.get()));
                        net.sorukoma.quasium.draft.DraftBlocks.ITEMS.getEntries().forEach(item -> output.accept(item.get()));
                        net.sorukoma.quasium.draft.GeneratorBlocks.ITEMS.getEntries().forEach(item -> output.accept(item.get()));
                        net.sorukoma.quasium.draft.MachineBlocks.ITEMS.getEntries().forEach(item -> output.accept(item.get()));
                        net.sorukoma.quasium.draft.ForgeBlocks.ITEMS.getEntries().forEach(item -> output.accept(item.get()));
                        net.sorukoma.quasium.cart.HookahCarts.ITEMS.getEntries().forEach(item -> output.accept(item.get()));
                        net.sorukoma.quasium.golem.MarkBlocks.ITEMS.getEntries().forEach(item -> output.accept(item.get()));
                        net.sorukoma.quasium.golem.program.ProgramBlocks.ITEMS.getEntries().forEach(item -> output.accept(item.get()));
                        net.sorukoma.quasium.golem.CalloudGolems.ITEMS.getEntries().forEach(item -> output.accept(item.get()));
                        net.sorukoma.quasium.golem.ClayGolems.ITEMS.getEntries().forEach(item -> output.accept(item.get()));
                        net.sorukoma.quasium.golem.WardenGolems.ITEMS.getEntries().forEach(item -> output.accept(item.get()));
                        net.sorukoma.quasium.kiln.KilnBlocks.ITEMS.getEntries().forEach(item -> output.accept(item.get()));
                        net.sorukoma.quasium.magic.page.PageRegistry.ITEMS.getEntries().forEach(item -> output.accept(item.get()));
                        net.sorukoma.quasium.magic.page.spirit.SpiritRegistry.ITEMS.getEntries().forEach(item -> output.accept(item.get()));
                        net.sorukoma.quasium.selection.SelectionRegistry.ITEMS.getEntries().forEach(item -> output.accept(item.get()));
                        net.sorukoma.quasium.ecology.EcologyRegistry.ITEMS.getEntries().forEach(item -> output.accept(item.get()));
                        net.sorukoma.quasium.mind.MindRegistry.ITEMS.getEntries().forEach(item -> output.accept(item.get()));
                        net.sorukoma.quasium.ritual.RitualRegistry.ITEMS.getEntries().forEach(item -> output.accept(item.get()));
                        net.sorukoma.quasium.smokelands.SmokelandsRegistry.ITEMS.getEntries().forEach(item -> output.accept(item.get()));
                        output.accept(ModItems.FOCUS_UPGRADE_RESONATOR);
                        output.accept(ModItems.FOCUS_UPGRADE_COLLIMATOR);
                        output.accept(ModItems.FOCUS_UPGRADE_COOLER);
                        output.accept(ModItems.FOCUS_UPGRADE_SPLITTER);
                        output.accept(ModItems.FOCUS_UPGRADE_CONDENSER);
                        output.accept(ModItems.FOCUS_UPGRADE_FILTER);
                        output.accept(ModItems.EMPTY_SMOKE_JAR);
                        output.accept(ModBlocks.SMOKE_JAR_ITEM);
                        output.accept(ModItems.MUSIC_DISC_LOFI_KAIF_BEATS);
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
