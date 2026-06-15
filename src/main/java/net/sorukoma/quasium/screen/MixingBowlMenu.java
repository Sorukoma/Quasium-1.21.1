package net.sorukoma.quasium.screen;

import com.mojang.logging.LogUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import net.sorukoma.quasium.component.ModDataComponents;
import net.sorukoma.quasium.item.ModItems;
import net.sorukoma.quasium.item.SelfMixedTobaccoItem;
import net.sorukoma.quasium.item.TobaccoItem;
import org.slf4j.Logger;

public class MixingBowlMenu extends AbstractContainerMenu {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final ItemStackHandler itemHandler;
    private final ContainerLevelAccess access;

    public MixingBowlMenu(int pContainerId, Inventory pPlayerInventory) {
        this(pContainerId, pPlayerInventory, new ItemStackHandler(3));
    }

    public MixingBowlMenu(int pContainerId, Inventory pPlayerInventory, RegistryFriendlyByteBuf pBuffer) {
        this(pContainerId, pPlayerInventory, new ItemStackHandler(3));
    }

    public MixingBowlMenu(int pContainerId, Inventory pPlayerInventory, ItemStackHandler pItemHandler) {
        super(ModMenuTypes.MIXING_BOWL_MENU.get(), pContainerId);
        this.itemHandler = pItemHandler;
        this.access = ContainerLevelAccess.NULL;

        // Mixing Bowl Slots
        this.addSlot(new SlotItemHandler(itemHandler, 0, 44, 26) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                LOGGER.debug("MixingBowlMenu: Slot 0 mayPlace called for item: {}, has TOBACCO_WEIGHT: {}", stack.getItem().getDescriptionId(), stack.has(ModDataComponents.TOBACCO_WEIGHT.get()));
                if (stack.has(ModDataComponents.TOBACCO_WEIGHT.get())) {
                    LOGGER.debug("MixingBowlMenu: Slot 0 item {} has weight {}", stack.getItem().getDescriptionId(), stack.get(ModDataComponents.TOBACCO_WEIGHT.get()));
                }
                return stack.getItem() instanceof TobaccoItem || stack.is(ModItems.MIXED_TOBACCO.get()) || stack.getItem() instanceof SelfMixedTobaccoItem;
            }
        });
        this.addSlot(new SlotItemHandler(itemHandler, 1, 116, 26) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                LOGGER.debug("MixingBowlMenu: Slot 1 mayPlace called for item: {}, has TOBACCO_WEIGHT: {}", stack.getItem().getDescriptionId(), stack.has(ModDataComponents.TOBACCO_WEIGHT.get()));
                if (stack.has(ModDataComponents.TOBACCO_WEIGHT.get())) {
                    LOGGER.debug("MixingBowlMenu: Slot 1 item {} has weight {}", stack.getItem().getDescriptionId(), stack.get(ModDataComponents.TOBACCO_WEIGHT.get()));
                }
                return stack.getItem() instanceof TobaccoItem || stack.is(ModItems.MIXED_TOBACCO.get()) || stack.getItem() instanceof SelfMixedTobaccoItem;
            }
        });
        this.addSlot(new SlotItemHandler(itemHandler, 2, 80, 58) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public boolean mayPickup(Player playerIn) {
                return true;
            }
        });

        // Player Inventory
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(pPlayerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Player Hotbar
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(pPlayerInventory, col, 8 + col * 18, 142));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(pIndex);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            LOGGER.debug("MixingBowlMenu: quickMoveStack itemstack1: {}, has TOBACCO_WEIGHT: {}", itemstack1.getItem().getDescriptionId(), itemstack1.has(ModDataComponents.TOBACCO_WEIGHT.get()));
            if (itemstack1.has(ModDataComponents.TOBACCO_WEIGHT.get())) {
                LOGGER.debug("MixingBowlMenu: quickMoveStack itemstack1 weight: {}", itemstack1.get(ModDataComponents.TOBACCO_WEIGHT.get()));
            }

            if (pIndex < 3) { // Mixing Bowl slots
                if (!this.moveItemStackTo(itemstack1, 3, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemstack1, 0, 2, false)) { // To mixing bowl input slots
                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(pPlayer, itemstack1);
        }
        return itemstack;
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return true;
    }

    @Override
    public void removed(Player pPlayer) {
        if (!pPlayer.level().isClientSide()) {
            for (int i = 0; i < itemHandler.getSlots(); i++) {
                ItemStack stack = itemHandler.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    pPlayer.getInventory().placeItemBackInInventory(stack);
                    itemHandler.setStackInSlot(i, ItemStack.EMPTY);
                }
            }
        }
        super.removed(pPlayer);
    }

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }
}
