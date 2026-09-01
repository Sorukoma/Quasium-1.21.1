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

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BonsaiPotBlockEntity>> BONSAI_POT_BE =
            BLOCK_ENTITIES.register("bonsai_pot_be", () ->
                    BlockEntityType.Builder.of(BonsaiPotBlockEntity::new,
                            ModBlocks.BONSAI_POT.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HookahBlockEntity>> HOOKAH_BE =
            BLOCK_ENTITIES.register("hookah_be", () ->
                    BlockEntityType.Builder.of(HookahBlockEntity::new,
                            ModBlocks.HOOKAH_BLOCK.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ModularHookahBlockEntity>> MODULAR_HOOKAH_BE =
            BLOCK_ENTITIES.register("modular_hookah_be", () ->
                    BlockEntityType.Builder.of(ModularHookahBlockEntity::new,
                            ModBlocks.MODULAR_HOOKAH_WHITE.get(),
                            ModBlocks.MODULAR_HOOKAH_BLACK.get(),
                            ModBlocks.MODULAR_HOOKAH_RED.get(),
                            ModBlocks.MODULAR_HOOKAH_BLUE.get(),
                            ModBlocks.MODULAR_HOOKAH_AMBER.get()).build(null));

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

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EssencePressBlockEntity>> ESSENCE_PRESS_BE =
            BLOCK_ENTITIES.register("essence_press_be", () ->
                    BlockEntityType.Builder.of(EssencePressBlockEntity::new,
                            ModBlocks.ESSENCE_PRESS.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<QuasiumAnvilBlockEntity>> QUASIUM_ANVIL_BE =
            BLOCK_ENTITIES.register("quasium_anvil_be", () ->
                    BlockEntityType.Builder.of(QuasiumAnvilBlockEntity::new,
                            ModBlocks.QUASIUM_ANVIL.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TobaccoPoleBlockEntity>> TOBACCO_POLE_BE =
            BLOCK_ENTITIES.register("tobacco_pole_be", () -> BlockEntityType.Builder.of(TobaccoPoleBlockEntity::new,
                    ModBlocks.WOODEN_TOBACCO_POLE.get(), ModBlocks.REINFORCED_TOBACCO_POLE.get(),
                    ModBlocks.GOLDEN_TOBACCO_POLE.get(), ModBlocks.DIAMOND_TOBACCO_POLE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TobaccoCropBlockEntity>> TOBACCO_CROP_BE =
            BLOCK_ENTITIES.register("tobacco_crop_be", () -> BlockEntityType.Builder.of(TobaccoCropBlockEntity::new,
                    ModBlocks.DIKAR_CROP.get(), ModBlocks.PESCHANIK_CROP.get(),
                    ModBlocks.BOLOTNIK_CROP.get(), ModBlocks.LEDYANOY_CROP.get(),
                    ModBlocks.HYBRID_TOBACCO_CROP.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HeaterBlockEntity>> HEATER_BE =
            BLOCK_ENTITIES.register("heater_be", () ->
                    BlockEntityType.Builder.of(HeaterBlockEntity::new,
                            ModBlocks.HEATER.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}