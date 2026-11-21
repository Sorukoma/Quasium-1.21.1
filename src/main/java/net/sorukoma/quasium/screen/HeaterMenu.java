package net.sorukoma.quasium.screen;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.sorukoma.quasium.block.entity.HeaterBlockEntity;
import net.sorukoma.quasium.item.CharcoalItem;
import net.sorukoma.quasium.screen.ModMenuTypes;
import org.jetbrains.annotations.NotNull;

public class HeaterMenu extends AbstractContainerMenu {
    private final HeaterBlockEntity blockEntity;
    private final DataSlot fuel = DataSlot.standalone();

    public HeaterMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, (HeaterBlockEntity) playerInventory.player.level().getBlockEntity(buf.readBlockPos()));
    }

    public HeaterMenu(int containerId, Inventory playerInventory, HeaterBlockEntity blockEntity) {
        super(ModMenuTypes.HEATER_MENU.get(), containerId);
        this.blockEntity = blockEntity;

        ItemStackHandler handler = blockEntity.getItemHandler();

        // Charcoal slots (15 slots, 5x3 grid)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 5; col++) {
                this.addSlot(new SlotItemHandler(handler, col + row * 5, 8 + col * 18, 18 + row * 18));
            }
        }

        // Fuel slot (right of the grid)
        this.addSlot(new SlotItemHandler(handler, HeaterBlockEntity.FUEL_SLOT, 152, 36) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return stack.getItem() == Items.CHARCOAL;
            }
        });

        // Player inventory
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Player hotbar
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
        addDataSlot(fuel);
    }

    public HeaterMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, (HeaterBlockEntity) null);
    }

    public HeaterBlockEntity getBlockEntity() { return blockEntity; }

    public int fuelTicks() {
        return fuel.get();
    }

    @Override
    public void broadcastChanges() {
        fuel.set(blockEntity.getFuelBurnTimeRemaining());
        super.broadcastChanges();
    }

    @Override
    public boolean stillValid(Player player) {
        // SableCompat: учитывает sub-level Sable, чтобы GUI нагревателя на движущемся
        // контрапшене не закрывался мгновенно из-за координат plot-grid.
        return blockEntity != null && !blockEntity.isRemoved()
                && net.sorukoma.quasium.compat.SableCompat.distanceSqrToBlockCenter(player, blockEntity.getBlockPos()) <= 64.0;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            itemstack = stackInSlot.copy();

            int charcoalSlotsEnd = 15;
            int fuelSlot = 16;

            if (index < fuelSlot) {
                // From block to player
                if (!this.moveItemStackTo(stackInSlot, fuelSlot, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // From player to block
                if (stackInSlot.getItem() instanceof CharcoalItem) {
                    if (!this.moveItemStackTo(stackInSlot, 0, charcoalSlotsEnd, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (stackInSlot.getItem() == Items.CHARCOAL) {
                    if (!this.moveItemStackTo(stackInSlot, charcoalSlotsEnd, fuelSlot, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    return ItemStack.EMPTY;
                }
            }

            if (stackInSlot.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return itemstack;
    }
}