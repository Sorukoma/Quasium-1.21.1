package net.sorukoma.quasium.cart;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.sorukoma.quasium.data.BackpackHookahState;
import org.jetbrains.annotations.Nullable;

public class CartHookahMenu extends AbstractContainerMenu {
    private static final int[][] HOOKAH_SLOTS = {{80, 17}, {80, 53}, {116, 17}, {116, 35}, {116, 53}};
    private static final int INVENTORY_START = HOOKAH_SLOTS.length;
    private static final int HOTBAR_START = INVENTORY_START + 27;
    private static final int HEAT_SCALE = 100;

    @Nullable
    private final HookahMinecart cart;
    private final ContainerData data = new SimpleContainerData(3);

    public CartHookahMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, null, new HookahMinecart.Hookah());
    }

    public CartHookahMenu(int containerId, Inventory inventory, HookahMinecart cart) {
        this(containerId, inventory, cart, cart.hookah());
    }

    private CartHookahMenu(int containerId, Inventory inventory, @Nullable HookahMinecart cart, BackpackHookahState hookah) {
        super(HookahCarts.HOOKAH_MINECART_MENU.get(), containerId);
        this.cart = cart;
        for (int slot = 0; slot < HOOKAH_SLOTS.length; slot++) {
            addSlot(new SlotItemHandler(hookah.getItemHandler(), slot, HOOKAH_SLOTS[slot][0], HOOKAH_SLOTS[slot][1]));
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 142));
        }
        addDataSlots(data);
        readHookah();
    }

    public float heat() {
        return data.get(0) / (float) HEAT_SCALE;
    }

    public int tobacco() {
        return data.get(1);
    }

    public int warmup() {
        return data.get(2);
    }

    private void readHookah() {
        if (cart == null) return;
        data.set(0, Math.round(cart.hookah().getTotalHeat() * HEAT_SCALE));
        data.set(1, cart.hookah().getTotalTobaccoWeight());
        data.set(2, cart.hookah().warmupPuffsRemaining());
    }

    @Override
    public void broadcastChanges() {
        readHookah();
        super.broadcastChanges();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack moved = stack.copy();
        boolean fits;
        if (index < INVENTORY_START) {
            fits = moveItemStackTo(stack, INVENTORY_START, slots.size(), true);
        } else {
            fits = moveItemStackTo(stack, 0, INVENTORY_START, false)
                    || (index < HOTBAR_START ? moveItemStackTo(stack, HOTBAR_START, slots.size(), false)
                    : moveItemStackTo(stack, INVENTORY_START, HOTBAR_START, false));
        }
        if (!fits) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, stack);
        return moved;
    }

    @Override
    public boolean stillValid(Player player) {
        return cart == null || (!cart.isRemoved() && player.canInteractWithEntity(cart.getBoundingBox(), 4.0));
    }
}
