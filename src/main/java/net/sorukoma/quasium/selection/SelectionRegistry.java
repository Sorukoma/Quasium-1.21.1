package net.sorukoma.quasium.selection;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.GeodeConfiguration;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.common.util.DeferredSoundType;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.block.ModBlocks;
import net.sorukoma.quasium.selection.geode.BuddingQuasiumBlock;
import net.sorukoma.quasium.selection.geode.QuasiumClusterBlock;
import net.sorukoma.quasium.selection.geode.QuasiumCrustBlock;
import net.sorukoma.quasium.selection.geode.QuasiumGeodeFeature;
import net.sorukoma.quasium.selection.quality.SecretRecipeItem;
import net.sorukoma.quasium.selection.trade.SmokeCaravanTrader;
import net.sorukoma.quasium.sound.ModSoundEvents;

import java.util.List;
import java.util.function.Supplier;

public final class SelectionRegistry {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(QuasiumMod.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(QuasiumMod.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, QuasiumMod.MOD_ID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, QuasiumMod.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, QuasiumMod.MOD_ID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, QuasiumMod.MOD_ID);
    public static final DeferredRegister.DataComponents DATA_COMPONENTS = DeferredRegister.createDataComponents(QuasiumMod.MOD_ID);
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, QuasiumMod.MOD_ID);
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, QuasiumMod.MOD_ID);
    public static final DeferredRegister<PoiType> POI_TYPES = DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, QuasiumMod.MOD_ID);
    public static final DeferredRegister<VillagerProfession> PROFESSIONS = DeferredRegister.create(Registries.VILLAGER_PROFESSION, QuasiumMod.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<String>>> TOBACCO_LINEAGE =
            DATA_COMPONENTS.registerComponentType("tobacco_lineage", builder -> builder
                    .persistent(Codec.STRING.listOf())
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list())));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> LEAF_QUALITY =
            DATA_COMPONENTS.registerComponentType("leaf_quality", builder -> builder
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> KAIF_PCT =
            DATA_COMPONENTS.registerComponentType("kaif_pct", builder -> builder
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<String>>> SECRET_MIX =
            DATA_COMPONENTS.registerComponentType("secret_mix", builder -> builder
                    .persistent(Codec.STRING.listOf())
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list())));

    public static final DeferredHolder<PoiType, PoiType> HOOKAH_MASTER_POI = POI_TYPES.register("hookah_master",
            () -> new PoiType(ImmutableSet.copyOf(ModBlocks.ESSENCE_PRESS.get().getStateDefinition().getPossibleStates()), 1, 1));
    public static final DeferredHolder<VillagerProfession, VillagerProfession> HOOKAH_MASTER = PROFESSIONS.register("hookah_master",
            () -> new VillagerProfession("hookah_master", poi -> poi.is(HOOKAH_MASTER_POI.getKey()), poi -> poi.is(HOOKAH_MASTER_POI.getKey()),
                    ImmutableSet.of(), ImmutableSet.of(), ModSoundEvents.VILLAGER_HOOKAH_MASTER_WORK.get()));

    public static final DeferredHolder<EntityType<?>, EntityType<SmokeCaravanTrader>> SMOKE_CARAVAN = ENTITY_TYPES.register("smoke_caravan",
            () -> EntityType.Builder.<SmokeCaravanTrader>of(SmokeCaravanTrader::new, MobCategory.CREATURE).sized(0.6f, 1.95f)
                    .eyeHeight(1.62f).clientTrackingRange(10).build("smoke_caravan"));

    public static final DeferredItem<Item> SECRET_RECIPE = ITEMS.register("secret_recipe",
            () -> new SecretRecipeItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> SMOKE_CARAVAN_SPAWN_EGG = ITEMS.register("smoke_caravan_spawn_egg",
            () -> new DeferredSpawnEggItem(SMOKE_CARAVAN, 0x4A3A5C, 0xE08A3C, new Item.Properties()));

    public static final Supplier<AttachmentType<List<String>>> KNOWN_HYBRIDS = ATTACHMENTS.register("known_hybrids",
            () -> AttachmentType.<List<String>>builder(() -> List.of()).serialize(Codec.STRING.listOf()).copyOnDeath().build());
    public static final Supplier<AttachmentType<Long>> CARAVAN_DEPARTURE = ATTACHMENTS.register("caravan_departure",
            () -> AttachmentType.builder(() -> 0L).serialize(Codec.LONG).sync(ByteBufCodecs.VAR_LONG).build());

    private static final SoundType CLUSTER_SOUND = new DeferredSoundType(1.0F, 1.0F, ModSoundEvents.CLUSTER_BREAK,
            () -> SoundEvents.AMETHYST_CLUSTER_STEP, () -> SoundEvents.AMETHYST_CLUSTER_PLACE,
            () -> SoundEvents.AMETHYST_CLUSTER_HIT, () -> SoundEvents.AMETHYST_CLUSTER_FALL);

    public static final DeferredBlock<Block> QUASIUM_CRUST = withItem("quasium_crust", () -> new QuasiumCrustBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE).instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresCorrectToolForDrops().strength(3.0F, 6.0F).sound(SoundType.DEEPSLATE)));
    public static final DeferredBlock<Block> BUDDING_QUASIUM = withItem("budding_quasium", () -> new BuddingQuasiumBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).randomTicks().strength(1.5F).sound(SoundType.AMETHYST)
                    .requiresCorrectToolForDrops().pushReaction(PushReaction.DESTROY).noLootTable()));
    public static final DeferredBlock<Block> SMALL_QUASIUM_BUD = withItem("small_quasium_bud",
            () -> new AmethystClusterBlock(3.0F, 4.0F, crystal(SoundType.SMALL_AMETHYST_BUD, 1)));
    public static final DeferredBlock<Block> MEDIUM_QUASIUM_BUD = withItem("medium_quasium_bud",
            () -> new AmethystClusterBlock(4.0F, 3.0F, crystal(SoundType.MEDIUM_AMETHYST_BUD, 2)));
    public static final DeferredBlock<Block> LARGE_QUASIUM_BUD = withItem("large_quasium_bud",
            () -> new AmethystClusterBlock(5.0F, 3.0F, crystal(SoundType.LARGE_AMETHYST_BUD, 4)));
    public static final DeferredBlock<Block> QUASIUM_CLUSTER = withItem("quasium_cluster",
            () -> new QuasiumClusterBlock(7.0F, 3.0F, crystal(CLUSTER_SOUND, 5).requiresCorrectToolForDrops().noLootTable()));
    public static final List<DeferredBlock<Block>> BUD_STAGES = List.of(SMALL_QUASIUM_BUD, MEDIUM_QUASIUM_BUD, LARGE_QUASIUM_BUD, QUASIUM_CLUSTER);

    public static final DeferredHolder<Feature<?>, QuasiumGeodeFeature> QUASIUM_GEODE =
            FEATURES.register("quasium_geode", () -> new QuasiumGeodeFeature(GeodeConfiguration.CODEC));

    private SelectionRegistry() {
    }

    private static DeferredBlock<Block> withItem(String name, Supplier<Block> block) {
        DeferredBlock<Block> registered = BLOCKS.register(name, block);
        ITEMS.registerSimpleBlockItem(registered);
        return registered;
    }

    private static BlockBehaviour.Properties crystal(SoundType sound, int light) {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).forceSolidOn().noOcclusion().sound(sound)
                .strength(1.5F).lightLevel(state -> light).pushReaction(PushReaction.DESTROY);
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);
        BLOCK_ENTITIES.register(eventBus);
        ENTITY_TYPES.register(eventBus);
        MENUS.register(eventBus);
        SOUNDS.register(eventBus);
        DATA_COMPONENTS.register(eventBus);
        ATTACHMENTS.register(eventBus);
        FEATURES.register(eventBus);
        POI_TYPES.register(eventBus);
        PROFESSIONS.register(eventBus);
        eventBus.addListener(SelectionRegistry::onAttributes);
    }

    private static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(SMOKE_CARAVAN.get(), DefaultAttributes.getSupplier(EntityType.WANDERING_TRADER));
    }
}
