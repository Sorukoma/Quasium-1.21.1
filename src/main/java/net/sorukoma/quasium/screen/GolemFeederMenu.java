package net.sorukoma.quasium.screen;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.sorukoma.quasium.golem.GolemFeederBlockEntity;
import net.sorukoma.quasium.golem.GolemMood;
import net.sorukoma.quasium.golem.MarkBlocks;

public class GolemFeederMenu extends AbstractContainerMenu {
    private static final int SLOTS = GolemFeederBlockEntity.SLOTS;

    private final ContainerLevelAccess access;

    public GolemFeederMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new ItemStackHandler(SLOTS), ContainerLevelAccess.NULL);
    }

    public GolemFeederMenu(int containerId, Inventory inventory, IItemHandler food, ContainerLevelAccess access) {
        super(ModMenuTypes.GOLEM_FEEDER_MENU.get(), containerId);
        this.access = access;
        for (int slot = 0; slot < SLOTS; slot++) {
            addSlot(new SlotItemHandler(food, slot, 62 + slot % 3 * 18, 17 + slot / 3 * 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return GolemMood.Food.of(stack) != null;
                }
            });
        }
        for (int slot = 0; slot < 27; slot++) addSlot(new Slot(inventory, slot + 9, 8 + slot % 9 * 18, 84 + slot / 9 * 18));
        for (int slot = 0; slot < 9; slot++) addSlot(new Slot(inventory, slot, 8 + slot * 18, 142));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack moved = stack.copy();
        boolean done = index < SLOTS ? moveItemStackTo(stack, SLOTS, slots.size(), true) : moveItemStackTo(stack, 0, SLOTS, false);
        if (!done) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return moved;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, MarkBlocks.GOLEM_FEEDER.get());
    }
}
