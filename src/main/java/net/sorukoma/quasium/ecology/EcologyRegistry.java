package net.sorukoma.quasium.ecology;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.tea.SealBlocks;
import net.sorukoma.quasium.tea.TeaBlocks;

public final class EcologyRegistry {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(QuasiumMod.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(QuasiumMod.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, QuasiumMod.MOD_ID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, QuasiumMod.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, QuasiumMod.MOD_ID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, QuasiumMod.MOD_ID);
    public static final DeferredRegister.DataComponents DATA_COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, QuasiumMod.MOD_ID);
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, QuasiumMod.MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> SMOG_AMBIENCE = sound("smog_ambience");
    public static final DeferredHolder<SoundEvent, SoundEvent> SMOG_CLEANSE = sound("smog_cleanse");
    public static final DeferredHolder<SoundEvent, SoundEvent> DYMNIK_WHISPER = sound("dymnik_whisper");
    public static final DeferredHolder<SoundEvent, SoundEvent> SAMOVAR_BOIL = sound("samovar_boil");
    public static final DeferredHolder<SoundEvent, SoundEvent> TEA_POUR = sound("tea_pour");
    public static final DeferredHolder<SoundEvent, SoundEvent> TEA_PARTY_CHIME = sound("tea_party_chime");
    public static final DeferredHolder<SoundEvent, SoundEvent> SEAL_HUM = sound("seal_hum");

    private EcologyRegistry() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, name)));
    }

    public static void register(IEventBus eventBus) {
        SmogMobs.register(eventBus);
        TeaBlocks.register(eventBus);
        SealBlocks.register(eventBus);
        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);
        BLOCK_ENTITIES.register(eventBus);
        ENTITY_TYPES.register(eventBus);
        MENUS.register(eventBus);
        SOUNDS.register(eventBus);
        DATA_COMPONENTS.register(eventBus);
        EFFECTS.register(eventBus);
    }
}
