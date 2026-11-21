package net.sorukoma.quasium.magic.page;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public interface HeartBehavior {
    default void hitEntity(PageHit hit, LivingEntity target) {
    }

    default boolean usesBlocks() {
        return false;
    }

    default void hitBlock(PageHit hit, BlockPos pos) {
    }

    default void pulse(PageHit hit, Vec3 center, float radius) {
    }
}
