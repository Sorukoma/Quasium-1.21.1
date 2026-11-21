package net.sorukoma.quasium.golem;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import net.sorukoma.quasium.block.entity.BowlAutomation;
import net.sorukoma.quasium.block.entity.SoakingBowlBlockEntity;
import net.sorukoma.quasium.block.entity.TobaccoAutomation;
import net.sorukoma.quasium.golem.ClayChoreGoal.Chore;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

final class MixingChores {
    private final ClayGolemEntity golem;

    MixingChores(ClayGolemEntity golem) {
        this.golem = golem;
    }

    static boolean kept(ItemStack stack) {
        return TobaccoAutomation.fermentedLeaf(stack) || BowlAutomation.liquid(stack);
    }

    @Nullable
    Chore collect(BlockPos zone, BlockEntity rack) {
        if (!(rack instanceof SoakingBowlBlockEntity bowl) || !bowl.isReady() || !bowl.holdsResult()) return null;
        BowlAutomation hands = new BowlAutomation(bowl);
        BlockPos pos = rack.getBlockPos();
        return Chore.at(zone, pos, () -> bowl.isReady() && bowl.holdsResult() && !bowl.isRemoved(), () -> {
            ItemStack mixed = hands.extractItem(BowlAutomation.RESULT, 1, false);
            ItemStack empties = hands.extractItem(BowlAutomation.EMPTIES, 64, false);
            golem.note("took_mixed", mixed.getDescriptionId());
            golem.takeIn(List.of(mixed, empties), pos);
            return true;
        });
    }

    @Nullable
    Chore load(BlockPos zone, BlockEntity rack) {
        if (!(rack instanceof SoakingBowlBlockEntity bowl)) return null;
        BowlAutomation hands = new BowlAutomation(bowl);
        if (next(hands) < 0) return null;
        return Chore.at(zone, rack.getBlockPos(), () -> !bowl.isRemoved() && next(hands) >= 0, () -> {
            for (int slot = next(hands); slot >= 0; slot = next(hands)) {
                ItemStack poured = golem.cargo().extractItem(slot, 1, false);
                hands.insertItem(BowlAutomation.INPUT, poured, false);
                golem.note("poured_bowl", poured.getDescriptionId());
            }
            if (bowl.isSoaking()) golem.note("bowl_started");
            return true;
        });
    }

    @Nullable
    Predicate<ItemStack> missing(BlockEntity rack) {
        if (!(rack instanceof SoakingBowlBlockEntity bowl)) return null;
        BowlAutomation hands = new BowlAutomation(bowl);
        if (!hands.idle() || next(hands) >= 0) return null;
        List<Predicate<ItemStack>> wanted = new ArrayList<>();
        if (!hands.hasLeaf() && ClayStores.slotOf(golem.cargo(), TobaccoAutomation::fermentedLeaf) < 0) {
            wanted.add(TobaccoAutomation::fermentedLeaf);
        }
        if (!hands.hasLiquid() && ClayStores.slotOf(golem.cargo(), BowlAutomation::liquid) < 0) wanted.add(BowlAutomation::liquid);
        return wanted.stream().reduce(Predicate::or).orElse(null);
    }

    private int next(BowlAutomation hands) {
        IItemHandler cargo = golem.cargo();
        for (int slot = 0; slot < cargo.getSlots(); slot++) {
            ItemStack stack = cargo.getStackInSlot(slot);
            if (!stack.isEmpty() && hands.isItemValid(BowlAutomation.INPUT, stack)) return slot;
        }
        return -1;
    }
}
