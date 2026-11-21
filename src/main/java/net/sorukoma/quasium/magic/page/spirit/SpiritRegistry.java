package net.sorukoma.quasium.magic.page.spirit;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.component.ModDataComponents;
import net.sorukoma.quasium.item.SelfMixedTobaccoItem;
import net.sorukoma.quasium.magic.SpellForm;
import net.sorukoma.quasium.magic.page.PageForms;
import net.sorukoma.quasium.selection.SelectionRegistry;
import net.sorukoma.quasium.selection.genetics.Genetics;

public final class SpiritRegistry {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(QuasiumMod.MOD_ID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, QuasiumMod.MOD_ID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, QuasiumMod.MOD_ID);

    public static final DeferredItem<Item> SPIRIT_MIX = ITEMS.register("spirit_mix", () -> new SelfMixedTobaccoItem(new Item.Properties()
            .stacksTo(1)
            .component(ModDataComponents.TOBACCO_BASE.get(), SpiritMix.BASE)
            .component(ModDataComponents.ESSENCE_TYPE.get(), SpiritMix.flavor(Genetics.MIN_STARS))
            .component(ModDataComponents.TOBACCO_WEIGHT.get(), SpiritMix.WEIGHT)
            .component(ModDataComponents.TOBACCO_DAMAGE.get(), SpiritMix.STRENGTH)
            .component(SelectionRegistry.LEAF_QUALITY.get(), Genetics.MIN_STARS)));

    public static final DeferredHolder<EntityType<?>, EntityType<HookahSpirit>> HOOKAH_SPIRIT = ENTITY_TYPES.register("hookah_spirit",
            () -> EntityType.Builder.<HookahSpirit>of(HookahSpirit::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f)
                    .fireImmune()
                    .noSave()
                    .noSummon()
                    .clientTrackingRange(8)
                    .updateInterval(2)
                    .build("hookah_spirit"));

    public static final DeferredHolder<SoundEvent, SoundEvent> SQUEAK = sound("hookah_spirit_squeak");
    public static final DeferredHolder<SoundEvent, SoundEvent> POP = sound("hookah_spirit_pop");

    private SpiritRegistry() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, name)));
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
        ENTITY_TYPES.register(eventBus);
        SOUNDS.register(eventBus);
        eventBus.addListener(EntityAttributeCreationEvent.class, event -> event.put(HOOKAH_SPIRIT.get(), HookahSpirit.createAttributes().build()));
        PageForms.register(SpellForm.SPIRIT, SpiritLaunch::launch);
    }
}
