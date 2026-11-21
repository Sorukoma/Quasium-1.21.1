package net.sorukoma.quasium.golem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.sorukoma.quasium.golem.program.Check;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

final class ProgramChecks {
    private ProgramChecks() {
    }

    static boolean holds(ClayGolemEntity golem, Check check, BlockPos pos, Direction face, ItemStack filter) {
        Level level = golem.level();
        return switch (check) {
            case LOAD_FULL -> !golem.hasRoom();
            case LOAD_EMPTY -> !golem.hasCargo();
            case HUNGRY -> golem.mood().hungry();
            case DAY -> level.isDay();
            case NIGHT -> !level.isDay();
            case HAS_ITEM -> holdsItem(ProgramHands.container(level, pos, face), filter);
            case NO_ROOM -> packed(ProgramHands.container(level, pos, face), filter);
            case BLOCK_IS -> level.isLoaded(pos) && standsThere(level.getBlockState(pos), filter);
            case EMPTY_AT -> level.isLoaded(pos) && level.getBlockState(pos).isAir();
            case SIGNAL -> powered(level, pos);
        };
    }

    static boolean powered(Level level, BlockPos pos) {
        if (!level.isLoaded(pos)) return false;
        if (level.hasNeighborSignal(pos)) return true;
        BlockState state = level.getBlockState(pos);
        if (!state.isSignalSource()) return false;
        for (Direction side : Direction.values()) {
            if (state.getSignal(level, pos, side) > 0) return true;
        }
        return false;
    }

    private static boolean holdsItem(@Nullable IItemHandler container, ItemStack filter) {
        if (container == null) return false;
        Predicate<ItemStack> wanted = ProgramHands.wanted(filter);
        for (int slot = 0; slot < container.getSlots(); slot++) {
            ItemStack stack = container.getStackInSlot(slot);
            if (!stack.isEmpty() && wanted.test(stack)) return true;
        }
        return false;
    }

    private static boolean packed(@Nullable IItemHandler container, ItemStack filter) {
        if (container == null) return false;
        if (!filter.isEmpty()) return !ItemHandlerHelper.insertItemStacked(container, filter.copyWithCount(1), true).isEmpty();
        for (int slot = 0; slot < container.getSlots(); slot++) {
            ItemStack stack = container.getStackInSlot(slot);
            if (stack.isEmpty() || stack.getCount() < Math.min(container.getSlotLimit(slot), stack.getMaxStackSize())) return false;
        }
        return true;
    }

    private static boolean standsThere(BlockState state, ItemStack filter) {
        if (filter.isEmpty()) return !state.isAir();
        return filter.getItem() instanceof BlockItem item && state.is(item.getBlock());
    }
}
