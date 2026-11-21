package net.sorukoma.quasium.golem;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.sorukoma.quasium.Config;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.item.ModItems;
import net.sorukoma.quasium.magic.NoteType;
import net.sorukoma.quasium.recipe.AnvilRecipes;

import java.util.List;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID)
public final class WardenGolems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(QuasiumMod.MOD_ID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, QuasiumMod.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<SmokeWardenEntity>> SMOKE_WARDEN =
            ENTITY_TYPES.register("smoke_warden", () -> EntityType.Builder.<SmokeWardenEntity>of(SmokeWardenEntity::new, MobCategory.MISC)
                    .sized(0.9f, 1.3f)
                    .eyeHeight(1.2f)
                    .clientTrackingRange(10)
                    .build("smoke_warden"));

    public static final DeferredHolder<EntityType<?>, EntityType<WardenShotEntity>> WARDEN_SHOT =
            ENTITY_TYPES.register("warden_shot", () -> EntityType.Builder.<WardenShotEntity>of(WardenShotEntity::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f)
                    .noSave()
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build("warden_shot"));

    public static final DeferredItem<Item> WARDEN_BLANK = ITEMS.registerSimpleItem("warden_blank");
    public static final DeferredItem<Item> SMOKE_WARDEN_ITEM = ITEMS.register("smoke_warden",
            () -> new SmokeWardenItem(new Item.Properties()));
    public static final DeferredItem<Item> SMOKE_WARDEN_SPAWN_EGG = ITEMS.register("smoke_warden_spawn_egg",
            () -> new DeferredSpawnEggItem(SMOKE_WARDEN, 0xB5673F, 0x6E4AA8, new Item.Properties()));

    private WardenGolems() {
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
        ENTITY_TYPES.register(eventBus);
    }

    @SubscribeEvent
    public static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(SMOKE_WARDEN.get(), SmokeWardenEntity.createAttributes().build());
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> AnvilRecipes.add(List.of(WARDEN_BLANK::get, ModItems.ESSENCE_BOTTLE::get), SMOKE_WARDEN_ITEM, 6, 50,
                AnvilRecipes.noteBottleIn(NoteType.ASH, 1)));
    }

    @SubscribeEvent
    public static void onPlayerHurt(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof Player victim) || !(event.getSource().getEntity() instanceof Player attacker)) return;
        double range = Config.WARDEN_FIRE_RANGE.getAsInt();
        victim.level().getEntitiesOfClass(SmokeWardenEntity.class, victim.getBoundingBox().inflate(range),
                warden -> warden.distanceToSqr(victim) <= range * range).forEach(warden -> warden.standUpFor(victim, attacker));
    }
}
