package net.sorukoma.quasium.ecology;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

public final class SmogMobs {
    public static final DeferredHolder<EntityType<?>, EntityType<DymnikEntity>> DYMNIK = EcologyRegistry.ENTITY_TYPES.register("dymnik",
            () -> EntityType.Builder.of(DymnikEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 0.9f)
                    .eyeHeight(0.6f)
                    .clientTrackingRange(8)
                    .build("dymnik"));

    public static final DeferredHolder<EntityType<?>, EntityType<GarevshchikEntity>> GAREVSHCHIK =
            EcologyRegistry.ENTITY_TYPES.register("garevshchik",
                    () -> EntityType.Builder.of(GarevshchikEntity::new, MobCategory.MONSTER)
                            .sized(0.7f, 1.9f)
                            .fireImmune()
                            .clientTrackingRange(8)
                            .build("garevshchik"));

    public static final DeferredItem<DeferredSpawnEggItem> DYMNIK_SPAWN_EGG = EcologyRegistry.ITEMS.register("dymnik_spawn_egg",
            () -> new DeferredSpawnEggItem(DYMNIK, 0x6E6A70, 0xC9C4CC, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> GAREVSHCHIK_SPAWN_EGG = EcologyRegistry.ITEMS.register("garevshchik_spawn_egg",
            () -> new DeferredSpawnEggItem(GAREVSHCHIK, 0x2A2224, 0xFF7A1E, new Item.Properties()));

    private SmogMobs() {
    }

    public static void register(IEventBus eventBus) {
        eventBus.addListener(SmogMobs::onAttributes);
    }

    private static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(DYMNIK.get(), DymnikEntity.createAttributes().build());
        event.put(GAREVSHCHIK.get(), GarevshchikEntity.createAttributes().build());
    }
}
