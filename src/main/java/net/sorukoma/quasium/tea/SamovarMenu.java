package net.sorukoma.quasium.tea;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.sorukoma.quasium.compat.SableCompat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SamovarMenu extends AbstractContainerMenu {
    public static final int LEAVES_X = 56;
    public static final int LEAVES_Y = 17;
    public static final int FUEL_X = 56;
    public static final int FUEL_Y = 53;
    public static final int TEAPOT_X = 116;
    public static final int TEAPOT_Y = 35;
    private static final int PLAYER_SLOTS_END = SamovarBlockEntity.SLOTS + 36;

    @Nullable
    private final SamovarBlockEntity samovar;
    private final ContainerData data;

    public SamovarMenu(int containerId, Inventory inventory, FriendlyByteBuf buf) {
        this(containerId, inventory,
                inventory.player.level().getBlockEntity(buf.readBlockPos()) instanceof SamovarBlockEntity samovar ? samovar : null,
                new SimpleContainerData(SamovarBlockEntity.DATA_COUNT));
    }

    public SamovarMenu(int containerId, Inventory inventory, @Nullable SamovarBlockEntity samovar, ContainerData data) {
        super(TeaBlocks.SAMOVAR_MENU.get(), containerId);
        this.samovar = samovar;
        this.data = data;
        ItemStackHandler items = samovar != null ? samovar.getItems() : new ItemStackHandler(SamovarBlockEntity.SLOTS);
        addSlot(new SlotItemHandler(items, SamovarBlockEntity.LEAVES_SLOT, LEAVES_X, LEAVES_Y));
        addSlot(new SlotItemHandler(items, SamovarBlockEntity.FUEL_SLOT, FUEL_X, FUEL_Y));
        addSlot(new SlotItemHandler(items, SamovarBlockEntity.TEAPOT_SLOT, TEAPOT_X, TEAPOT_Y));
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }
        addDataSlots(data);
    }

    public int value(int index) {
        return data.get(index);
    }

    @Override
    public @NotNull ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < SamovarBlockEntity.SLOTS) {
            if (!moveItemStackTo(stack, SamovarBlockEntity.SLOTS, PLAYER_SLOTS_END, true)) return ItemStack.EMPTY;
        } else if (!moveIntoSamovar(stack)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    private boolean moveIntoSamovar(ItemStack stack) {
        for (int index = 0; index < SamovarBlockEntity.SLOTS; index++) {
            if (slots.get(index).mayPlace(stack) && moveItemStackTo(stack, index, index + 1, false)) return true;
        }
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return samovar != null && !samovar.isRemoved()
                && SableCompat.distanceSqrToBlockCenter(player, samovar.getBlockPos()) <= 64.0;
    }
}
