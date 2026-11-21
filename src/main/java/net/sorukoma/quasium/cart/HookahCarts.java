package net.sorukoma.quasium.cart;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.sorukoma.quasium.QuasiumMod;

public final class HookahCarts {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(QuasiumMod.MOD_ID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, QuasiumMod.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, QuasiumMod.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<HookahMinecart>> HOOKAH_MINECART =
            ENTITY_TYPES.register("hookah_minecart", () -> EntityType.Builder.<HookahMinecart>of(HookahMinecart::new, MobCategory.MISC)
                    .sized(0.98f, 0.7f)
                    .passengerAttachments(0.1875f)
                    .clientTrackingRange(8)
                    .build("hookah_minecart"));
    public static final DeferredItem<Item> HOOKAH_MINECART_ITEM = ITEMS.register("hookah_minecart",
            () -> new HookahMinecartItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<MenuType<?>, MenuType<CartHookahMenu>> HOOKAH_MINECART_MENU =
            MENUS.register("hookah_minecart", () -> new MenuType<>(CartHookahMenu::new, FeatureFlags.VANILLA_SET));

    private HookahCarts() {
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
        ENTITY_TYPES.register(eventBus);
        MENUS.register(eventBus);
        eventBus.addListener(HookahCarts::registerPayloads);
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedOutEvent.class, event -> HookahMinecart.unhookAll(event.getEntity()));
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(CartPipePacket.ID, CartPipePacket.STREAM_CODEC, CartPipePacket::handle);
        registrar.playToClient(CartHosePacket.ID, CartHosePacket.STREAM_CODEC, CartHosePacket::handle);
    }
}
