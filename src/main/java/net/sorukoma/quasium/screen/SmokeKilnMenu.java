package net.sorukoma.quasium.screen;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.sorukoma.quasium.advancements.triggers.SmokecraftTrigger;
import net.sorukoma.quasium.compat.SableCompat;
import net.sorukoma.quasium.kiln.KilnBlocks;
import net.sorukoma.quasium.kiln.NoteInfusion;
import net.sorukoma.quasium.kiln.SmokeKilnBlockEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SmokeKilnMenu extends AbstractContainerMenu {
    public static final int ESSENCE_X = 26;
    public static final int TOOL_X = 82;
    public static final int SLOT_Y = 35;
    private static final int KILN_SLOTS = SmokeKilnBlockEntity.TOOL_SLOTS + 1;
    private static final int PLAYER_SLOTS_END = KILN_SLOTS + 36;

    @Nullable
    private final SmokeKilnBlockEntity kiln;
    private final ContainerData data;

    public SmokeKilnMenu(int containerId, Inventory inventory, FriendlyByteBuf buf) {
        this(containerId, inventory,
                inventory.player.level().getBlockEntity(buf.readBlockPos()) instanceof SmokeKilnBlockEntity kiln ? kiln : null,
                new SimpleContainerData(1));
    }

    public SmokeKilnMenu(int containerId, Inventory inventory, @Nullable SmokeKilnBlockEntity kiln, ContainerData data) {
        super(KilnBlocks.SMOKE_KILN_MENU.get(), containerId);
        this.kiln = kiln;
        this.data = data;
        ItemStackHandler items = kiln != null ? kiln.getItems() : new ItemStackHandler(KILN_SLOTS);
        for (int slot = 0; slot < SmokeKilnBlockEntity.TOOL_SLOTS; slot++) {
            addSlot(new ToolSlot(items, slot, TOOL_X + slot * 18, SLOT_Y));
        }
        addSlot(new SlotItemHandler(items, SmokeKilnBlockEntity.ESSENCE_SLOT, ESSENCE_X, SLOT_Y));
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

    public int progressPercent() {
        return data.get(0);
    }

    @Override
    public @NotNull ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < KILN_SLOTS) {
            if (!moveItemStackTo(stack, KILN_SLOTS, PLAYER_SLOTS_END, true)) return ItemStack.EMPTY;
            slot.onTake(player, original);
        } else if (!moveItemStackTo(stack, 0, KILN_SLOTS, false)) {
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
        return kiln != null && !kiln.isRemoved()
                && SableCompat.distanceSqrToBlockCenter(player, kiln.getBlockPos()) <= 64.0;
    }

    private static class ToolSlot extends SlotItemHandler {
        ToolSlot(IItemHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
        }

        @Override
        public void onTake(Player player, ItemStack stack) {
            if (player instanceof ServerPlayer serverPlayer && NoteInfusion.of(stack) != null) {
                SmokecraftTrigger.fire(serverPlayer, "kiln_infuse");
            }
            super.onTake(player, stack);
        }
    }
}
