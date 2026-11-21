package net.sorukoma.quasium.golem;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.item.ModItems;
import net.sorukoma.quasium.magic.NoteType;
import net.sorukoma.quasium.recipe.AnvilRecipes;

import java.util.List;

public final class ClayGolems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(QuasiumMod.MOD_ID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, QuasiumMod.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<ClayGolemEntity>> CLAY_GOLEM = ENTITY_TYPES.register("clay_golem",
            () -> EntityType.Builder.<ClayGolemEntity>of(ClayGolemEntity::new, MobCategory.MISC).sized(0.8f, 1.2f)
                    .clientTrackingRange(10).build("clay_golem"));

    public static final DeferredItem<Item> CLAY_GOLEM_ITEM = ITEMS.register("clay_golem",
            () -> new ClayGolemItem(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> CLAY_GOLEM_SPAWN_EGG = ITEMS.register("clay_golem_spawn_egg",
            () -> new DeferredSpawnEggItem(CLAY_GOLEM, 0xB5673F, 0x9FD3C7, new Item.Properties()));
    public static final DeferredItem<Item> SOMMELIER_MODULE = ITEMS.register("sommelier_module",
            () -> new ClayModuleItem(new Item.Properties().stacksTo(16), "sommelier", ClayGolemEntity::becomeSommelier));
    public static final DeferredItem<Item> TOOLKEEPER_MODULE = ITEMS.register("toolkeeper_module",
            () -> new ClayModuleItem(new Item.Properties().stacksTo(16), "toolkeeper", ClayGolemEntity::becomeToolkeeper));

    public static final TagKey<Item> JUNK = ItemTags.create(ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "golem_junk"));

    private ClayGolems() {
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
        ENTITY_TYPES.register(eventBus);
        eventBus.addListener(ClayGolems::onAttributes);
        eventBus.addListener(ClayGolems::onCommonSetup);
        NeoForge.EVENT_BUS.addListener(LivingDropsEvent.class, HerderGoal::collectDrops);
        NeoForge.EVENT_BUS.addListener(LivingIncomingDamageEvent.class, GolemNap::onHurt);
    }

    private static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(CLAY_GOLEM.get(), ClayGolemEntity.createAttributes().build());
    }

    private static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            AnvilRecipes.add(List.of(() -> Items.CLAY, () -> ModItems.ESSENCE_BOTTLE.get()), CLAY_GOLEM_ITEM, 4, 30,
                    AnvilRecipes.noteBottleIn(NoteType.RESIN, 1));
            AnvilRecipes.add(List.of(() -> Items.GOLDEN_CARROT, () -> Items.AMETHYST_SHARD, () -> ModItems.ESSENCE_BOTTLE.get()),
                    SOMMELIER_MODULE, 3, 40, AnvilRecipes.noteBottleIn(NoteType.RESIN, 2));
            AnvilRecipes.add(List.of(() -> Items.CHEST, () -> Items.IRON_INGOT, () -> ModItems.ESSENCE_BOTTLE.get()),
                    TOOLKEEPER_MODULE, 3, 40, AnvilRecipes.noteBottleIn(NoteType.EMBER, 2));
        });
    }
}
