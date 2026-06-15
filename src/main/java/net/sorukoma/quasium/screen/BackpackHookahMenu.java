package net.sorukoma.quasium.screen;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.sorukoma.quasium.data.BackpackHookahState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.SimpleContainer;
import net.sorukoma.quasium.item.CalloudItem;
import net.sorukoma.quasium.item.ModItems;

public class BackpackHookahMenu extends AbstractContainerMenu {
    @Nullable
    public final BackpackHookahState state;

    private static final int COAL_SLOT = 0;
    private static final int CONTENT_SLOT = 1;
    private static final int PLAYER_INV_START = 2;
    private static final int PLAYER_INV_END = 29;
    private static final int HOTBAR_START = 29;
    private static final int HOTBAR_END = 38;

    // Server-side constructor
    public BackpackHookahMenu(int containerId, Inventory playerInventory, BackpackHookahState state) {
        super(net.sorukoma.quasium.screen.ModMenuTypes.BACKPACK_HOOKAH_MENU.get(), containerId);
        this.state = state;

        this.addSlot(new SlotItemHandler(state.getItemHandler(), 0, 80, 17) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return state.getItemHandler().isItemValid(0, stack);
            }
        });

        this.addSlot(new SlotItemHandler(state.getItemHandler(), 1, 80, 53) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return state.getItemHandler().isItemValid(1, stack);
            }

            @Override
            public int getMaxStackSize() { return 1; }

            @Override
            public int getMaxStackSize(@NotNull ItemStack stack) { return 1; }

            @Override
            public ItemStack safeInsert(ItemStack stack, int increment) {
                int countToInsert = Math.min(increment, stack.getCount());
                ItemStack insertStack = stack.copyWithCount(countToInsert);
                ItemStack remainder = state.getItemHandler().insertItem(1, insertStack, false);

                if (ItemStack.isSameItemSameComponents(remainder, insertStack)) {
                    return stack;
                }

                this.setChanged();

                if (remainder.isEmpty()) {
                    stack.shrink(countToInsert);
                    return stack;
                }

                return remainder;
            }
        });

        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }

    // Client-side constructor (no state, uses SimpleContainer for display)
    public BackpackHookahMenu(int containerId, Inventory playerInventory) {
        super(net.sorukoma.quasium.screen.ModMenuTypes.BACKPACK_HOOKAH_MENU.get(), containerId);
        this.state = null;

        SimpleContainer container = new SimpleContainer(2);

        this.addSlot(new Slot(container, 0, 80, 17) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return stack.getItem() instanceof CalloudItem;
            }
        });

        this.addSlot(new Slot(container, 1, 80, 53) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return stack.is(ModItems.HOOKAH_BOWL.get());
            }
            @Override
            public int getMaxStackSize() { return 1; }
            @Override
            public int getMaxStackSize(@NotNull ItemStack stack) { return 1; }
        });

        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        result = stack.copy();

        if (index == COAL_SLOT || index == CONTENT_SLOT) {
            if (!this.moveItemStackTo(stack, PLAYER_INV_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (index >= PLAYER_INV_START && index < HOTBAR_END) {
            if (state != null) {
                if (stack.getItem() instanceof CalloudItem) {
                    if (!this.moveItemStackTo(stack, COAL_SLOT, COAL_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (state.getItemHandler().isItemValid(1, stack)) {
                    ItemStack remainder = state.getItemHandler().insertItem(1, stack, false);
                    if (!remainder.isEmpty()) {
                        return ItemStack.EMPTY;
                    }
                    stack.shrink(stack.getCount());
                    slot.setChanged();
                    return result;
                }
            }
            if (index >= PLAYER_INV_START && index < PLAYER_INV_END) {
                if (!this.moveItemStackTo(stack, HOTBAR_START, HOTBAR_END, false)) return ItemStack.EMPTY;
            } else if (index >= HOTBAR_START && index < HOTBAR_END) {
                if (!this.moveItemStackTo(stack, PLAYER_INV_START, PLAYER_INV_END, false)) return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();

        if (stack.getCount() == result.getCount() && ItemStack.isSameItemSameComponents(stack, result)) return ItemStack.EMPTY;

        slot.onTake(player, stack);
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return true; // Always valid since it's on the player's back
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (state != null && !player.level().isClientSide()) {
            state.saveToPlayer();
            state.syncToClient();
        }
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 8 + l * 18, 84 + i * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }
}
