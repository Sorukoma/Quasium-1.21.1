package net.sorukoma.quasium.screen;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder; // Импортируем DeferredHolder
import net.sorukoma.quasium.QuasiumMod;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, QuasiumMod.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<HookahMenu>> HOOKAH_MENU =
            MENUS.register("hookah_menu", () -> IMenuTypeExtension.create(HookahMenu::new));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
