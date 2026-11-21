package net.sorukoma.quasium.magic.page;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.sorukoma.quasium.QuasiumMod;

public final class PageRegistry {
    public static final int SMALL_PAGES = 8;
    public static final int LARGE_PAGES = 16;

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(QuasiumMod.MOD_ID);
    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, QuasiumMod.MOD_ID);
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, QuasiumMod.MOD_ID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, QuasiumMod.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<TetradContents>> TETRAD =
            DATA_COMPONENTS.registerComponentType("tetrad", builder -> builder
                    .persistent(TetradContents.CODEC)
                    .networkSynchronized(TetradContents.STREAM_CODEC)
                    .cacheEncoding());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SpellPage>> PAGE =
            DATA_COMPONENTS.registerComponentType("page", builder -> builder
                    .persistent(SpellPage.CODEC)
                    .networkSynchronized(SpellPage.STREAM_CODEC));

    public static final DeferredItem<SpellTetradItem> SPELL_TETRAD = ITEMS.register("spell_tetrad",
            () -> new SpellTetradItem(new Item.Properties(), SMALL_PAGES));
    public static final DeferredItem<SpellTetradItem> LARGE_SPELL_TETRAD = ITEMS.register("large_spell_tetrad",
            () -> new SpellTetradItem(new Item.Properties().rarity(Rarity.EPIC).fireResistant(), LARGE_PAGES));

    public static final DeferredItem<Item> TORN_PAGE = ITEMS.register("torn_page",
            () -> new Item(new Item.Properties().stacksTo(1).component(PAGE, SpellPage.EMPTY)));

    public static final DeferredHolder<MobEffect, MobEffect> SOAKED = EFFECTS.register("soaked",
            () -> new PageEffectMarker(MobEffectCategory.NEUTRAL, 0x99DDFF));
    public static final DeferredHolder<MobEffect, MobEffect> CHOKING = EFFECTS.register("choking", ChokingEffect::new);
    public static final DeferredHolder<MobEffect, MobEffect> GROUNDED = EFFECTS.register("grounded", Grounding.GroundedEffect::new);

    public static final DeferredHolder<EntityType<?>, EntityType<SmokeBoltEntity>> SMOKE_BOLT =
            ENTITY_TYPES.register("smoke_bolt", () -> EntityType.Builder.<SmokeBoltEntity>of(SmokeBoltEntity::new, MobCategory.MISC)
                    .sized(0.4f, 0.4f)
                    .noSave()
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .build("smoke_bolt"));

    private PageRegistry() {
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
        DATA_COMPONENTS.register(eventBus);
        EFFECTS.register(eventBus);
        ENTITY_TYPES.register(eventBus);
        eventBus.addListener(PagePackets::register);
    }
}
