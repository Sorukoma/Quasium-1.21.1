package net.sorukoma.quasium.mind;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.mind.ghost.HookahGhost;
import net.sorukoma.quasium.mind.overdose.SmokePhantom;
import net.sorukoma.quasium.mind.table.ResearchTableBlock;
import net.sorukoma.quasium.mind.table.ResearchTableBlockEntity;
import net.sorukoma.quasium.mind.table.ResearchTableMenu;

public final class MindRegistry {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(QuasiumMod.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(QuasiumMod.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, QuasiumMod.MOD_ID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, QuasiumMod.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, QuasiumMod.MOD_ID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, QuasiumMod.MOD_ID);
    public static final DeferredRegister.DataComponents DATA_COMPONENTS = DeferredRegister.createDataComponents(QuasiumMod.MOD_ID);
    public static final DeferredRegister<ResourceLocation> CUSTOM_STATS = DeferredRegister.create(Registries.CUSTOM_STAT, QuasiumMod.MOD_ID);
    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, QuasiumMod.MOD_ID);

    public static final DeferredBlock<ResearchTableBlock> RESEARCH_TABLE = BLOCKS.register("research_table",
            () -> new ResearchTableBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5f)
                    .sound(SoundType.WOOD)));
    public static final DeferredItem<BlockItem> RESEARCH_TABLE_ITEM = ITEMS.registerSimpleBlockItem(RESEARCH_TABLE);
    public static final DeferredItem<Item> TREATISE = ITEMS.register("treatise",
            () -> new TooltipItem(new Item.Properties().stacksTo(16), "tooltip.quasium.treatise"));
    public static final DeferredItem<Item> SMOKELAND_HOOK = ITEMS.register("smokeland_hook",
            () -> new TooltipItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC), "tooltip.quasium.smokeland_hook"));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ResearchTableBlockEntity>> RESEARCH_TABLE_BE =
            BLOCK_ENTITIES.register("research_table", () -> BlockEntityType.Builder.of(ResearchTableBlockEntity::new, RESEARCH_TABLE.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<ResearchTableMenu>> RESEARCH_TABLE_MENU =
            MENUS.register("research_table", () -> IMenuTypeExtension.create(ResearchTableMenu::new));

    public static final DeferredHolder<EntityType<?>, EntityType<SmokePhantom>> SMOKE_PHANTOM = ENTITY_TYPES.register("smoke_phantom",
            () -> EntityType.Builder.of(SmokePhantom::new, MobCategory.MISC).sized(0.9f, 0.5f).fireImmune().clientTrackingRange(8)
                    .build("smoke_phantom"));
    public static final DeferredHolder<EntityType<?>, EntityType<HookahGhost>> HOOKAH_GHOST = ENTITY_TYPES.register("hookah_ghost",
            () -> EntityType.Builder.of(HookahGhost::new, MobCategory.MISC).sized(0.6f, 1.95f).fireImmune().clientTrackingRange(8)
                    .build("hookah_ghost"));

    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<BestiaryLootModifier>> BESTIARY =
            LOOT_MODIFIERS.register("bestiary", () -> BestiaryLootModifier.CODEC);

    public static final DeferredHolder<SoundEvent, SoundEvent> SCAN_REVEAL = sound("scan_reveal");
    public static final DeferredHolder<SoundEvent, SoundEvent> RESEARCH_COMPLETE = sound("research_complete");
    public static final DeferredHolder<SoundEvent, SoundEvent> TREATISE_WRITE = sound("treatise_write");
    public static final DeferredHolder<SoundEvent, SoundEvent> GHOST_OFFER = sound("ghost_offer");
    public static final DeferredHolder<SoundEvent, SoundEvent> GHOST_DEAL = sound("ghost_deal");
    public static final DeferredHolder<SoundEvent, SoundEvent> COMA_FADE = sound("coma_fade");
    public static final DeferredHolder<SoundEvent, SoundEvent> DREAM_INSPIRATION = sound("dream_inspiration");
    public static final DeferredHolder<SoundEvent, SoundEvent> DREAM_COMPASS = sound("dream_compass");
    public static final DeferredHolder<SoundEvent, SoundEvent> DREAM_RECIPE = sound("dream_recipe");
    public static final DeferredHolder<SoundEvent, SoundEvent> DREAM_INSIGHT = sound("dream_insight");
    public static final DeferredHolder<SoundEvent, SoundEvent> DREAM_LUCID = sound("dream_lucid");
    public static final DeferredHolder<SoundEvent, SoundEvent> AWAKEN_NIGHTMARE = sound("awaken_nightmare");
    public static final DeferredHolder<SoundEvent, SoundEvent> ENLIGHTENMENT = sound("enlightenment");

    public static final DeferredHolder<ResourceLocation, ResourceLocation> GRAMS_SMOKED = stat("grams_smoked");
    public static final DeferredHolder<ResourceLocation, ResourceLocation> PUFFS = stat("puffs");
    public static final DeferredHolder<ResourceLocation, ResourceLocation> CASTS = stat("casts");
    public static final DeferredHolder<ResourceLocation, ResourceLocation> ZONE_MINUTES = stat("zone_minutes");
    public static final DeferredHolder<ResourceLocation, ResourceLocation> OVERDOSES = stat("overdoses");
    public static final DeferredHolder<ResourceLocation, ResourceLocation> NOTES_OPENED = stat("notes_opened");
    public static final DeferredHolder<ResourceLocation, ResourceLocation> HYBRIDS_FOUND = stat("hybrids_found");

    private MindRegistry() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, name)));
    }

    private static DeferredHolder<ResourceLocation, ResourceLocation> stat(String name) {
        return CUSTOM_STATS.register(name, () -> ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, name));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);
        BLOCK_ENTITIES.register(eventBus);
        ENTITY_TYPES.register(eventBus);
        MENUS.register(eventBus);
        SOUNDS.register(eventBus);
        DATA_COMPONENTS.register(eventBus);
        CUSTOM_STATS.register(eventBus);
        LOOT_MODIFIERS.register(eventBus);
        eventBus.addListener(EntityAttributeCreationEvent.class, event -> {
            event.put(SMOKE_PHANTOM.get(), SmokePhantom.createAttributes().build());
            event.put(HOOKAH_GHOST.get(), HookahGhost.createAttributes().build());
        });
    }
}
