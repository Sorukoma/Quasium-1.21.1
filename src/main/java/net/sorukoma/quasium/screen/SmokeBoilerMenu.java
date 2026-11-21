package net.sorukoma.quasium.screen;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.sorukoma.quasium.compat.SableCompat;
import net.sorukoma.quasium.draft.GeneratorBlocks;
import net.sorukoma.quasium.draft.SmokeBoilerBlockEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SmokeBoilerMenu extends AbstractContainerMenu {
    public static final int FUEL_X = 98;
    public static final int FUEL_Y = 53;
    private static final int MACHINE_SLOTS = 1;
    private static final int PLAYER_SLOTS_END = MACHINE_SLOTS + 36;

    @Nullable
    private final SmokeBoilerBlockEntity boiler;
    private final ContainerData data;

    public SmokeBoilerMenu(int containerId, Inventory inventory, FriendlyByteBuf buf) {
        this(containerId, inventory,
                inventory.player.level().getBlockEntity(buf.readBlockPos()) instanceof SmokeBoilerBlockEntity boiler ? boiler : null,
                new SimpleContainerData(SmokeBoilerBlockEntity.DATA_COUNT));
    }

    public SmokeBoilerMenu(int containerId, Inventory inventory, @Nullable SmokeBoilerBlockEntity boiler, ContainerData data) {
        super(GeneratorBlocks.SMOKE_BOILER_MENU.get(), containerId);
        this.boiler = boiler;
        this.data = data;
        ItemStackHandler items = boiler != null ? boiler.getItems() : new ItemStackHandler(MACHINE_SLOTS);
        addSlot(new SlotItemHandler(items, SmokeBoilerBlockEntity.FUEL_SLOT, FUEL_X, FUEL_Y));
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
        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(stack, MACHINE_SLOTS, PLAYER_SLOTS_END, true)) return ItemStack.EMPTY;
        } else if (stack.getBurnTime(RecipeType.SMELTING) <= 0 || !moveItemStackTo(stack, 0, MACHINE_SLOTS, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return boiler != null && !boiler.isRemoved()
                && SableCompat.distanceSqrToBlockCenter(player, boiler.getBlockPos()) <= 64.0;
    }
}
