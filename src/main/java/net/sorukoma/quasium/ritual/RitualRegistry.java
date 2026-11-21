package net.sorukoma.quasium.ritual;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.progression.ConfigArgs;
import net.sorukoma.quasium.ritual.artifact.ArtifactItem;
import net.sorukoma.quasium.ritual.artifact.DreamCatcherBlock;
import net.sorukoma.quasium.ritual.artifact.DreamCatcherBlockEntity;
import net.sorukoma.quasium.ritual.artifact.EternalCoalItem;
import net.sorukoma.quasium.ritual.artifact.SignalPipeItem;
import net.sorukoma.quasium.ritual.brazier.RitualBrazierBlock;
import net.sorukoma.quasium.ritual.brazier.RitualBrazierBlockEntity;
import net.sorukoma.quasium.ritual.brazier.RitualPedestalBlock;
import net.sorukoma.quasium.ritual.brazier.RitualPedestalBlockEntity;

public final class RitualRegistry {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(QuasiumMod.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(QuasiumMod.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, QuasiumMod.MOD_ID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, QuasiumMod.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, QuasiumMod.MOD_ID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, QuasiumMod.MOD_ID);
    public static final DeferredRegister.DataComponents DATA_COMPONENTS = DeferredRegister.createDataComponents(QuasiumMod.MOD_ID);
    public static final DeferredRegister<IngredientType<?>> INGREDIENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.INGREDIENT_TYPES, QuasiumMod.MOD_ID);

    public static final DeferredBlock<RitualBrazierBlock> RITUAL_BRAZIER = BLOCKS.register("ritual_brazier",
            () -> new RitualBrazierBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(5f, 6f)
                    .requiresCorrectToolForDrops().sound(SoundType.METAL).noOcclusion().pushReaction(PushReaction.BLOCK)
                    .lightLevel(state -> state.getValue(RitualBrazierBlock.LIT) ? 13 : 3)));
    public static final DeferredBlock<RitualPedestalBlock> RITUAL_PEDESTAL = BLOCKS.register("ritual_pedestal",
            () -> new RitualPedestalBlock(BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE).strength(3f, 6f)
                    .requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE_BRICKS).noOcclusion()));
    public static final DeferredBlock<DreamCatcherBlock> DREAM_CATCHER = BLOCKS.register("dream_catcher",
            () -> new DreamCatcherBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(0.5f)
                    .sound(SoundType.WOOL).noOcclusion().noCollission().lightLevel(state -> 4)));

    public static final DeferredItem<BlockItem> RITUAL_BRAZIER_ITEM = ITEMS.registerSimpleBlockItem(RITUAL_BRAZIER);
    public static final DeferredItem<BlockItem> RITUAL_PEDESTAL_ITEM = ITEMS.registerSimpleBlockItem(RITUAL_PEDESTAL);
    public static final DeferredItem<BlockItem> DREAM_CATCHER_ITEM = ITEMS.register("dream_catcher",
            () -> new BlockItem(DREAM_CATCHER.get(), new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<EternalCoalItem> ETERNAL_COAL = ITEMS.register("eternal_coal",
            () -> new EternalCoalItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));
    public static final DeferredItem<ArtifactItem> BOTTOMLESS_FLASK = ITEMS.register("bottomless_flask",
            () -> new ArtifactItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC),
                    () -> new Object[]{ConfigArgs.synced(RitualConfig.BOTTOMLESS_FLASK_KAIF_PER_CAST)}));
    public static final DeferredItem<ArtifactItem> VOID_PIPE = ITEMS.register("void_pipe",
            () -> new ArtifactItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE),
                    () -> new Object[]{ConfigArgs.synced(RitualConfig.VOID_PIPE_LOW_BOWL_GRAMS),
                            ConfigArgs.synced(RitualConfig.VOID_PIPE_HP_PER_CAST)}));
    public static final DeferredItem<SignalPipeItem> SIGNAL_PIPE = ITEMS.register("signal_pipe",
            () -> new SignalPipeItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<ArtifactItem> DREAM_QUARTZ = ITEMS.register("dream_quartz",
            () -> new ArtifactItem(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<ArtifactItem> SMOKE_PORTAL_ACTIVATOR = ITEMS.register("smoke_portal_activator",
            () -> new ArtifactItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RitualBrazierBlockEntity>> RITUAL_BRAZIER_BE =
            BLOCK_ENTITIES.register("ritual_brazier_be",
                    () -> BlockEntityType.Builder.of(RitualBrazierBlockEntity::new, RITUAL_BRAZIER.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RitualPedestalBlockEntity>> RITUAL_PEDESTAL_BE =
            BLOCK_ENTITIES.register("ritual_pedestal_be",
                    () -> BlockEntityType.Builder.of(RitualPedestalBlockEntity::new, RITUAL_PEDESTAL.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DreamCatcherBlockEntity>> DREAM_CATCHER_BE =
            BLOCK_ENTITIES.register("dream_catcher_be",
                    () -> BlockEntityType.Builder.of(DreamCatcherBlockEntity::new, DREAM_CATCHER.get()).build(null));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Unit>> QUASIUM_HARNESS = marker("quasium_harness");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Unit>> SMOKE_CARAPACE = marker("smoke_carapace");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Unit>> SMOKESTEP = marker("smokestep");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Unit>> SMOKE_THREAD = marker("smoke_thread");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Unit>> SMOKE_TEMPERED = marker("smoke_tempered");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Float>> CARAPACE_CHARGE =
            DATA_COMPONENTS.registerComponentType("carapace_charge",
                    builder -> builder.persistent(Codec.floatRange(0f, Float.MAX_VALUE)).networkSynchronized(ByteBufCodecs.FLOAT));

    public static final DeferredHolder<IngredientType<?>, IngredientType<GearIngredient>> GEAR = INGREDIENT_TYPES.register("gear",
            () -> new IngredientType<>(GearIngredient.CODEC, GearIngredient.STREAM_CODEC));

    public static final DeferredHolder<SoundEvent, SoundEvent> RITUAL_LOOP = sound("ritual_loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> RITUAL_PULSE = sound("ritual_pulse");
    public static final DeferredHolder<SoundEvent, SoundEvent> RITUAL_COMPLETE = sound("ritual_complete");
    public static final DeferredHolder<SoundEvent, SoundEvent> RITUAL_FAIL = sound("ritual_fail");
    public static final DeferredHolder<SoundEvent, SoundEvent> ETERNAL_COAL_IDLE = sound("eternal_coal_idle");
    public static final DeferredHolder<SoundEvent, SoundEvent> PORTAL_OPEN = sound("portal_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> ARTIFACT_EQUIP = sound("artifact_equip");

    private RitualRegistry() {
    }

    private static DeferredHolder<DataComponentType<?>, DataComponentType<Unit>> marker(String name) {
        return DATA_COMPONENTS.registerComponentType(name,
                builder -> builder.persistent(Unit.CODEC).networkSynchronized(StreamCodec.unit(Unit.INSTANCE)));
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, name)));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);
        BLOCK_ENTITIES.register(eventBus);
        ENTITY_TYPES.register(eventBus);
        MENUS.register(eventBus);
        SOUNDS.register(eventBus);
        DATA_COMPONENTS.register(eventBus);
        INGREDIENT_TYPES.register(eventBus);
    }
}
