package net.sorukoma.quasium.golem;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.sorukoma.quasium.component.ModDataComponents;
import net.sorukoma.quasium.component.ModDataComponents.BowlTobaccoGroup;
import net.sorukoma.quasium.component.ModDataComponents.HookahBowlContents;
import net.sorukoma.quasium.item.SelfMixedTobaccoItem;
import net.sorukoma.quasium.item.TobaccoItem;
import net.sorukoma.quasium.screen.HookahBowlMenu;

final class BowlTobacco {
    private BowlTobacco() {
    }

    static HookahBowlContents contents(ItemStack bowl) {
        return bowl.getOrDefault(ModDataComponents.HOOKAH_BOWL_CONTENTS.get(), HookahBowlContents.cleared());
    }

    static boolean holds(IItemHandler store, HookahBowlContents bowl) {
        for (int slot = 0; slot < store.getSlots(); slot++) {
            if (suits(bowl, gramOf(store.getStackInSlot(slot)))) return true;
        }
        return false;
    }

    static ItemStack take(WorkerGolem golem, IItemHandler store, HookahBowlContents bowl, int grams) {
        ItemStack taken = ItemStack.EMPTY;
        for (int slot = 0; slot < store.getSlots() && taken.getCount() < grams; slot++) {
            ItemStack gram = gramOf(store.getStackInSlot(slot));
            if (!suits(bowl, gram) || !taken.isEmpty() && !ItemStack.isSameItemSameComponents(taken, gram)) continue;
            taken = gram.copyWithCount(taken.getCount() + pinch(golem, store, slot, grams - taken.getCount()));
        }
        return taken;
    }

    private static int pinch(WorkerGolem golem, IItemHandler store, int slot, int grams) {
        ItemStack stack = store.getStackInSlot(slot);
        boolean selfMix = stack.getItem() instanceof SelfMixedTobaccoItem;
        if (!selfMix && !(stack.getItem() instanceof TobaccoItem)) return store.extractItem(slot, grams, false).getCount();
        ItemStack pack = store.extractItem(slot, 1, false);
        int weight = pack.getOrDefault(ModDataComponents.TOBACCO_WEIGHT.get(), 0);
        int taken = Math.min(weight, grams);
        pack.set(ModDataComponents.TOBACCO_WEIGHT.get(), weight - taken);
        if (!selfMix || taken < weight) golem.spawnAtLocation(store.insertItem(slot, pack, false));
        return taken;
    }

    private static ItemStack gramOf(ItemStack stack) {
        boolean full = stack.getOrDefault(ModDataComponents.TOBACCO_WEIGHT.get(), 0) > 0;
        if (stack.getItem() instanceof TobaccoItem pack) return full ? TobaccoItem.oneGram(pack.getFlavor()) : ItemStack.EMPTY;
        if (stack.getItem() instanceof SelfMixedTobaccoItem) return full ? SelfMixedTobaccoItem.createOneGram(stack) : ItemStack.EMPTY;
        return HookahBowlMenu.groupOf(stack) == null ? ItemStack.EMPTY : stack.copyWithCount(1);
    }

    private static boolean suits(HookahBowlContents bowl, ItemStack gram) {
        BowlTobaccoGroup kind = HookahBowlMenu.groupOf(gram);
        return kind != null && bowl.addSlot(HookahBowlContents.MAX_SLOTS, kind).isCompatible();
    }
}
