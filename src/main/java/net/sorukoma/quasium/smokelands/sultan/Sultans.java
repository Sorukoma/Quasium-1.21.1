package net.sorukoma.quasium.smokelands.sultan;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.smokelands.SmokelandsRegistry;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID)
public final class Sultans {
    public static final DeferredHolder<EntityType<?>, EntityType<SultanOfSmoke>> SULTAN = SmokelandsRegistry.ENTITY_TYPES.register(
            "sultan_of_smoke", () -> EntityType.Builder.of(SultanOfSmoke::new, MobCategory.MONSTER).sized(2.2f, 4.2f).fireImmune()
                    .clientTrackingRange(12).build("sultan_of_smoke"));
    public static final DeferredHolder<EntityType<?>, EntityType<SultanRing>> RING = SmokelandsRegistry.ENTITY_TYPES.register(
            "sultan_ring", () -> EntityType.Builder.<SultanRing>of(SultanRing::new, MobCategory.MISC).sized(1.6f, 1.6f).fireImmune()
                    .noSummon().clientTrackingRange(8).updateInterval(2).build("sultan_ring"));
    public static final DeferredBlock<SultanThroneBlock> THRONE = SmokelandsRegistry.BLOCKS.register("sultan_throne",
            () -> new SultanThroneBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(-1f, 3600000f)
                    .sound(SoundType.NETHERITE_BLOCK).lightLevel(state -> 7).noLootTable().pushReaction(PushReaction.BLOCK)));
    public static final DeferredItem<BlockItem> THRONE_ITEM = SmokelandsRegistry.ITEMS.registerSimpleBlockItem(THRONE,
            new Item.Properties().rarity(Rarity.EPIC));
    public static final DeferredItem<DeferredSpawnEggItem> SULTAN_EGG = SmokelandsRegistry.ITEMS.register("sultan_of_smoke_spawn_egg",
            () -> new DeferredSpawnEggItem(SULTAN, 0x352452, 0xFF9A3C, new Item.Properties()));

    private Sultans() {
    }

    public static void load() {
    }

    @SubscribeEvent
    public static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(SULTAN.get(), SultanOfSmoke.createAttributes().build());
    }
}
