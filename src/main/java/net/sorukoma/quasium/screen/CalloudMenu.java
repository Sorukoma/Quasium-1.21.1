package net.sorukoma.quasium.screen;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.sorukoma.quasium.component.ModDataComponents;
import net.sorukoma.quasium.item.CharcoalItem;
import net.sorukoma.quasium.item.ModItems;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class CalloudMenu extends AbstractContainerMenu {
    private final Player player;
    private final int handIndex; // 0=main, 1=off
    private final ItemStackHandler calloudSlots;
    // Точная ссылка на открытый предмет — чтобы не писать в другой калауд,
    // если игрок переложит/подменит предмет в руке при открытом GUI.
    private final ItemStack openedStack;

    public static final int BASE_SLOTS = 4;
    public static final int STACKED_SLOTS = 4;
    public static final int TOTAL_CALLOUD_SLOTS = BASE_SLOTS + STACKED_SLOTS;
    public static final int CALLOUD_INV_END = TOTAL_CALLOUD_SLOTS;
    public static final int PLAYER_INV_START = CALLOUD_INV_END;
    public static final int PLAYER_INV_END = PLAYER_INV_START + 27;
    public static final int HOTBAR_START = PLAYER_INV_END;
    public static final int HOTBAR_END = HOTBAR_START + 9;

    // Server constructor
    public CalloudMenu(int containerId, Inventory playerInventory, int handIndex, ModDataComponents.CalloudData contents) {
        super(ModMenuTypes.CALLOUD_MENU.get(), containerId);
        this.player = playerInventory.player;
        this.handIndex = handIndex;
        this.openedStack = this.player.getItemInHand(handIndex == 0 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);

        this.calloudSlots = new ItemStackHandler(TOTAL_CALLOUD_SLOTS) {
            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                if (!(stack.getItem() instanceof CharcoalItem)) return false;
                if (slot >= BASE_SLOTS) {
                    int baseSlot = slot - BASE_SLOTS;
                    if (getStackInSlot(baseSlot).isEmpty()) return false;
                }
                int currentVolume = 0;
                for (int i = 0; i < TOTAL_CALLOUD_SLOTS; i++) {
                    ItemStack existing = getStackInSlot(i);
                    if (!existing.isEmpty() && existing.getItem() instanceof CharcoalItem ci) {
                        currentVolume += ci.getSize().getVolume();
                    }
                }
                CharcoalItem newItem = (CharcoalItem) stack.getItem();
                return currentVolume + newItem.getSize().getVolume() <= 12;
            }

            @Override
            protected void onContentsChanged(int slot) {
                if (!player.level().isClientSide()) {
                    updateCalloudItem();
                }
            }
        };

        // Populate base slots from CalloudData
        for (int i = 0; i < contents.charcoals().size() && i < BASE_SLOTS; i++) {
            ModDataComponents.CharcoalData cd = contents.charcoals().get(i);
            CharcoalItem item = getCharcoalItemForSize(cd.size());
            ItemStack stack = new ItemStack(item);
            stack.set(ModDataComponents.CHARCOAL_DATA.get(), cd);
            calloudSlots.setStackInSlot(i, stack);
        }
        // Populate stacked slots from CalloudData
        for (int i = 0; i < contents.stackedCharcoals().size() && i < STACKED_SLOTS; i++) {
            ModDataComponents.CharcoalData cd = contents.stackedCharcoals().get(i);
            CharcoalItem item = getCharcoalItemForSize(cd.size());
            ItemStack stack = new ItemStack(item);
            stack.set(ModDataComponents.CHARCOAL_DATA.get(), cd);
            calloudSlots.setStackInSlot(i + BASE_SLOTS, stack);
        }

        // Base slots
        for (int i = 0; i < BASE_SLOTS; i++) {
            this.addSlot(new SlotItemHandler(calloudSlots, i, 44 + i * 18, 36));
        }
        // Stacked slots (above base slots)
        for (int i = 0; i < BASE_SLOTS; i++) {
            this.addSlot(new SlotItemHandler(calloudSlots, i + BASE_SLOTS, 44 + i * 18, 18));
        }

        addPlayerInventory(playerInventory);
    }

    // Client constructor
    public CalloudMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        super(ModMenuTypes.CALLOUD_MENU.get(), containerId);
        this.player = playerInventory.player;
        this.handIndex = buf.readVarInt();
        this.openedStack = this.player.getItemInHand(handIndex == 0 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);

        // Read base charcoals from buf
        int baseCount = buf.readVarInt();
        List<ModDataComponents.CharcoalData> baseList = new ArrayList<>();
        for (int i = 0; i < baseCount; i++) {
            ModDataComponents.CharcoalSize size = ModDataComponents.CharcoalSize.valueOf(buf.readUtf());
            int remainingTicks = buf.readVarInt();
            float heatingProgress = buf.readFloat();
            baseList.add(new ModDataComponents.CharcoalData(size, remainingTicks, heatingProgress));
        }
        // Read stacked charcoals from buf
        int stackedCount = buf.readVarInt();
        List<ModDataComponents.CharcoalData> stackedList = new ArrayList<>();
        for (int i = 0; i < stackedCount; i++) {
            ModDataComponents.CharcoalSize size = ModDataComponents.CharcoalSize.valueOf(buf.readUtf());
            int remainingTicks = buf.readVarInt();
            float heatingProgress = buf.readFloat();
            stackedList.add(new ModDataComponents.CharcoalData(size, remainingTicks, heatingProgress));
        }
        ModDataComponents.CalloudData contents = new ModDataComponents.CalloudData(baseList, stackedList);

        this.calloudSlots = new ItemStackHandler(TOTAL_CALLOUD_SLOTS) {
            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                if (!(stack.getItem() instanceof CharcoalItem)) return false;
                if (slot >= BASE_SLOTS) {
                    int baseSlot = slot - BASE_SLOTS;
                    if (getStackInSlot(baseSlot).isEmpty()) return false;
                }
                int currentVolume = 0;
                for (int i = 0; i < TOTAL_CALLOUD_SLOTS; i++) {
                    ItemStack existing = getStackInSlot(i);
                    if (!existing.isEmpty() && existing.getItem() instanceof CharcoalItem ci) {
                        currentVolume += ci.getSize().getVolume();
                    }
                }
                CharcoalItem newItem = (CharcoalItem) stack.getItem();
                return currentVolume + newItem.getSize().getVolume() <= 12;
            }
        };

        // Populate base slots
        for (int i = 0; i < contents.charcoals().size() && i < BASE_SLOTS; i++) {
            ModDataComponents.CharcoalData cd = contents.charcoals().get(i);
            CharcoalItem item = getCharcoalItemForSize(cd.size());
            ItemStack stack = new ItemStack(item);
            stack.set(ModDataComponents.CHARCOAL_DATA.get(), cd);
            calloudSlots.setStackInSlot(i, stack);
        }
        // Populate stacked slots
        for (int i = 0; i < contents.stackedCharcoals().size() && i < STACKED_SLOTS; i++) {
            ModDataComponents.CharcoalData cd = contents.stackedCharcoals().get(i);
            CharcoalItem item = getCharcoalItemForSize(cd.size());
            ItemStack stack = new ItemStack(item);
            stack.set(ModDataComponents.CHARCOAL_DATA.get(), cd);
            calloudSlots.setStackInSlot(i + BASE_SLOTS, stack);
        }

        // Base slots
        for (int i = 0; i < BASE_SLOTS; i++) {
            this.addSlot(new SlotItemHandler(calloudSlots, i, 44 + i * 18, 36));
        }
        // Stacked slots (above base slots)
        for (int i = 0; i < BASE_SLOTS; i++) {
            this.addSlot(new SlotItemHandler(calloudSlots, i + BASE_SLOTS, 44 + i * 18, 18));
        }

        addPlayerInventory(playerInventory);
    }

    private void addPlayerInventory(Inventory inv) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inv, col, 8 + col * 18, 142));
        }
    }

    private void updateCalloudItem() {
        ItemStack calloudStack = player.getItemInHand(handIndex == 0 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
        // Защита от дюпа: пишем только если в руке всё ещё ровно тот предмет, что был открыт.
        if (calloudStack != openedStack || !calloudStack.is(ModItems.CALLOUD.get())) return;

        // Handle orphaned stacked slots: give back to player if base slot is empty
        boolean hadOrphans = false;
        for (int i = 0; i < BASE_SLOTS; i++) {
            ItemStack baseStack = calloudSlots.getStackInSlot(i);
            ItemStack stackedStack = calloudSlots.getStackInSlot(i + BASE_SLOTS);
            if (baseStack.isEmpty() && !stackedStack.isEmpty()) {
                ItemStack orphan = stackedStack.copy();
                player.getInventory().add(orphan);
                if (!orphan.isEmpty()) {
                    player.drop(orphan, false);
                }
                calloudSlots.setStackInSlot(i + BASE_SLOTS, ItemStack.EMPTY);
                hadOrphans = true;
            }
        }
        if (hadOrphans) return;

        calloudStack.set(ModDataComponents.CALLOUD_CONTENTS.get(), contents());
    }

    public ModDataComponents.CalloudData contents() {
        List<ModDataComponents.CharcoalData> charcoals = new ArrayList<>();
        List<ModDataComponents.CharcoalData> stackedCharcoals = new ArrayList<>();
        for (int i = 0; i < BASE_SLOTS; i++) {
            ItemStack slotStack = calloudSlots.getStackInSlot(i);
            if (!slotStack.isEmpty() && slotStack.getItem() instanceof CharcoalItem ci) {
                ModDataComponents.CharcoalData data = slotStack.getOrDefault(ModDataComponents.CHARCOAL_DATA.get(),
                        new ModDataComponents.CharcoalData(ci.getSize(), ci.getSize().getMaxTicks(), 0f));
                charcoals.add(data);
            }
            ItemStack stackedStack = calloudSlots.getStackInSlot(i + BASE_SLOTS);
            if (!slotStack.isEmpty() && !stackedStack.isEmpty() && stackedStack.getItem() instanceof CharcoalItem ci) {
                ModDataComponents.CharcoalData data = stackedStack.getOrDefault(ModDataComponents.CHARCOAL_DATA.get(),
                        new ModDataComponents.CharcoalData(ci.getSize(), ci.getSize().getMaxTicks(), 0f));
                stackedCharcoals.add(data);
            }
        }
        return new ModDataComponents.CalloudData(charcoals, stackedCharcoals);
    }

    private static CharcoalItem getCharcoalItemForSize(ModDataComponents.CharcoalSize size) {
        return switch (size) {
            case MM_22 -> (CharcoalItem) ModItems.CHARCOAL_22.get();
            case MM_25 -> (CharcoalItem) ModItems.CHARCOAL_25.get();
            case MM_27 -> (CharcoalItem) ModItems.CHARCOAL_27.get();
        };
    }

    @Override
    public boolean stillValid(Player player) {
        // GUI действителен, только пока открытый калауд остаётся в той же руке.
        ItemStack inHand = player.getItemInHand(handIndex == 0 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
        return inHand == openedStack && inHand.is(ModItems.CALLOUD.get());
    }

    @Override
    public @NotNull ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            itemstack = stackInSlot.copy();

            if (index < CALLOUD_INV_END) {
                if (!this.moveItemStackTo(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (stackInSlot.getItem() instanceof CharcoalItem) {
                    if (!this.moveItemStackTo(stackInSlot, 0, CALLOUD_INV_END, false)) {
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

    @Override
    public void removed(Player player) {
        if (!player.level().isClientSide()) {
            updateCalloudItem();
        }
        super.removed(player);
    }

    public ItemStackHandler getCalloudSlots() { return calloudSlots; }
    public int getHandIndex() { return handIndex; }
}
