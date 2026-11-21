package net.sorukoma.quasium.smokelands.mob;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.smokelands.SmokelandsRegistry;
import net.sorukoma.quasium.smokelands.fluid.SmokeFluids;

import java.util.UUID;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID)
public final class SmokelandsEntities {
    public static final DeferredHolder<EntityType<?>, EntityType<MistPhantom>> MIST_PHANTOM = SmokelandsRegistry.ENTITY_TYPES.register(
            "mist_phantom", () -> EntityType.Builder.of(MistPhantom::new, MobCategory.MONSTER).sized(0.9f, 0.5f).fireImmune()
                    .clientTrackingRange(8).build("mist_phantom"));
    public static final DeferredHolder<EntityType<?>, EntityType<TarSlime>> TAR_SLIME = SmokelandsRegistry.ENTITY_TYPES.register(
            "tar_slime", () -> EntityType.Builder.of(TarSlime::new, MobCategory.MONSTER).sized(0.52f, 0.52f).eyeHeight(0.325f)
                    .spawnDimensionsScale(4f).fireImmune().clientTrackingRange(10).build("tar_slime"));
    public static final DeferredHolder<EntityType<?>, EntityType<DraftWisp>> DRAFT_WISP = SmokelandsRegistry.ENTITY_TYPES.register(
            "draft_wisp", () -> EntityType.Builder.of(DraftWisp::new, MobCategory.AMBIENT).sized(0.4f, 0.4f).fireImmune()
                    .clientTrackingRange(8).build("draft_wisp"));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UUID>> FLUTE_WISP =
            SmokelandsRegistry.DATA_COMPONENTS.registerComponentType("flute_wisp",
                    builder -> builder.persistent(UUIDUtil.CODEC).networkSynchronized(UUIDUtil.STREAM_CODEC));
    public static final DeferredItem<Item> WISP_FLUTE = SmokelandsRegistry.ITEMS.register("wisp_flute",
            () -> new WispFluteItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final DeferredBlock<WispLightBlock> WISP_LIGHT = SmokelandsRegistry.BLOCKS.register("wisp_light",
            () -> new WispLightBlock(BlockBehaviour.Properties.of().replaceable().noCollission().noLootTable().instabreak().air()
                    .lightLevel(state -> state.getValue(LightBlock.LEVEL)).pushReaction(PushReaction.DESTROY)));

    public static final DeferredItem<DeferredSpawnEggItem> MIST_PHANTOM_EGG = egg("mist_phantom", MIST_PHANTOM, 0x5E5868, 0xB9B2C9);
    public static final DeferredItem<DeferredSpawnEggItem> TAR_SLIME_EGG = egg("tar_slime", TAR_SLIME, 0x1E1714, 0xC9822E);
    public static final DeferredItem<DeferredSpawnEggItem> DRAFT_WISP_EGG = egg("draft_wisp", DRAFT_WISP, 0x9FD8FF, 0xF2FBFF);

    private SmokelandsEntities() {
    }

    public static void load() {
    }

    private static DeferredItem<DeferredSpawnEggItem> egg(String name, DeferredHolder<EntityType<?>, ? extends EntityType<? extends Mob>> type,
                                                          int base, int spots) {
        return SmokelandsRegistry.ITEMS.register(name + "_spawn_egg", () -> new DeferredSpawnEggItem(type, base, spots, new Item.Properties()));
    }

    static void setHealth(LivingEntity entity, int health) {
        entity.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
        entity.setHealth(health);
    }

    @SubscribeEvent
    public static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(MIST_PHANTOM.get(), MistPhantom.createAttributes().build());
        event.put(TAR_SLIME.get(), TarSlime.createAttributes().build());
        event.put(DRAFT_WISP.get(), DraftWisp.createAttributes().build());
    }

    @SubscribeEvent
    public static void onSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(MIST_PHANTOM.get(), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                SmokelandsEntities::overSmoke, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(TAR_SLIME.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                SmokelandsEntities::hostile, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(DRAFT_WISP.get(), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> level.getBlockState(pos).isAir(), RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    private static boolean overSmoke(EntityType<MistPhantom> type, ServerLevelAccessor level, MobSpawnType reason, BlockPos pos,
                                     RandomSource random) {
        return level.getDifficulty() != Difficulty.PEACEFUL && level.getBlockState(pos).isAir()
                && level.getFluidState(pos.below()).getFluidType() == SmokeFluids.TYPE.get();
    }

    private static <T extends Mob> boolean hostile(EntityType<T> type, ServerLevelAccessor level, MobSpawnType reason, BlockPos pos,
                                                   RandomSource random) {
        return level.getDifficulty() != Difficulty.PEACEFUL && Mob.checkMobSpawnRules(type, level, reason, pos, random);
    }
}
