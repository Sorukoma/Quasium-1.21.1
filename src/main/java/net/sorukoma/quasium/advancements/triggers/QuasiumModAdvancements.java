package net.sorukoma.quasium.advancements.triggers;

import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.Registries; // Добавлен импорт Registries
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.sorukoma.quasium.QuasiumMod;

import java.util.function.Supplier;

public class QuasiumModAdvancements {
    public static final DeferredRegister<CriterionTrigger<?>> CRITERION_TRIGGERS =
            DeferredRegister.create(Registries.TRIGGER_TYPE, QuasiumMod.MOD_ID); // Исправлено на Registries.TRIGGER_TYPE

    public static final Supplier<KaifLevelTrigger> KAIF_LEVEL = CRITERION_TRIGGERS.register("kaif_level", KaifLevelTrigger::new);

    public static void register(IEventBus eventBus) {
        CRITERION_TRIGGERS.register(eventBus);
    }
}
