package net.sorukoma.quasium.screen;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.sorukoma.quasium.block.ModBlocks;
import net.sorukoma.quasium.block.entity.HookahBlockEntity;
import net.sorukoma.quasium.item.ModItems;
import org.jetbrains.annotations.NotNull;

public class HookahMenu extends AbstractContainerMenu {
    public final HookahBlockEntity blockEntity;
    private final Level level;

    private static final int COAL_SLOT = 0;
    private static final int CONTENT_SLOT = 1;
    private static final int PLAYER_INV_START = 2;
    private static final int PLAYER_INV_END = 29;
    private static final int HOTBAR_START = 29;
    private static final int HOTBAR_END = 38;

    public HookahMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, playerInventory.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public HookahMenu(int containerId, Inventory playerInventory, BlockEntity be) {
        super(ModMenuTypes.HOOKAH_MENU.get(), containerId);
        this.blockEntity = (HookahBlockEntity) be;
        this.level = playerInventory.player.level();

        // Слот углей (itemHandler slot 0)
        this.addSlot(new SlotItemHandler(blockEntity.getItemHandler(), 0, 80, 17) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return blockEntity.getItemHandler().isItemValid(0, stack);
            }
        });

        // Слот содержимого — ИСКЛЮЧИТЕЛЬНО HookahBowlItem
        this.addSlot(new SlotItemHandler(blockEntity.getItemHandler(), 1, 80, 53) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return stack.is(ModItems.HOOKAH_BOWL.get());
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public int getMaxStackSize(@NotNull ItemStack stack) {
                return 1;
            }
        });

        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        result = stack.copy();

        if (index == COAL_SLOT || index == CONTENT_SLOT) {
            if (!this.moveItemStackTo(stack, PLAYER_INV_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (index >= PLAYER_INV_START && index < HOTBAR_END) {
            // Угли → calloud slot (каллауд)
            if (stack.getItem() instanceof net.sorukoma.quasium.item.CalloudItem) {
                if (!this.moveItemStackTo(stack, COAL_SLOT, COAL_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            }
            // HookahBowl → content slot
            else if (stack.is(ModItems.HOOKAH_BOWL.get())) {
                if (!this.moveItemStackTo(stack, CONTENT_SLOT, CONTENT_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            }
            // Swap main inv ↔ hotbar
            else {
                if (index >= PLAYER_INV_START && index < PLAYER_INV_END) {
                    if (!this.moveItemStackTo(stack, HOTBAR_START, HOTBAR_END, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index >= HOTBAR_START && index < HOTBAR_END) {
                    if (!this.moveItemStackTo(stack, PLAYER_INV_START, PLAYER_INV_END, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }
        } else {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        if (stack.getCount() == result.getCount() && ItemStack.isSameItemSameComponents(stack, result)) {
            return ItemStack.EMPTY;
        }

        slot.onTake(player, stack);
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()),
                player, ModBlocks.HOOKAH_BLOCK.get());
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