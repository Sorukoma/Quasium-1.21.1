package net.sorukoma.quasium.golem;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.sorukoma.quasium.block.TobaccoCropBlock;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class ClayFields {
    record Work(BlockPos pos, @Nullable ItemEntity item, BlockPos mark) {
        Vec3 spot() {
            return item != null ? item.position() : Vec3.atBottomCenterOf(pos);
        }
    }

    private ClayFields() {
    }

    @Nullable
    static Work findWork(ClayGolemEntity golem) {
        for (BlockPos mark : fields(golem)) {
            Work work = nearestIn(golem, mark);
            if (work != null) return work;
        }
        return null;
    }

    static List<BlockPos> fields(ClayGolemEntity golem) {
        List<BlockPos> fields = new ArrayList<>(golem.marks(MarkType.HARVEST));
        if (golem.job() == GolemJob.TOBACCO) fields.addAll(golem.marks(MarkType.ZONE));
        return fields;
    }

    static boolean stillThere(ClayGolemEntity golem, Work work) {
        if (work.item() != null) return work.item().isAlive() && golem.wants(work.item().getItem());
        return golem.hasRoom() && isRipe(golem.level().getBlockState(work.pos()));
    }

    private static boolean isRipe(BlockState state) {
        return state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state);
    }

    @Nullable
    private static Work nearestIn(ClayGolemEntity golem, BlockPos mark) {
        Level level = golem.level();
        BoundingBox box = ZoneBox.of(level, mark);
        if (!ZoneBox.loaded(level, box)) return null;
        List<Work> options = new ArrayList<>();
        if (golem.hasRoom()) {
            for (BlockPos pos : ZoneBox.blocks(box)) {
                if (isRipe(level.getBlockState(pos)) && !golem.isUnreachable(pos)) options.add(new Work(pos.immutable(), null, mark));
            }
        }
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, AABB.of(box),
                item -> !item.hasPickUpDelay() && golem.wants(item.getItem()) && !golem.isUnreachable(item.blockPosition()))) {
            options.add(new Work(item.blockPosition(), item, mark));
        }
        Comparator<Work> order = Comparator.comparingDouble(work -> golem.distanceToSqr(work.spot()));
        if (golem.isSommelier()) order = Comparator.comparing((Work work) -> !isTobaccoCrop(level, work))
                .thenComparing(work -> !isExactMature(level, work)).thenComparing(order);
        return options.stream().min(order).orElse(null);
    }

    private static boolean isTobaccoCrop(Level level, Work work) {
        return work.item() == null && level.getBlockState(work.pos()).getBlock() instanceof TobaccoCropBlock;
    }

    private static boolean isExactMature(Level level, Work work) {
        return work.item() == null && level.getBlockState(work.pos()).getBlock() instanceof TobaccoCropBlock crop
                && crop.isExactMature(level, work.pos());
    }

    static List<ItemStack> reap(ServerLevel level, BlockPos pos, ClayGolemEntity golem) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof TobaccoCropBlock tobacco) return tobacco.shearLeaves(state, level, pos);
        CropBlock crop = (CropBlock) state.getBlock();
        List<ItemStack> drops = new ArrayList<>(Block.getDrops(state, level, pos, level.getBlockEntity(pos), golem, ItemStack.EMPTY));
        Item seed = crop.getCloneItemStack(level, pos, state).getItem();
        level.destroyBlock(pos, false, golem);
        if (level.getBlockState(pos.below()).getBlock() instanceof FarmBlock && takeOne(drops, seed)) {
            level.setBlockAndUpdate(pos, crop.getStateForAge(0));
        }
        return drops;
    }

    private static boolean takeOne(List<ItemStack> drops, Item item) {
        for (ItemStack drop : drops) {
            if (drop.is(item)) {
                drop.shrink(1);
                return true;
            }
        }
        return false;
    }
}
