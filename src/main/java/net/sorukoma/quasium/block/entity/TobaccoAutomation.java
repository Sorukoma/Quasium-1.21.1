package net.sorukoma.quasium.block.entity;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.sorukoma.quasium.component.ModDataComponents;
import net.sorukoma.quasium.item.JerkyItem;
import net.sorukoma.quasium.item.TobaccoVarieties;
import net.sorukoma.quasium.recipe.DryingRecipes;

public final class TobaccoAutomation {
    private static final int DRIED = 2;

    private TobaccoAutomation() {
    }

    public static boolean freshLeaf(ItemStack stack) {
        return TobaccoVarieties.isLeaf(stack) && !stack.has(ModDataComponents.FERMENTED.get())
                && stack.getOrDefault(ModDataComponents.DRYING_STAGE.get(), 0) < DRIED;
    }

    public static boolean driedLeaf(ItemStack stack) {
        return TobaccoVarieties.isLeaf(stack) && !stack.has(ModDataComponents.FERMENTED.get())
                && stack.getOrDefault(ModDataComponents.DRYING_STAGE.get(), 0) >= DRIED;
    }

    public static boolean fermentedLeaf(ItemStack stack) {
        return TobaccoVarieties.isLeaf(stack) && stack.getOrDefault(ModDataComponents.FERMENTED.get(), false)
                && !stack.getOrDefault(ModDataComponents.SPOILED.get(), false);
    }

    static boolean needsDrying(ItemStack stack) {
        if (stack.getItem() instanceof JerkyItem) {
            return stack.getOrDefault(ModDataComponents.DRYING_PROGRESS.get(), 0) < JerkyItem.MAX_DRYING_PERCENT;
        }
        return freshLeaf(stack) || DryingRecipes.isValidInput(stack);
    }

    static boolean doneDrying(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (TobaccoVarieties.isLeaf(stack)) return stack.getOrDefault(ModDataComponents.DRYING_STAGE.get(), 0) >= DRIED;
        return !needsDrying(stack);
    }

    public static IItemHandler shelf(DryingShelfBlockEntity shelf) {
        ItemStackHandler slot = shelf.getItemHandler();
        return new OneSlot() {
            @Override
            public ItemStack getStackInSlot(int index) {
                return slot.getStackInSlot(0);
            }

            @Override
            public ItemStack insertItem(int index, ItemStack stack, boolean simulate) {
                if (!needsDrying(stack) || !slot.getStackInSlot(0).isEmpty()) return stack;
                return slot.insertItem(0, stack, simulate);
            }

            @Override
            public ItemStack extractItem(int index, int amount, boolean simulate) {
                return doneDrying(slot.getStackInSlot(0)) ? slot.extractItem(0, amount, simulate) : ItemStack.EMPTY;
            }

            @Override
            public int getSlotLimit(int index) {
                return 1;
            }

            @Override
            public boolean isItemValid(int index, ItemStack stack) {
                return needsDrying(stack);
            }
        };
    }

    public static IItemHandler barrel(FermentationBarrelBlockEntity barrel) {
        ItemStackHandler slot = barrel.getItemHandler();
        return new OneSlot() {
            @Override
            public ItemStack getStackInSlot(int index) {
                return slot.getStackInSlot(0);
            }

            @Override
            public ItemStack insertItem(int index, ItemStack stack, boolean simulate) {
                ItemStack inside = slot.getStackInSlot(0);
                if (!isItemValid(index, stack)) return stack;
                int moved = Math.min(stack.getCount(), slot.getSlotLimit(0) - inside.getCount());
                if (moved <= 0) return stack;
                if (!simulate) {
                    slot.setStackInSlot(0, (inside.isEmpty() ? stack : inside).copyWithCount(inside.getCount() + moved));
                    barrel.startFermentation();
                }
                return stack.copyWithCount(stack.getCount() - moved);
            }

            @Override
            public ItemStack extractItem(int index, int amount, boolean simulate) {
                ItemStack inside = slot.getStackInSlot(0);
                if (!barrel.isReady() || inside.isEmpty() || amount <= 0) return ItemStack.EMPTY;
                if (amount < inside.getCount()) return slot.extractItem(0, amount, simulate);
                return simulate ? inside.copy() : barrel.takeResult();
            }

            @Override
            public int getSlotLimit(int index) {
                return slot.getSlotLimit(0);
            }

            @Override
            public boolean isItemValid(int index, ItemStack stack) {
                return !barrel.isFermenting() && !barrel.isReady() && driedLeaf(stack)
                        && FermentationBarrelBlockEntity.sameBatch(slot.getStackInSlot(0), stack);
            }
        };
    }

    private abstract static class OneSlot implements IItemHandler {
        @Override
        public int getSlots() {
            return 1;
        }
    }
}
