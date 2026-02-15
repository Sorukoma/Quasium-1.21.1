package net.sorukoma.quasium.component;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.sorukoma.quasium.QuasiumMod;
import net.minecraft.core.registries.Registries; // Добавлен импорт Registries

public class ModDataComponents {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, QuasiumMod.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> KAIF_VALUE = DATA_COMPONENT_TYPES.register("kaif_value",
            () -> DataComponentType.<Integer>builder() // Явно указываем тип для билдера
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    public static void register(net.neoforged.bus.api.IEventBus eventBus) {
        DATA_COMPONENT_TYPES.register(eventBus);
    }
}
