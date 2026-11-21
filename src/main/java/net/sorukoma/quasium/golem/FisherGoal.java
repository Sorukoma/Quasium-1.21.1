package net.sorukoma.quasium.golem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.sorukoma.quasium.Config;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Stream;

final class FisherGoal extends Goal {
    private static final double REACH = 1.2;
    private static final int RESCAN_TICKS = 40;
    private static final int RIPPLE_TICKS = 10;

    private final ClayGolemEntity golem;
    private final ClayTrek trek;
    @Nullable
    private Spot spot;
    private long biteAt;
    private boolean cast;
    private int nextScan;

    private record Spot(BlockPos water, BlockPos stand) {
    }

    FisherGoal(ClayGolemEntity golem) {
        this.golem = golem;
        this.trek = new ClayTrek(golem);
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private boolean onDuty() {
        return golem.job() == GolemJob.FISHER && !golem.savesTool() && golem.mayWork() && golem.hasRoom();
    }

    @Override
    public boolean canUse() {
        if (GolemPrograms.running(golem) || GolemSmoke.withOwner(golem)) return false;
        if (golem.tickCount < nextScan || !onDuty()) return false;
        nextScan = golem.tickCount + golem.paced(RESCAN_TICKS);
        List<BlockPos> zones = golem.marks(MarkType.ZONE);
        if (zones.isEmpty()) {
            golem.waitFor("no_zone");
            return false;
        }
        spot = findWater(zones);
        if (spot == null) golem.waitFor("no_water");
        return spot != null;
    }

    @Override
    public boolean canContinueToUse() {
        return spot != null && golem.job() == GolemJob.FISHER && !golem.savesTool() && golem.mood().canWork() && golem.hasRoom()
                && water(golem.level(), spot.water());
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        cast = false;
        golem.note("fishing_start", spot.water());
        trek.to(Vec3.atBottomCenterOf(spot.stand()));
    }

    @Override
    public void stop() {
        spot = null;
        cast = false;
        golem.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (spot == null) return;
        Vec3 water = Vec3.atCenterOf(spot.water());
        golem.getLookControl().setLookAt(water.x, water.y, water.z);
        if (!trek.arrived(REACH)) {
            if (trek.stuck()) {
                golem.markUnreachable(spot.water());
                spot = null;
            }
            return;
        }
        golem.getNavigation().stop();
        long now = golem.level().getGameTime();
        if (!cast) castLine(now);
        else if (now >= biteAt) reelIn(water);
        else if (now % RIPPLE_TICKS == 0) ripple(water, 1);
    }

    private void castLine(long now) {
        cast = true;
        golem.swing(InteractionHand.MAIN_HAND);
        golem.level().playSound(null, golem.blockPosition(), SoundEvents.FISHING_BOBBER_THROW, SoundSource.NEUTRAL, 0.5f, 0.4f);
        ServerLevel level = (ServerLevel) golem.level();
        int lure = (int) (EnchantmentHelper.getFishingTimeReduction(level, golem.tool(), golem) * 20f);
        int min = Config.GOLEM_FISH_WAIT_MIN_SECONDS.getAsInt() * 20;
        int max = Math.max(min, Config.GOLEM_FISH_WAIT_MAX_SECONDS.getAsInt() * 20);
        biteAt = now + golem.paced(Math.max(20, Mth.nextInt(golem.getRandom(), min, max) - lure));
    }

    private void reelIn(Vec3 water) {
        ServerLevel level = (ServerLevel) golem.level();
        ItemStack rod = golem.tool();
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, water)
                .withParameter(LootContextParams.TOOL, rod)
                .withParameter(LootContextParams.ATTACKING_ENTITY, golem)
                .withLuck(EnchantmentHelper.getFishingLuckBonus(level, rod, golem))
                .create(LootContextParamSets.FISHING);
        List<ItemStack> catches = level.getServer().reloadableRegistries().getLootTable(BuiltInLootTables.FISHING).getRandomItems(params);
        golem.swing(InteractionHand.MAIN_HAND);
        ripple(water, 6);
        level.playSound(null, golem.blockPosition(), SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.NEUTRAL, 1f, 0.9f);
        catches.forEach(caught -> golem.note("caught", caught.getCount(), caught.getDescriptionId()));
        golem.takeIn(catches, golem.blockPosition());
        GolemTools.use(golem);
        golem.endHarvestTrip();
        cast = false;
    }

    private void ripple(Vec3 water, int count) {
        ServerLevel level = (ServerLevel) golem.level();
        level.sendParticles(ParticleTypes.FISHING, water.x, water.y + 0.5, water.z, count, 0.2, 0.0, 0.2, 0.0);
        level.sendParticles(ParticleTypes.BUBBLE, water.x, water.y + 0.3, water.z, count, 0.2, 0.1, 0.2, 0.05);
        if (count > 1) level.playSound(null, water.x, water.y, water.z, SoundEvents.FISHING_BOBBER_SPLASH, SoundSource.NEUTRAL, 0.4f, 1f);
    }

    @Nullable
    private Spot findWater(List<BlockPos> zones) {
        Level level = golem.level();
        for (BlockPos zone : zones) {
            BoundingBox box = ZoneBox.of(level, zone);
            if (!ZoneBox.loaded(level, box)) continue;
            List<BlockPos> waters = new ArrayList<>();
            for (BlockPos pos : ZoneBox.blocks(box)) {
                if (water(level, pos) && !golem.isUnreachable(pos)) waters.add(pos.immutable());
            }
            waters.sort(Comparator.comparingDouble(pos -> golem.distanceToSqr(Vec3.atCenterOf(pos))));
            for (BlockPos pos : waters) {
                BlockPos stand = shore(pos);
                if (stand != null) return new Spot(pos, stand);
            }
        }
        return null;
    }

    private static boolean water(Level level, BlockPos pos) {
        FluidState fluid = level.getFluidState(pos);
        return fluid.is(FluidTags.WATER) && fluid.isSource() && level.getBlockState(pos.above()).isAir();
    }

    @Nullable
    private BlockPos shore(BlockPos water) {
        return Direction.Plane.HORIZONTAL.stream().map(water::relative).flatMap(side -> Stream.of(side, side.above()))
                .filter(this::standable).min(Comparator.comparingDouble(pos -> golem.distanceToSqr(Vec3.atCenterOf(pos))))
                .map(BlockPos::immutable).orElse(null);
    }

    private boolean standable(BlockPos pos) {
        PathType type = WalkNodeEvaluator.getPathTypeStatic(golem, pos);
        return type == PathType.WALKABLE || type == PathType.WATER_BORDER;
    }
}
