package net.sorukoma.quasium.entity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.sorukoma.quasium.QuasiumMod;

public class ModEntityTypes {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, QuasiumMod.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<HookahConnectionEntity>> HOOKAH_CONNECTION =
            ENTITY_TYPES.register("hookah_connection", () -> EntityType.Builder.<HookahConnectionEntity>of(HookahConnectionEntity::new, MobCategory.MISC)
                    .sized(0.1F, 0.1F) // Очень маленький размер, так как сущность невидима
                    .setShouldReceiveVelocityUpdates(false) // Не нужно обновлять скорость
                    .setTrackingRange(64) // Дальность отслеживания
                    .setUpdateInterval(20) // Обновление каждые 20 тиков (1 секунда)
                    .build("hookah_connection"));

    public static final DeferredHolder<EntityType<?>, EntityType<ToxicSmokeEntity>> TOXIC_SMOKE =
            ENTITY_TYPES.register("toxic_smoke", () -> EntityType.Builder.<ToxicSmokeEntity>of(ToxicSmokeEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .setShouldReceiveVelocityUpdates(true)
                    .setTrackingRange(64)
                    .setUpdateInterval(1)
                    .build("toxic_smoke"));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}