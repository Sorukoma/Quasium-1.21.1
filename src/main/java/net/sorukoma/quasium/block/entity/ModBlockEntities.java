package net.sorukoma.quasium.block.entity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.block.ModBlocks;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, QuasiumMod.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HookahBlockEntity>> HOOKAH_BE =
            BLOCK_ENTITIES.register("hookah_be", () ->
                    BlockEntityType.Builder.of(HookahBlockEntity::new,
                            ModBlocks.HOOKAH_BLOCK.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DryingShelfBlockEntity>> DRYING_SHELF_BE =
            BLOCK_ENTITIES.register("drying_shelf_be", () ->
                    BlockEntityType.Builder.of(DryingShelfBlockEntity::new,
                            ModBlocks.DRYING_SHELF.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FermentationBarrelBlockEntity>> FERMENTATION_BARREL_BE =
            BLOCK_ENTITIES.register("fermentation_barrel_be", () ->
                    BlockEntityType.Builder.of(FermentationBarrelBlockEntity::new,
                            ModBlocks.FERMENTATION_BARREL.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SoakingBowlBlockEntity>> SOAKING_BOWL_BE =
            BLOCK_ENTITIES.register("soaking_bowl_be", () ->
                    BlockEntityType.Builder.of( SoakingBowlBlockEntity::new,
                            ModBlocks.SOAKING_BOWL.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<QuasiumAnvilBlockEntity>> QUASIUM_ANVIL_BE =
            BLOCK_ENTITIES.register("quasium_anvil_be", () ->
                    BlockEntityType.Builder.of(QuasiumAnvilBlockEntity::new,
                            ModBlocks.QUASIUM_ANVIL.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HeaterBlockEntity>> HEATER_BE =
            BLOCK_ENTITIES.register("heater_be", () ->
                    BlockEntityType.Builder.of(HeaterBlockEntity::new,
                            ModBlocks.HEATER.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}