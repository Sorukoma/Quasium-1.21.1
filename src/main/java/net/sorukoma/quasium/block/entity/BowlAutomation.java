package net.sorukoma.quasium.block.entity;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.sorukoma.quasium.component.ModDataComponents;
import net.sorukoma.quasium.item.ModItems;

public final class BowlAutomation implements IItemHandler {
    public static final int RESULT = 0;
    public static final int EMPTIES = 1;
    public static final int INPUT = 2;
    private static final int MOLASSES_MB = 5;
    private static final int LIQUID_CAP_MB = 20;
    private static final int ESSENCE_DEFAULT_MB = 20;

    private final SoakingBowlBlockEntity bowl;

    public BowlAutomation(SoakingBowlBlockEntity bowl) {
        this.bowl = bowl;
    }

    public static boolean liquid(ItemStack stack) {
        return stack.is(ModItems.MOLASSES_BOTTLE.get()) || stack.is(ModItems.ESSENCE_BOTTLE.get());
    }

    public boolean idle() {
        return !bowl.isSoaking() && !bowl.holdsResult() && bowl.getCatalyst().isEmpty();
    }

    public boolean hasLeaf() {
        return bowl.getTobaccoGrams() > 0;
    }

    public boolean hasLiquid() {
        return bowl.getMolassesMb() + bowl.getEssenceMb() > 0;
    }

    private ItemStackHandler slots() {
        return bowl.getItemHandler();
    }

    @Override
    public int getSlots() {
        return 3;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        if (slot == RESULT) return bowl.isReady() ? bowl.getResultItem() : ItemStack.EMPTY;
        if (slot == EMPTIES) return slots().getStackInSlot(SoakingBowlBlockEntity.EMPTIES_SLOT);
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty() || !isItemValid(slot, stack)) return stack;
        if (!simulate) pourIn(stack);
        return stack.copyWithCount(stack.getCount() - 1);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount <= 0) return ItemStack.EMPTY;
        if (slot == RESULT) {
            if (!bowl.isReady() || !bowl.holdsResult()) return ItemStack.EMPTY;
            return simulate ? bowl.getResultItem().copy() : bowl.takeResult();
        }
        if (slot != EMPTIES) return ItemStack.EMPTY;
        ItemStack empties = slots().getStackInSlot(SoakingBowlBlockEntity.EMPTIES_SLOT);
        int taken = Math.min(amount, empties.getCount());
        if (taken <= 0) return ItemStack.EMPTY;
        if (!simulate) slots().setStackInSlot(SoakingBowlBlockEntity.EMPTIES_SLOT, empties.copyWithCount(empties.getCount() - taken));
        return empties.copyWithCount(taken);
    }

    @Override
    public int getSlotLimit(int slot) {
        return slot == INPUT ? 1 : slots().getSlotLimit(SoakingBowlBlockEntity.EMPTIES_SLOT);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        if (slot != INPUT || !idle()) return false;
        if (TobaccoAutomation.fermentedLeaf(stack)) return !hasLeaf();
        return liquid(stack) && !hasLiquid() && essence(stack) <= LIQUID_CAP_MB && emptiesRoom();
    }

    private boolean emptiesRoom() {
        return slots().getStackInSlot(SoakingBowlBlockEntity.EMPTIES_SLOT).getCount() < slots().getSlotLimit(SoakingBowlBlockEntity.EMPTIES_SLOT);
    }

    private static int essence(ItemStack bottle) {
        if (!bottle.is(ModItems.ESSENCE_BOTTLE.get())) return 0;
        return bottle.getOrDefault(ModDataComponents.ESSENCE_AMOUNT.get(), ESSENCE_DEFAULT_MB);
    }

    private void pourIn(ItemStack stack) {
        if (TobaccoAutomation.fermentedLeaf(stack)) {
            bowl.addLeaf(stack);
        } else {
            if (stack.is(ModItems.MOLASSES_BOTTLE.get())) bowl.setMolassesMb(bowl.getMolassesMb() + MOLASSES_MB);
            else bowl.addEssence(stack.getOrDefault(ModDataComponents.ESSENCE_TYPE.get(), "unknown"), essence(stack));
            ItemStack empties = slots().getStackInSlot(SoakingBowlBlockEntity.EMPTIES_SLOT);
            slots().setStackInSlot(SoakingBowlBlockEntity.EMPTIES_SLOT, new ItemStack(Items.GLASS_BOTTLE, empties.getCount() + 1));
        }
        if (bowl.canStartSoaking()) bowl.stir();
    }
}
