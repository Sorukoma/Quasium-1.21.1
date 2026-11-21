package net.sorukoma.quasium.golem;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class LumberjackGoal extends ClayChoreGoal {
    private static final int MAX_LOGS = 64;

    LumberjackGoal(ClayGolemEntity golem) {
        super(golem, GolemJob.LUMBERJACK);
    }

    @Nullable
    @Override
    protected Chore next(List<BlockPos> zones) {
        Level level = golem.level();
        if (!EventHooks.canEntityGrief(level, golem)) {
            golem.waitFor("no_griefing");
            return null;
        }
        for (BlockPos zone : zones) {
            BoundingBox box = ZoneBox.of(level, zone);
            if (!ZoneBox.loaded(level, box)) continue;
            Chore chore = plant(zone, box);
            if (chore == null && golem.hasRoom()) chore = pickUp(zone);
            if (chore == null && golem.hasRoom()) chore = fell(zone, box);
            if (chore != null) return chore;
        }
        if (golem.hasRoom()) golem.waitFor("no_trees");
        return null;
    }

    @Nullable
    private Chore plant(BlockPos zone, BoundingBox box) {
        if (ClayStores.slotOf(golem.cargo(), LumberjackGoal::sapling) < 0) return null;
        Level level = golem.level();
        golem.stumps().removeIf(stump -> level.isLoaded(stump) && !free(stump));
        for (BlockPos stump : golem.stumps()) {
            if (!box.isInside(stump) || golem.isUnreachable(stump) || !level.isLoaded(stump)) continue;
            return Chore.at(zone, stump, () -> free(stump), () -> plantAt(stump));
        }
        return null;
    }

    private boolean free(BlockPos stump) {
        return golem.level().getBlockState(stump).canBeReplaced() && golem.level().getBlockState(stump.below()).is(BlockTags.DIRT);
    }

    private boolean plantAt(BlockPos stump) {
        int slot = ClayStores.slotOf(golem.cargo(), LumberjackGoal::sapling);
        if (slot < 0) return true;
        Level level = golem.level();
        ItemStack sapling = golem.cargo().getStackInSlot(slot);
        BlockState state = ((BlockItem) sapling.getItem()).getBlock().defaultBlockState();
        golem.stumps().remove(stump);
        if (!state.canSurvive(level, stump)) return true;
        level.setBlockAndUpdate(stump, state);
        level.playSound(null, stump, state.getSoundType(level, stump, golem).getPlaceSound(), SoundSource.BLOCKS, 1f, 0.9f);
        golem.note("planted", sapling.getDescriptionId());
        golem.cargo().extractItem(slot, 1, false);
        return true;
    }

    private static boolean sapling(ItemStack stack) {
        return stack.is(ItemTags.SAPLINGS) && stack.getItem() instanceof BlockItem;
    }

    @Nullable
    private Chore pickUp(BlockPos zone) {
        ItemEntity item = nearest(around(ItemEntity.class, zone, this::litter));
        if (item == null) return null;
        return Chore.on(zone, item, () -> litter(item), () -> {
            golem.pickUp(item);
            return true;
        });
    }

    private boolean litter(ItemEntity item) {
        ItemStack stack = item.getItem();
        return !item.hasPickUpDelay() && (stack.is(ItemTags.SAPLINGS) || stack.is(Items.STICK) || stack.is(Items.APPLE)) && golem.wants(stack);
    }

    @Nullable
    private Chore fell(BlockPos zone, BoundingBox box) {
        Level level = golem.level();
        List<BlockPos> bases = new ArrayList<>();
        for (BlockPos pos : ZoneBox.blocks(box)) {
            if (level.getBlockState(pos).is(BlockTags.LOGS) && level.getBlockState(pos.below()).is(BlockTags.DIRT) && !golem.isUnreachable(pos)) {
                bases.add(pos.immutable());
            }
        }
        bases.sort(Comparator.comparingDouble(base -> golem.distanceToSqr(base.getCenter())));
        for (BlockPos base : bases) {
            Deque<BlockPos> logs = tree(level, base);
            if (logs == null) continue;
            Felling felling = new Felling(base, logs);
            return Chore.at(zone, base, () -> golem.hasRoom() && EventHooks.canEntityGrief(level, golem), felling::chop);
        }
        return null;
    }

    @Nullable
    private static Deque<BlockPos> tree(Level level, BlockPos base) {
        List<BlockPos> logs = new ArrayList<>(List.of(base));
        Set<BlockPos> seen = new HashSet<>(logs);
        boolean leafy = false;
        for (int i = 0; i < logs.size(); i++) {
            BlockPos log = logs.get(i);
            for (BlockPos near : BlockPos.betweenClosed(log.offset(-1, -1, -1), log.offset(1, 1, 1))) {
                if (near.getY() < base.getY() || seen.contains(near) || !level.isLoaded(near)) continue;
                BlockState state = level.getBlockState(near);
                if (state.is(BlockTags.LOGS) && logs.size() < MAX_LOGS) {
                    BlockPos found = near.immutable();
                    seen.add(found);
                    logs.add(found);
                } else if (state.is(BlockTags.LEAVES) && !state.getOptionalValue(LeavesBlock.PERSISTENT).orElse(false)) {
                    leafy = true;
                }
            }
        }
        if (!leafy) return null;
        logs.sort(Comparator.<BlockPos>comparingInt(BlockPos::getY).reversed());
        return new ArrayDeque<>(logs);
    }

    private final class Felling {
        private final BlockPos base;
        private final Deque<BlockPos> logs;
        private int felled;

        private Felling(BlockPos base, Deque<BlockPos> logs) {
            this.base = base;
            this.logs = logs;
        }

        boolean chop() {
            ServerLevel level = (ServerLevel) golem.level();
            while (!logs.isEmpty() && !level.getBlockState(logs.peek()).is(BlockTags.LOGS)) logs.pop();
            if (!logs.isEmpty()) {
                BlockPos log = logs.pop();
                BlockState state = level.getBlockState(log);
                List<ItemStack> drops = Block.getDrops(state, level, log, level.getBlockEntity(log), golem, golem.tool());
                level.destroyBlock(log, false, golem);
                golem.takeIn(drops, log);
                if (level.getBlockState(log.below()).is(BlockTags.DIRT)) golem.stumps().add(log);
                GolemTools.use(golem);
                felled++;
            }
            if (!logs.isEmpty()) return false;
            if (felled > 0) golem.note("felled", felled, base);
            return true;
        }
    }
}
