package net.sorukoma.quasium.golem;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import net.sorukoma.quasium.Config;
import net.sorukoma.quasium.block.LoungeCushionBlock;
import net.sorukoma.quasium.block.entity.HookahBlockEntity;
import net.sorukoma.quasium.component.ModDataComponents.HookahBowlContents;
import net.sorukoma.quasium.entity.SeatEntity;
import net.sorukoma.quasium.network.ModPackets;
import net.sorukoma.quasium.network.PacketSpawnSmoke;
import net.sorukoma.quasium.util.SmokeColorUtil;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

final class SmokeWithOwnerGoal extends Goal {
    private static final int LOOK_TICKS = 20;
    private static final int BUBBLE_TICKS = 20;
    private static final int MOOD_TICKS = 20;
    private static final int GIVE_UP_TICKS = 400;
    private static final double SIT_REACH = 0.75;
    private static final double STEP_REACH = 1.6;

    private final ClayGolemEntity golem;
    private final ClayTrek trek;
    @Nullable
    private HookahBlockEntity hookah;
    @Nullable
    private BlockPos cushion;
    private boolean seated;
    private boolean drew;
    private int ticks;
    private long nextLook;

    SmokeWithOwnerGoal(ClayGolemEntity golem) {
        this.golem = golem;
        this.trek = new ClayTrek(golem);
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        long now = golem.level().getGameTime();
        if (now < nextLook || GolemPrograms.running(golem)) return false;
        nextLook = now + LOOK_TICKS;
        hookah = GolemSmoke.hookah(golem);
        if (hookah == null || !golem.mayWork()) return false;
        cushion = GolemSmoke.freeCushion(golem, hookah);
        if (cushion != null && hookah.zone().seat(golem.getUUID(), cushion, now)) return true;
        golem.waitFor(cushion != null ? "smoke_full" : GolemSmoke.anyCushion(golem.level(), hookah.getBlockPos()) ? "smoke_no_seat" : "smoke_no_cushion");
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        if (hookah == null || cushion == null || GolemSmoke.hookah(golem) != hookah || !golem.mood().canWork()
                || GolemPrograms.running(golem)) return false;
        return seated ? GolemSmoke.cushion(golem.level(), cushion) && golem.distanceToSqr(seat()) < 1.0 : ticks < GIVE_UP_TICKS;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        seated = false;
        ticks = 0;
        trek.to(seat());
    }

    @Override
    public void stop() {
        if (hookah != null) hookah.zone().leave(golem.getUUID());
        if (seated) {
            golem.setPose(Pose.STANDING);
            golem.removeData(SmokeMarks.PIPE);
            golem.note("smoke_stand");
        }
        seated = false;
        hookah = null;
        cushion = null;
        golem.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (hookah == null || cushion == null) return;
        hookah.zone().seat(golem.getUUID(), cushion, golem.level().getGameTime());
        golem.getLookControl().setLookAt(Vec3.atCenterOf(hookah.getBlockPos()));
        if (seated) smoke();
        else approach();
    }

    private void approach() {
        if (ticks >= GIVE_UP_TICKS) return;
        ticks++;
        if (!GolemSmoke.cushion(golem.level(), cushion) || SeatEntity.isOccupied(golem.level(), cushion)) {
            BlockPos other = GolemSmoke.freeCushion(golem, hookah);
            if (other == null) {
                ticks = GIVE_UP_TICKS;
                return;
            }
            cushion = other;
            trek.to(seat());
        } else if (golem.distanceToSqr(seat()) <= SIT_REACH * SIT_REACH) {
            sit();
        } else if (golem.distanceToSqr(seat()) <= STEP_REACH * STEP_REACH) {
            Vec3 spot = seat();
            golem.getNavigation().stop();
            golem.getMoveControl().setWantedPosition(spot.x, spot.y, spot.z, 1.0);
        } else if (trek.stuck()) {
            golem.markUnreachable(cushion);
            ticks = GIVE_UP_TICKS;
        }
    }

    private void sit() {
        Vec3 spot = seat();
        golem.getNavigation().stop();
        golem.moveTo(spot.x, spot.y, spot.z, golem.getYRot(), golem.getXRot());
        golem.setPose(Pose.SITTING);
        seated = true;
        ticks = 0;
        golem.note("smoke_sit", hookah.getBlockPos());
    }

    private void smoke() {
        int pull = Config.GOLEM_SMOKE_PULL_SECONDS.getAsInt() * 20;
        int phase = ticks++ % (Math.max(Config.GOLEM_SMOKE_ROUND_SECONDS.getAsInt() * 20, pull + 20));
        boolean pulled = phase < pull && hookah.golemPuff(golem.getUUID());
        long now = golem.level().getGameTime();
        if (phase == 0) drew = false;
        drew |= pulled;
        if (pulled && phase == 0) golem.setData(SmokeMarks.PIPE, new GolemPipe(hookah.getBlockPos(), now, now + pull));
        if (pulled && phase % BUBBLE_TICKS == 0) {
            golem.level().playSound(null, hookah.getBlockPos(), SoundEvents.LAVA_POP, SoundSource.BLOCKS, 0.35f, 1.1f);
        }
        if (phase == pull) exhale();
        if (ticks % MOOD_TICKS == 0) hunger();
    }

    private void exhale() {
        if (drew) {
            hookah.golemExhaled(golem.onlineOwner());
            GolemNap.puffed(golem);
        }
        HookahBowlContents row = hookah.burningRow();
        ModPackets.sendToClientsTrackingEntityAndSelf(new PacketSpawnSmoke(golem.getId(), golem.getLookAngle(),
                SmokeColorUtil.colorForContents(row), SmokeColorUtil.smokeDensity(row)), golem);
    }

    private void hunger() {
        float idle = GolemMood.FULL * MOOD_TICKS / GolemMood.periodTicks();
        int before = golem.mood().state();
        golem.mood().spend(golem.isVibing() ? 2 * idle : idle);
        golem.afterMood(before);
    }

    private Vec3 seat() {
        return new Vec3(cushion.getX() + 0.5, cushion.getY() + LoungeCushionBlock.SEAT_HEIGHT, cushion.getZ() + 0.5);
    }
}
