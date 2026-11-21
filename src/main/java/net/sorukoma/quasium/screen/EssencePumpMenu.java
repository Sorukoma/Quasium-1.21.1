package net.sorukoma.quasium.screen;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.sorukoma.quasium.compat.SableCompat;
import net.sorukoma.quasium.draft.EssencePumpBlockEntity;
import net.sorukoma.quasium.draft.MachineBlocks;
import org.jetbrains.annotations.Nullable;

public class EssencePumpMenu extends AbstractContainerMenu {
    private static final int PUMP_SLOTS = 2;
    private static final int PLAYER_SLOTS_END = PUMP_SLOTS + 36;

    @Nullable
    private final EssencePumpBlockEntity pump;
    private final DataSlot progress;

    public EssencePumpMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, null, EssencePumpBlockEntity.createItems(() -> {}), DataSlot.standalone());
    }

    public EssencePumpMenu(int containerId, Inventory inventory, EssencePumpBlockEntity pump) {
        this(containerId, inventory, pump, pump.getItems(), new DataSlot() {
            @Override
            public int get() {
                return pump.progress();
            }

            @Override
            public void set(int value) {
            }
        });
    }

    private EssencePumpMenu(int containerId, Inventory inventory, @Nullable EssencePumpBlockEntity pump, IItemHandler items,
                            DataSlot progress) {
        super(MachineBlocks.ESSENCE_PUMP_MENU.get(), containerId);
        this.pump = pump;
        this.progress = progress;
        addSlot(new SlotItemHandler(items, EssencePumpBlockEntity.INPUT, 56, 35));
        addSlot(new SlotItemHandler(items, EssencePumpBlockEntity.OUTPUT, 116, 35));
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }
        addDataSlot(progress);
    }

    public int progress() {
        return progress.get();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack moved = stack.copy();
        boolean done = index < PUMP_SLOTS
                ? moveItemStackTo(stack, PUMP_SLOTS, PLAYER_SLOTS_END, true)
                : moveItemStackTo(stack, EssencePumpBlockEntity.INPUT, EssencePumpBlockEntity.INPUT + 1, false);
        if (!done) return ItemStack.EMPTY;
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return moved;
    }

    @Override
    public boolean stillValid(Player player) {
        return pump != null && !pump.isRemoved() && SableCompat.distanceSqrToBlockCenter(player, pump.getBlockPos()) <= 64.0;
    }
}
