package net.sorukoma.quasium.compat;

import dev.ryanhcode.sable.companion.SableCompanion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Soft compatibility with the Sable physics engine (the dependency behind Create Aeronautics).
 * <p>
 * Sable stores blocks/entities that belong to a moving structure inside "sub-levels" whose
 * coordinates live at extreme values in a plot grid. Naive position math
 * ({@code BlockPos#distSqr}, {@code Vec3#distanceToSqr}) therefore returns garbage when one of
 * the points is on a moving contraption — e.g. a hookah placed on an airship would report a
 * distance of millions of blocks and become permanently unusable.
 * <p>
 * The Sable Companion library is jar-in-jar'd into this mod and always provides an
 * implementation: a no-op default that computes plain Euclidean distance when Sable is absent,
 * transparently replaced by the real sub-level-aware implementation when Sable is installed.
 * That means every method here behaves <strong>exactly like vanilla math without Sable</strong>
 * and becomes sub-level-correct with Sable — no hard dependency, no runtime branching needed.
 */
public final class SableCompat {

    private SableCompat() {
    }

    /**
     * Squared distance between two points, correct across Sable sub-levels.
     * Without Sable this is identical to plain Euclidean {@code dx²+dy²+dz²}.
     */
    public static double distanceSqr(Level level,
                                     double ax, double ay, double az,
                                     double bx, double by, double bz) {
        return SableCompanion.INSTANCE.distanceSquaredWithSubLevels(level, ax, ay, az, bx, by, bz);
    }

    /**
     * Squared distance from an entity to the center of a block, sub-level aware.
     * Use this instead of {@code player.blockPosition().distSqr(pos)} for range checks against
     * blocks/block-entities that could be mounted on a moving contraption.
     */
    public static double distanceSqrToBlockCenter(Entity entity, BlockPos pos) {
        return distanceSqr(entity.level(),
                entity.getX(), entity.getY(), entity.getZ(),
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }

    /**
     * Squared distance from an entity to an arbitrary point, sub-level aware.
     * Use this instead of {@code entity.distanceToSqr(point)} when {@code point} is derived from
     * a block position that may live inside a sub-level plot.
     */
    public static double distanceSqr(Entity entity, Vec3 point) {
        return distanceSqr(entity.level(),
                entity.getX(), entity.getY(), entity.getZ(),
                point.x, point.y, point.z);
    }
}
