package net.sorukoma.quasium.smokelands.fluid;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidType;

public class SmokeFluidType extends FluidType {
    private static final double SINK_SPEED = -0.04;

    public SmokeFluidType(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canSwim(Entity entity) {
        return !(entity instanceof Player player) || SmokeSea.holds(player);
    }

    @Override
    public boolean canDrownIn(LivingEntity entity) {
        return entity instanceof Player player && !SmokeSea.holds(player);
    }

    @Override
    public boolean move(FluidState state, LivingEntity entity, Vec3 movementVector, double gravity) {
        if (!(entity instanceof Player player) || SmokeSea.holds(player)) return false;
        entity.moveRelative(0.01f, movementVector);
        entity.move(MoverType.SELF, entity.getDeltaMovement());
        Vec3 motion = entity.getDeltaMovement();
        entity.setDeltaMovement(motion.x * 0.6, Math.min(motion.y * 0.8 - gravity / 4, SINK_SPEED), motion.z * 0.6);
        return true;
    }
}
