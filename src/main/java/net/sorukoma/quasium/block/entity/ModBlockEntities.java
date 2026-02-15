package net.sorukoma.quasium.block.entity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder; // Импортируем DeferredHolder

import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.block.ModBlocks;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, QuasiumMod.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HookahBlockEntity>> HOOKAH_BE =
            BLOCK_ENTITIES.register("hookah_be", () ->
                    BlockEntityType.Builder.of(HookahBlockEntity::new,
                            ModBlocks.HOOKAH_BLOCK.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
