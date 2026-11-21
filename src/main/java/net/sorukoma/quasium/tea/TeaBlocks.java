package net.sorukoma.quasium.tea;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.ecology.EcologyConfig;
import net.sorukoma.quasium.ecology.EcologyRegistry;
import net.sorukoma.quasium.progression.ConfigArgs;
import net.sorukoma.quasium.recipe.DryingRecipes;

public final class TeaBlocks {
    private static final ResourceLocation TEA_STEP = ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "samovar");

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> TEA_CUPS =
            EcologyRegistry.DATA_COMPONENTS.registerComponentType("tea_cups",
                    builder -> builder.persistent(ExtraCodecs.NON_NEGATIVE_INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    public static final DeferredHolder<MobEffect, VigorEffect> VIGOR = EcologyRegistry.EFFECTS.register("vigor", VigorEffect::new);

    public static final DeferredBlock<TeaBushBlock> TEA_BUSH = EcologyRegistry.BLOCKS.registerBlock("tea_bush", TeaBushBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).noCollission().randomTicks().instabreak()
                    .sound(SoundType.SWEET_BERRY_BUSH).pushReaction(PushReaction.DESTROY));

    public static final DeferredBlock<SamovarBlock> SAMOVAR = EcologyRegistry.BLOCKS.registerBlock("samovar", SamovarBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(2.5f, 6f).sound(SoundType.COPPER)
                    .requiresCorrectToolForDrops().noOcclusion()
                    .lightLevel(state -> state.getValue(SamovarBlock.LIT) ? SamovarBlock.LIT_LIGHT : 0));

    public static final DeferredItem<ItemNameBlockItem> TEA_SEEDS = EcologyRegistry.ITEMS.register("tea_seeds",
            () -> new ItemNameBlockItem(TEA_BUSH.get(), new Item.Properties()));
    public static final DeferredItem<Item> TEA_LEAF = EcologyRegistry.ITEMS.registerSimpleItem("tea_leaf");
    public static final DeferredItem<Item> DRIED_TEA = EcologyRegistry.ITEMS.registerSimpleItem("dried_tea");
    public static final DeferredItem<Item> TEA_CUP = EcologyRegistry.ITEMS.registerSimpleItem("tea_cup", new Item.Properties().stacksTo(16));
    public static final DeferredItem<CupOfTeaItem> CUP_OF_TEA = EcologyRegistry.ITEMS.register("cup_of_tea",
            () -> new CupOfTeaItem(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<TeapotItem> TEAPOT = EcologyRegistry.ITEMS.register("teapot",
            () -> new TeapotItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<BlockItem> SAMOVAR_ITEM = EcologyRegistry.ITEMS.registerSimpleBlockItem(SAMOVAR);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SamovarBlockEntity>> SAMOVAR_BE =
            EcologyRegistry.BLOCK_ENTITIES.register("samovar",
                    () -> BlockEntityType.Builder.of(SamovarBlockEntity::new, SAMOVAR.get()).build(null));

    public static final DeferredHolder<MenuType<?>, MenuType<SamovarMenu>> SAMOVAR_MENU =
            EcologyRegistry.MENUS.register("samovar", () -> IMenuTypeExtension.create(SamovarMenu::new));

    private TeaBlocks() {
    }

    public static void register(IEventBus eventBus) {
        eventBus.addListener(TeaBlocks::onCommonSetup);
    }

    private static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> DryingRecipes.add(TEA_LEAF, DRIED_TEA, () -> ConfigArgs.synced(EcologyConfig.TEA_DRYING_SECONDS) * 20, TEA_STEP));
    }
}
