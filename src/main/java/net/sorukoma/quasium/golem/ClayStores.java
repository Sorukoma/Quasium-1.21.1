package net.sorukoma.quasium.golem;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

final class ClayStores {
    private ClayStores() {
    }

    static List<BlockPos> of(ClayGolemEntity golem) {
        List<BlockPos> stores = new ArrayList<>();
        for (BlockPos mark : golem.marks(MarkType.FILL)) stores.addAll(SmokeMarks.stores(golem.level(), mark));
        return stores;
    }

    @Nullable
    static IItemHandler handler(Level level, BlockPos pos) {
        return level.isLoaded(pos) ? level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null) : null;
    }

    static int slotOf(@Nullable IItemHandler items, Predicate<ItemStack> wanted) {
        if (items == null) return -1;
        for (int slot = 0; slot < items.getSlots(); slot++) {
            if (wanted.test(items.getStackInSlot(slot))) return slot;
        }
        return -1;
    }

    private static int takeableSlot(@Nullable IItemHandler items, Predicate<ItemStack> wanted) {
        if (items == null) return -1;
        for (int slot = 0; slot < items.getSlots(); slot++) {
            if (wanted.test(items.getStackInSlot(slot)) && !items.extractItem(slot, 1, true).isEmpty()) return slot;
        }
        return -1;
    }

    static int count(IItemHandler items, Predicate<ItemStack> wanted) {
        int count = 0;
        for (int slot = 0; slot < items.getSlots(); slot++) {
            ItemStack stack = items.getStackInSlot(slot);
            if (wanted.test(stack)) count += stack.getCount();
        }
        return count;
    }

    @Nullable
    static BlockPos holding(ClayGolemEntity golem, Predicate<ItemStack> wanted) {
        for (BlockPos store : of(golem)) {
            if (!golem.isUnreachable(store) && holds(golem, store, wanted)) return store;
        }
        return null;
    }

    static boolean holds(ClayGolemEntity golem, BlockPos store, Predicate<ItemStack> wanted) {
        return takeableSlot(handler(golem.level(), store), wanted) >= 0;
    }

    static ItemStack take(ClayGolemEntity golem, BlockPos store, Predicate<ItemStack> wanted, int max) {
        IItemHandler items = handler(golem.level(), store);
        int slot = takeableSlot(items, wanted);
        if (slot < 0) return ItemStack.EMPTY;
        ItemStack offered = items.extractItem(slot, max, true);
        int fits = offered.getCount() - golem.stow(offered, true).getCount();
        if (fits <= 0) return ItemStack.EMPTY;
        ItemStack taken = items.extractItem(slot, fits, false);
        golem.stow(taken.copy(), false);
        return taken;
    }
}
