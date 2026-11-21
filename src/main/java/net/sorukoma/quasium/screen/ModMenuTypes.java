package net.sorukoma.quasium.screen;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.sorukoma.quasium.QuasiumMod;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, QuasiumMod.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<HookahMenu>> HOOKAH_MENU =
            MENUS.register("hookah_menu", () -> IMenuTypeExtension.create(HookahMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<BackpackHookahMenu>> BACKPACK_HOOKAH_MENU =
            MENUS.register("backpack_hookah_menu", () -> IMenuTypeExtension.create((id, inv, buf) -> new BackpackHookahMenu(id, inv)));

    public static final DeferredHolder<MenuType<?>, MenuType<HookahBowlMenu>> HOOKAH_BOWL_MENU =
            MENUS.register("hookah_bowl_menu", () -> IMenuTypeExtension.create(HookahBowlMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<HeaterMenu>> HEATER_MENU =
            MENUS.register("heater_menu", () -> IMenuTypeExtension.create((id, inv, buf) -> new HeaterMenu(id, inv, buf)));

    public static final DeferredHolder<MenuType<?>, MenuType<CalloudMenu>> CALLOUD_MENU =
            MENUS.register("calloud_menu", () -> IMenuTypeExtension.create((id, inv, buf) -> new CalloudMenu(id, inv, buf)));

    public static final DeferredHolder<MenuType<?>, MenuType<DistillationCubeMenu>> DISTILLATION_CUBE_MENU =
            MENUS.register("distillation_cube_menu", () -> IMenuTypeExtension.create(DistillationCubeMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<GolemMenu>> GOLEM_MENU =
            MENUS.register("golem_menu", () -> IMenuTypeExtension.create(GolemMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<GolemFeederMenu>> GOLEM_FEEDER_MENU =
            MENUS.register("golem_feeder_menu", () -> IMenuTypeExtension.create((id, inv, buf) -> new GolemFeederMenu(id, inv)));

    public static final DeferredHolder<MenuType<?>, MenuType<ProgramTableMenu>> PROGRAM_TABLE_MENU =
            MENUS.register("program_table_menu", () -> IMenuTypeExtension.create(ProgramTableMenu::new));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}