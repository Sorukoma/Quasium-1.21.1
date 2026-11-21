package net.sorukoma.quasium.smokelands.fluid;

import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.sorukoma.quasium.smokelands.SmokelandsRegistry;

public final class SmokeFluids {
    public static final DeferredHolder<FluidType, SmokeFluidType> TYPE = SmokelandsRegistry.FLUID_TYPES.register("smoke",
            () -> new SmokeFluidType(FluidType.Properties.create().descriptionId("block.quasium.smoke")
                    .canSwim(false).canDrown(true).canPushEntity(false).canExtinguish(false).canConvertToSource(false)
                    .canHydrate(false).supportsBoating(false).motionScale(0.007).fallDistanceModifier(0f)
                    .density(-200).viscosity(1500).temperature(330)));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> SOURCE =
            SmokelandsRegistry.FLUIDS.register("smoke", () -> new BaseFlowingFluid.Source(properties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING =
            SmokelandsRegistry.FLUIDS.register("flowing_smoke", () -> new BaseFlowingFluid.Flowing(properties()));
    public static final DeferredBlock<SmokeBlock> BLOCK = SmokelandsRegistry.BLOCKS.register("smoke",
            () -> new SmokeBlock(SOURCE.get(), BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).replaceable().noCollission()
                    .destroyTime(0f).explosionResistance(100f).pushReaction(PushReaction.DESTROY).noLootTable().liquid()));

    private SmokeFluids() {
    }

    public static void load() {
    }

    private static BaseFlowingFluid.Properties properties() {
        return new BaseFlowingFluid.Properties(TYPE, SOURCE, FLOWING).block(BLOCK).slopeFindDistance(2).levelDecreasePerBlock(2)
                .tickRate(15).explosionResistance(100f);
    }
}
