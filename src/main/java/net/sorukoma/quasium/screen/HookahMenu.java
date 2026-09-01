package net.sorukoma.quasium.screen;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.sorukoma.quasium.block.entity.HookahBlockEntity;
import net.sorukoma.quasium.item.CalloudItem;
import net.sorukoma.quasium.item.ModItems;
import org.jetbrains.annotations.NotNull;

public class HookahMenu extends AbstractContainerMenu {
    public final HookahBlockEntity blockEntity;
    private final Level level;

    private static final int COAL_SLOT = 0;
    private static final int CONTENT_SLOT = 1;
    private static final int DIFFUSER_SLOT = 2;
    private static final int COOLER_SLOT = 3;
    private static final int ICE_CHAMBER_SLOT = 4;
    private static final int CONTAINER_SLOTS_COUNT = 5;
    private static final int PLAYER_INV_START = 5;
    private static final int PLAYER_INV_END = 32;
    private static final int HOTBAR_START = 32;
    private static final int HOTBAR_END = 41;

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

        // Слот содержимого — HookahBowlItem (slot 1)
        this.addSlot(new SlotItemHandler(blockEntity.getItemHandler(), 1, 80, 53) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return stack.is(ModItems.HOOKAH_BOWL.get());
            }

            @Override
            public int getMaxStackSize() { return 1; }

            @Override
            public int getMaxStackSize(@NotNull ItemStack stack) { return 1; }
        });

        // Слот диффузора (slot 2)
        this.addSlot(new SlotItemHandler(blockEntity.getItemHandler(), 2, 116, 17) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return stack.is(ModItems.HOOKAH_DIFFUSER.get());
            }
            @Override public int getMaxStackSize() { return 1; }
            @Override public int getMaxStackSize(@NotNull ItemStack stack) { return 1; }
        });

        // Слот охладителя (slot 3)
        this.addSlot(new SlotItemHandler(blockEntity.getItemHandler(), 3, 116, 35) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return stack.is(ModItems.HOOKAH_COOLER.get());
            }
            @Override public int getMaxStackSize() { return 1; }
            @Override public int getMaxStackSize(@NotNull ItemStack stack) { return 1; }
        });

        // Слот ледяной колбы (slot 4)
        this.addSlot(new SlotItemHandler(blockEntity.getItemHandler(), 4, 116, 53) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return stack.is(ModItems.HOOKAH_ICE_CHAMBER.get());
            }
            @Override public int getMaxStackSize() { return 1; }
            @Override public int getMaxStackSize(@NotNull ItemStack stack) { return 1; }
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

        if (index < CONTAINER_SLOTS_COUNT) {
            if (!this.moveItemStackTo(stack, PLAYER_INV_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (index >= PLAYER_INV_START && index < HOTBAR_END) {
            if (stack.getItem() instanceof CalloudItem) {
                if (!this.moveItemStackTo(stack, COAL_SLOT, COAL_SLOT + 1, false)) return ItemStack.EMPTY;
            } else if (stack.is(ModItems.HOOKAH_BOWL.get())) {
                if (!this.moveItemStackTo(stack, CONTENT_SLOT, CONTENT_SLOT + 1, false)) return ItemStack.EMPTY;
            } else if (stack.is(ModItems.HOOKAH_DIFFUSER.get())) {
                if (!this.moveItemStackTo(stack, DIFFUSER_SLOT, DIFFUSER_SLOT + 1, false)) return ItemStack.EMPTY;
            } else if (stack.is(ModItems.HOOKAH_COOLER.get())) {
                if (!this.moveItemStackTo(stack, COOLER_SLOT, COOLER_SLOT + 1, false)) return ItemStack.EMPTY;
            } else if (stack.is(ModItems.HOOKAH_ICE_CHAMBER.get())) {
                if (!this.moveItemStackTo(stack, ICE_CHAMBER_SLOT, ICE_CHAMBER_SLOT + 1, false)) return ItemStack.EMPTY;
            } else if (index >= PLAYER_INV_START && index < PLAYER_INV_END) {
                if (!this.moveItemStackTo(stack, HOTBAR_START, HOTBAR_END, false)) return ItemStack.EMPTY;
            } else if (index >= HOTBAR_START && index < HOTBAR_END) {
                if (!this.moveItemStackTo(stack, PLAYER_INV_START, PLAYER_INV_END, false)) return ItemStack.EMPTY;
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
        // Действительно для базового и модульного кальяна (оба содержат HookahBlockEntity).
        return blockEntity != null
                && !blockEntity.isRemoved()
                && player.level().getBlockEntity(blockEntity.getBlockPos()) == blockEntity
                && player.distanceToSqr(blockEntity.getBlockPos().getX() + 0.5,
                        blockEntity.getBlockPos().getY() + 0.5,
                        blockEntity.getBlockPos().getZ() + 0.5) <= 64.0;
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