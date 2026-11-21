package net.sorukoma.quasium.magic;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public interface SpellAction {
    default void hitEntity(CastContext ctx, LivingEntity target) {
    }

    default void hitBlock(CastContext ctx, BlockPos pos) {
    }

    default void impactBlock(CastContext ctx, BlockPos pos) {
        hitBlock(ctx, pos);
    }

    default boolean usesBlocks() {
        return false;
    }

    default boolean usesBlocks(CastContext ctx) {
        return usesBlocks();
    }

    default void cloudTick(CastContext ctx, Vec3 center, float radius, List<LivingEntity> inside) {
    }

    default void pulse(CastContext ctx, Vec3 center, float radius) {
    }
}
