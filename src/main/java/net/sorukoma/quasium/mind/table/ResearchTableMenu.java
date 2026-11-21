package net.sorukoma.quasium.mind.table;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
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
import net.sorukoma.quasium.mind.MindRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ResearchTableMenu extends AbstractContainerMenu {
    public static final int WRITE_BUTTON = 0;
    public static final int INPUT_X = 26;
    public static final int PAPER_Y = 17;
    public static final int INK_Y = 53;
    public static final int OUTPUT_X = 90;
    public static final int OUTPUT_Y = 35;
    private static final int TABLE_SLOTS = 3;
    private static final int PLAYER_SLOTS_END = TABLE_SLOTS + 36;

    @Nullable
    private final ResearchTableBlockEntity table;
    private final ContainerData data;

    public ResearchTableMenu(int containerId, Inventory inventory, FriendlyByteBuf buf) {
        this(containerId, inventory,
                inventory.player.level().getBlockEntity(buf.readBlockPos()) instanceof ResearchTableBlockEntity table ? table : null,
                new SimpleContainerData(2));
    }

    public ResearchTableMenu(int containerId, Inventory inventory, @Nullable ResearchTableBlockEntity table, ContainerData data) {
        super(MindRegistry.RESEARCH_TABLE_MENU.get(), containerId);
        this.table = table;
        this.data = data;
        ItemStackHandler items = table != null ? table.getItems() : new ItemStackHandler(TABLE_SLOTS);
        addSlot(new SlotItemHandler(items, ResearchTableBlockEntity.PAPER, INPUT_X, PAPER_Y));
        addSlot(new SlotItemHandler(items, ResearchTableBlockEntity.INK, INPUT_X, INK_Y));
        addSlot(new SlotItemHandler(items, ResearchTableBlockEntity.OUTPUT, OUTPUT_X, OUTPUT_Y) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return false;
            }
        });
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

    public float progress() {
        return data.get(0) / (float) ResearchTableBlockEntity.writeTicks();
    }

    public int queued() {
        return data.get(1);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        return id == WRITE_BUTTON && table != null && player instanceof ServerPlayer serverPlayer && table.order(serverPlayer);
    }

    @Override
    public @NotNull ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < TABLE_SLOTS) {
            if (!moveItemStackTo(stack, TABLE_SLOTS, PLAYER_SLOTS_END, true)) return ItemStack.EMPTY;
            slot.onTake(player, original);
        } else if (!moveItemStackTo(stack, 0, ResearchTableBlockEntity.OUTPUT, false)) {
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
        return table != null && !table.isRemoved() && SableCompat.distanceSqrToBlockCenter(player, table.getBlockPos()) <= 64.0;
    }
}
