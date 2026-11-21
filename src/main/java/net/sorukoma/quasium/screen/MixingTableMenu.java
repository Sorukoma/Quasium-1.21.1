package net.sorukoma.quasium.screen;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.sorukoma.quasium.block.MixingBlocks;
import net.sorukoma.quasium.block.entity.MixingTableBlockEntity;
import net.sorukoma.quasium.compat.SableCompat;
import net.sorukoma.quasium.item.EssenceBottleItem;
import net.sorukoma.quasium.item.FocusUpgradeItem;
import net.sorukoma.quasium.item.ModItems;
import net.sorukoma.quasium.magic.FocusEssence;
import net.sorukoma.quasium.mind.research.Research;
import net.sorukoma.quasium.recipe.MixingRecipe;
import net.sorukoma.quasium.recipe.MixingRecipes;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class MixingTableMenu extends AbstractContainerMenu {
    public static final int ESSENCE = 0;
    public static final int CATALYST = 1;
    public static final int RESULT = 2;
    public static final int BOWL = 3;
    public static final int UPGRADE = 4;
    public static final int FOCUS = 5;
    private static final int TABLE_SLOTS = FOCUS + FocusEssence.MAX_SLOTS;
    private static final int INVENTORY_END = TABLE_SLOTS + 27;
    private static final int HOTBAR_END = INVENTORY_END + 9;

    @Nullable
    private final MixingTableBlockEntity table;
    private final ItemStackHandler items;
    private final Player player;

    public MixingTableMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, null, new ItemStackHandler(3), new ItemStackHandler(MixingTableBlockEntity.BOWL_VIEW_SIZE));
    }

    public MixingTableMenu(int containerId, Inventory inventory, MixingTableBlockEntity table) {
        this(containerId, inventory, table, table.getItems(), table.getBowlView());
    }

    private MixingTableMenu(int containerId, Inventory inventory, @Nullable MixingTableBlockEntity table,
                            ItemStackHandler items, ItemStackHandler bowlView) {
        super(MixingBlocks.MIXING_TABLE_MENU.get(), containerId);
        this.table = table;
        this.items = items;
        this.player = inventory.player;
        addSlot(new RuleSlot(items, MixingTableBlockEntity.ESSENCE_SLOT, 44, 17, 1, MixingTableMenu::isChargedBottle));
        addSlot(new RuleSlot(items, MixingTableBlockEntity.CATALYST_SLOT, 80, 17, 64, MixingRecipes::isCatalyst));
        addSlot(new ResultSlot(134, 17));
        addSlot(new RuleSlot(items, MixingTableBlockEntity.BOWL_SLOT, 26, 40, 1, stack -> stack.is(ModItems.HOOKAH_BOWL.get())));
        addSlot(new RuleSlot(bowlView, MixingTableBlockEntity.UPGRADE_SLOT, 134, 40, 1,
                stack -> hasBowl() && stack.getItem() instanceof FocusUpgradeItem));
        for (int i = 0; i < FocusEssence.MAX_SLOTS; i++) {
            addSlot(new RuleSlot(bowlView, MixingTableBlockEntity.FOCUS_SLOT + i, 62 + i * 18, 40, 1,
                    stack -> hasBowl() && isChargedBottle(stack)));
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }
    }

    private static boolean isChargedBottle(ItemStack stack) {
        return stack.getItem() instanceof EssenceBottleItem && EssenceBottleItem.amountOf(stack) > 0;
    }

    public boolean hasBowl() {
        return !items.getStackInSlot(MixingTableBlockEntity.BOWL_SLOT).isEmpty();
    }

    public List<String> focusEssences() {
        List<String> essences = new ArrayList<>();
        for (int i = 0; i < FocusEssence.MAX_SLOTS; i++) {
            ItemStack bottle = getSlot(FOCUS + i).getItem();
            if (isChargedBottle(bottle)) essences.add(EssenceBottleItem.typeOf(bottle));
        }
        return essences;
    }

    @Nullable
    private MixingRecipe recipe() {
        MixingRecipe recipe = MixingRecipes.find(items.getStackInSlot(MixingTableBlockEntity.ESSENCE_SLOT),
                items.getStackInSlot(MixingTableBlockEntity.CATALYST_SLOT));
        if (recipe == null || recipe.research().isEmpty() || Research.studied(player, recipe.research().get())) return recipe;
        return null;
    }

    private void craft() {
        MixingRecipe recipe = recipe();
        if (recipe == null) return;
        items.setStackInSlot(MixingTableBlockEntity.ESSENCE_SLOT, recipe.drain(items.getStackInSlot(MixingTableBlockEntity.ESSENCE_SLOT)));
        items.extractItem(MixingTableBlockEntity.CATALYST_SLOT, 1, false);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack moved = stack.copy();
        if (index < TABLE_SLOTS) {
            if (!moveItemStackTo(stack, TABLE_SLOTS, HOTBAR_END, true)) return ItemStack.EMPTY;
        } else if (!moveIntoTable(stack) && !moveWithinInventory(stack, index)) {
            return ItemStack.EMPTY;
        }
        if (index == RESULT) {
            slot.onTake(player, moved);
        } else if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return moved;
    }

    private boolean moveIntoTable(ItemStack stack) {
        if (stack.is(ModItems.HOOKAH_BOWL.get())) return moveItemStackTo(stack, BOWL, BOWL + 1, false);
        if (stack.getItem() instanceof FocusUpgradeItem) return moveItemStackTo(stack, UPGRADE, UPGRADE + 1, false);
        if (isChargedBottle(stack)) {
            return moveItemStackTo(stack, FOCUS, TABLE_SLOTS, false) || moveItemStackTo(stack, ESSENCE, ESSENCE + 1, false);
        }
        return moveItemStackTo(stack, CATALYST, CATALYST + 1, false);
    }

    private boolean moveWithinInventory(ItemStack stack, int index) {
        return index < INVENTORY_END
                ? moveItemStackTo(stack, INVENTORY_END, HOTBAR_END, false)
                : moveItemStackTo(stack, TABLE_SLOTS, INVENTORY_END, false);
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.index != RESULT && super.canTakeItemForPickAll(stack, slot);
    }

    @Override
    public boolean stillValid(Player player) {
        return table != null && !table.isRemoved()
                && SableCompat.distanceSqrToBlockCenter(player, table.getBlockPos()) <= 64.0;
    }

    private static class RuleSlot extends SlotItemHandler {
        private final int limit;
        private final Predicate<ItemStack> rule;

        RuleSlot(IItemHandler handler, int index, int x, int y, int limit, Predicate<ItemStack> rule) {
            super(handler, index, x, y);
            this.limit = limit;
            this.rule = rule;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return rule.test(stack);
        }

        @Override
        public int getMaxStackSize() {
            return limit;
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return Math.min(limit, stack.getMaxStackSize());
        }
    }

    private class ResultSlot extends Slot {
        ResultSlot(int x, int y) {
            super(new SimpleContainer(1), 0, x, y);
        }

        @Override
        public ItemStack getItem() {
            MixingRecipe recipe = recipe();
            return recipe == null ? ItemStack.EMPTY : new ItemStack(recipe.result());
        }

        @Override
        public ItemStack remove(int amount) {
            return getItem();
        }

        @Override
        public void set(ItemStack stack) {
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public void onTake(Player player, ItemStack stack) {
            craft();
            super.onTake(player, stack);
        }
    }
}
