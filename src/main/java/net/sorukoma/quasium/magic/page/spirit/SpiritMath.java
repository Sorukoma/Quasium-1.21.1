package net.sorukoma.quasium.magic.page.spirit;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.sorukoma.quasium.magic.MagicMath;
import net.sorukoma.quasium.magic.NoteType;
import org.jetbrains.annotations.Nullable;

public final class SpiritMath {
    private static final Vec3 UP = new Vec3(0, 1, 0);
    private static final Vec3 EAST = new Vec3(1, 0, 0);

    private SpiritMath() {
    }

    public static Vec3 turn(Vec3 heading, Vec3 wanted, double maxDegrees) {
        Vec3 from = heading.normalize();
        Vec3 to = wanted.normalize();
        double cos = Mth.clamp(from.dot(to), -1.0, 1.0);
        double max = Math.toRadians(maxDegrees);
        if (Math.acos(cos) <= max) return to;
        Vec3 side = to.subtract(from.scale(cos));
        if (side.lengthSqr() < 1.0E-8) side = from.cross(Math.abs(from.y) < 0.9 ? UP : EAST);
        return from.scale(Math.cos(max)).add(side.normalize().scale(Math.sin(max)));
    }

    public static double turnDegrees(double base, float homing, int stars, int homingPct, int starPct) {
        return base * (1 + homing * homingPct / 100.0) * (1 + stars * starPct / 100.0);
    }

    public static int lifeTicks(float lifetimeSeconds, int stars, int starSeconds) {
        return Math.round((lifetimeSeconds + stars * starSeconds) * MagicMath.TICKS_PER_SECOND);
    }

    public static float ageStep(boolean inRain, int rainLifePct) {
        return inRain ? 100f / rainLifePct : 1f;
    }

    public static float flockPower(float power, int count, int copyPowerPct) {
        return count > 1 ? power * copyPowerPct / 100f : power;
    }

    public static float squeakPitch(@Nullable NoteType note) {
        int semitones = note == null ? 2 : switch (note) {
            case MENTHOL -> 9;
            case EMBER -> 0;
            case NECTAR -> 4;
            case DEW -> 7;
            case RESIN -> -5;
            case ASH -> -3;
        };
        return (float) Math.pow(2.0, semitones / 12.0);
    }
}
