package net.sorukoma.quasium.golem;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.sorukoma.quasium.Config;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.EnumSet;

final class ClayFightGoal extends Goal {
    private static final int CHASE_MARGIN = 4;
    private static final int SCAN_TICKS = 10;
    private static final int REPATH_TICKS = 10;
    private static final int GIVE_UP_TICKS = 200;

    private final ClayGolemEntity golem;
    @Nullable
    private Mob target;
    @Nullable
    private Mob ignored;
    private Vec3 post = Vec3.ZERO;
    private int nextScan;
    private int nextHit;
    private int repath;
    private int sinceHit;

    ClayFightGoal(ClayGolemEntity golem) {
        this.golem = golem;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (golem.tickCount < nextScan || !golem.mayWork()) return false;
        nextScan = golem.tickCount + SCAN_TICKS;
        BlockPos mark = golem.boundGuardMark();
        if (mark == null) return false;
        post = Vec3.atBottomCenterOf(mark);
        target = nearestHostile(mark);
        return target != null;
    }

    @Override
    public boolean canContinueToUse() {
        return target != null && target.isAlive() && sinceHit < GIVE_UP_TICKS && near(target, chase()) && near(golem, chase());
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        repath = 0;
        sinceHit = 0;
        golem.note("fight", BlockPos.containing(post), target.getType().getDescriptionId());
    }

    @Override
    public void stop() {
        if (target != null) {
            if (sinceHit >= GIVE_UP_TICKS) ignored = target;
            golem.note(target.isAlive() ? "drove_off" : "killed", target.getType().getDescriptionId());
            if (!target.isAlive()) golem.jobDone();
        }
        target = null;
        golem.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (target == null) return;
        sinceHit++;
        golem.getLookControl().setLookAt(target, 30f, 30f);
        if (--repath <= 0) {
            repath = REPATH_TICKS;
            golem.getNavigation().moveTo(target, 1.0);
        }
        if (golem.tickCount < nextHit || !golem.isWithinMeleeAttackRange(target) || !golem.getSensing().hasLineOfSight(target)) return;
        nextHit = golem.tickCount + golem.paced(Config.CLAY_GUARD_ATTACK_TICKS.getAsInt());
        sinceHit = 0;
        golem.swing(InteractionHand.MAIN_HAND);
        target.hurt(golem.damageSources().mobAttack(golem), (float) Config.CLAY_GUARD_DAMAGE.getAsDouble());
    }

    @Nullable
    private Mob nearestHostile(BlockPos mark) {
        int radius = Config.CLAY_GUARD_RADIUS.getAsInt();
        return golem.level().getEntitiesOfClass(Mob.class, new AABB(mark).inflate(radius),
                        mob -> mob != ignored && hostile(mob) && near(mob, radius) && golem.getSensing().hasLineOfSight(mob))
                .stream().min(Comparator.comparingDouble(golem::distanceToSqr)).orElse(null);
    }

    private static boolean hostile(Mob mob) {
        return mob instanceof Enemy && !(mob instanceof Creeper) && !(mob instanceof NeutralMob neutral && !neutral.isAngry());
    }

    private double chase() {
        return Config.CLAY_GUARD_RADIUS.getAsInt() + CHASE_MARGIN;
    }

    private boolean near(Entity entity, double reach) {
        return entity.distanceToSqr(post) <= reach * reach;
    }
}
