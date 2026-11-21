package net.sorukoma.quasium.tea;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.ecology.EcologyConfig;
import net.sorukoma.quasium.ecology.EcologyRegistry;
import net.sorukoma.quasium.item.ModItems;
import net.sorukoma.quasium.magic.NoteType;
import net.sorukoma.quasium.progression.ConfigArgs;
import net.sorukoma.quasium.recipe.AnvilRecipes;

import java.util.List;

public final class SealBlocks {
    public static final int LIGHT = 9;

    public static final DeferredBlock<SobrietySealBlock> SOBRIETY_SEAL = EcologyRegistry.BLOCKS.registerBlock("sobriety_seal",
            SobrietySealBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).strength(1.5f, 6f)
                    .sound(SoundType.STONE).requiresCorrectToolForDrops().noOcclusion().lightLevel(state -> LIGHT));

    public static final DeferredItem<Item> SOBRIETY_SEAL_ITEM = EcologyRegistry.ITEMS.register("sobriety_seal",
            () -> new BlockItem(SOBRIETY_SEAL.get(), new Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SobrietySealBlockEntity>> SOBRIETY_SEAL_BE =
            EcologyRegistry.BLOCK_ENTITIES.register("sobriety_seal",
                    () -> BlockEntityType.Builder.of(SobrietySealBlockEntity::new, SOBRIETY_SEAL.get()).build(null));

    public static final ResourceLocation RESEARCH = ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "research_smog_seal");

    private SealBlocks() {
    }

    public static void register(IEventBus eventBus) {
        eventBus.addListener(SealBlocks::onCommonSetup);
    }

    private static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> AnvilRecipes.addSober(List.of(() -> Items.QUARTZ, () -> ModItems.ESSENCE_BOTTLE.get()), SOBRIETY_SEAL_ITEM,
                () -> ConfigArgs.synced(EcologyConfig.SEAL_ANVIL_HITS), AnvilRecipes.noteBottleIn(NoteType.DEW, 1),
                () -> ConfigArgs.synced(EcologyConfig.SEAL_CRAFT_MAX_KAIF), RESEARCH));
    }
}
