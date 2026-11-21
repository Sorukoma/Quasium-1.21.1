package net.sorukoma.quasium.golem;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.item.ModItems;
import net.sorukoma.quasium.recipe.AnvilRecipes;

import java.util.List;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID)
public final class CalloudGolems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(QuasiumMod.MOD_ID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, QuasiumMod.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<CalloudGolemEntity>> CALLOUD_GOLEM = ENTITY_TYPES.register("calloud_golem",
            () -> EntityType.Builder.of(CalloudGolemEntity::new, MobCategory.MISC)
                    .sized(0.6f, 0.8f)
                    .fireImmune()
                    .clientTrackingRange(10)
                    .build("calloud_golem"));

    public static final DeferredItem<Item> CALLOUD_GOLEM_ITEM = ITEMS.register("calloud_golem",
            () -> new CalloudGolemItem(new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> CALLOUD_GOLEM_SPAWN_EGG = ITEMS.register("calloud_golem_spawn_egg",
            () -> new DeferredSpawnEggItem(CALLOUD_GOLEM, 0x2B2320, 0xFF8A2A, new Item.Properties()));

    private CalloudGolems() {
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
        ENTITY_TYPES.register(eventBus);
    }

    @SubscribeEvent
    public static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(CALLOUD_GOLEM.get(), CalloudGolemEntity.createAttributes().build());
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> AnvilRecipes.add(List.of(ModItems.CALLOUD::get, () -> Items.CLAY), CALLOUD_GOLEM_ITEM, 4, 40));
    }
}
