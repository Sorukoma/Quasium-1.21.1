package net.sorukoma.quasium.screen;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.sorukoma.quasium.component.ModDataComponents;
import net.sorukoma.quasium.item.ModItems;
import net.sorukoma.quasium.item.SelfMixedTobaccoItem;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class HookahBowlMenu extends AbstractContainerMenu {
    private final InteractionHand hand;
    private final ItemStackHandler bowlSlots;
    private final boolean isServerSide;
    private final Player player;
    private final boolean isHeated;
    // Точная ссылка на открытую чашу — чтобы запись шла именно в неё, а не в любую
    // другую чашу из инвентаря (это давало дюп/перезапись).
    private final ItemStack openedStack;

    private static final int BOWL_SLOTS_START = 0;
    private static final int BOWL_SLOTS_COUNT = 20;
    private static final int PLAYER_INV_START = 20;
    private static final int PLAYER_INV_END = 47;
    private static final int HOTBAR_START = 47;
    private static final int HOTBAR_END = 56;

    private static final int GRID_COLS = 5;
    private static final int GRID_ROWS = 4;
    private static final int GRID_X_START = 43;
    private static final int GRID_Y_START = 18;
    private static final int SLOT_SIZE = 18;

    public HookahBowlMenu(int containerId, Inventory playerInventory, InteractionHand hand, ModDataComponents.HookahBowlContents contents) {
        super(ModMenuTypes.HOOKAH_BOWL_MENU.get(), containerId);
        this.hand = hand;
        this.isServerSide = true;
        this.player = playerInventory.player;
        this.isHeated = contents.heated();
        this.openedStack = this.player.getItemInHand(hand);

        ItemStackHandler handler = new ItemStackHandler(BOWL_SLOTS_COUNT) {
            @Override
            protected void onContentsChanged(int slot) {
                updateBowlItem();
            }
        };

        populateHandlerFromContents(handler, contents);
        this.bowlSlots = handler;

        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                int slotIndex = row * GRID_COLS + col;
                int x = GRID_X_START + col * SLOT_SIZE;
                int y = GRID_Y_START + row * SLOT_SIZE;
                this.addSlot(new BowlSlot(bowlSlots, slotIndex, x, y));
            }
        }

        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }

    public HookahBowlMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        super(ModMenuTypes.HOOKAH_BOWL_MENU.get(), containerId);
        this.hand = InteractionHand.values()[buf.readVarInt()];
        this.isServerSide = false;
        this.player = playerInventory.player;
        this.openedStack = this.player.getItemInHand(hand);

        ModDataComponents.HookahBowlContents contents = ModDataComponents.HookahBowlContents.STREAM_CODEC.decode(buf);
        this.isHeated = contents.heated();

        ItemStackHandler handler = new ItemStackHandler(BOWL_SLOTS_COUNT);
        populateHandlerFromContents(handler, contents);
        this.bowlSlots = handler;

        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                int slotIndex = row * GRID_COLS + col;
                int x = GRID_X_START + col * SLOT_SIZE;
                int y = GRID_Y_START + row * SLOT_SIZE;
                this.addSlot(new BowlSlot(bowlSlots, slotIndex, x, y));
            }
        }

        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }

    private class BowlSlot extends SlotItemHandler {
        BowlSlot(ItemStackHandler handler, int index, int xPosition, int yPosition) {
            super(handler, index, xPosition, yPosition);
        }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            if (isHeated) return false;
            return (stack.is(ModItems.ONE_GRAM_TOBACCO.get()) && stack.has(ModDataComponents.TOBACCO_FLAVOR.get()))
                    || (stack.is(ModItems.ONE_GRAM_SELF_MIXED_TOBACCO.get()) && stack.has(ModDataComponents.TOBACCO_BASE.get()))
                    || stack.getItem() instanceof SelfMixedTobaccoItem;
        }

        @Override
        public boolean mayPickup(@NotNull Player player) {
            // Нагретая чаша: забор несгоревшего табака разрешён, но предмет уничтожается
            // (см. tryRemove ниже). Сгоревший табак и обычные слоты — без ограничений.
            return true;
        }

        @Override
        public java.util.Optional<ItemStack> tryRemove(int amount, int maxAmount, Player player) {
            ItemStack stack = getItem();
            if (isHeated && !stack.isEmpty() && !stack.is(ModItems.BURNED_TOBACCO.get())) {
                // Несгоревший табак из нагретой чаши: очищаем слот и не отдаём предмет игроку.
                // Установка EMPTY триггерит onContentsChanged -> updateBowlItem() — состояние
                // чаши корректно пересохраняется в DataComponent.
                setByPlayer(ItemStack.EMPTY);
                return java.util.Optional.of(ItemStack.EMPTY);
            }
            return super.tryRemove(amount, maxAmount, player);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public int getMaxStackSize(@NotNull ItemStack stack) {
            return 1;
        }
    }

    private void populateHandlerFromContents(ItemStackHandler handler, ModDataComponents.HookahBowlContents contents) {
        for (Map.Entry<Integer, Integer> entry : contents.slotAssignments().entrySet()) {
            int slotIndex = entry.getKey();
            int groupIdx = entry.getValue();
            if (slotIndex < 0 || slotIndex >= BOWL_SLOTS_COUNT || groupIdx >= contents.groups().size()) continue;

            ModDataComponents.BowlTobaccoGroup group = contents.groups().get(groupIdx);
            ItemStack slotItem;
            if (group.isRegular()) {
                slotItem = new ItemStack(ModItems.ONE_GRAM_TOBACCO.get());
                slotItem.set(ModDataComponents.TOBACCO_FLAVOR.get(), group.flavor());
            } else {
                slotItem = new ItemStack(ModItems.ONE_GRAM_SELF_MIXED_TOBACCO.get());
                slotItem.set(ModDataComponents.TOBACCO_BASE.get(), group.base());
                slotItem.set(ModDataComponents.TOBACCO_DAMAGE.get(), group.strength());
                slotItem.set(ModDataComponents.SELF_MIXED_EFFECTS.get(), group.effects());
                slotItem.set(ModDataComponents.ESSENCE_TYPE.get(), group.flavor());
                slotItem.set(ModDataComponents.ESSENCE_AMOUNT.get(), group.essenceAmount());
                slotItem.set(ModDataComponents.MOLASSES_AMOUNT.get(), group.molassesAmount());
                if (group.toxic()) slotItem.set(ModDataComponents.TOXIC.get(), true);
            }
            if (isHeated) {
                slotItem.set(ModDataComponents.HEATED_MARKER.get(), true);
            }
            handler.setStackInSlot(slotIndex, slotItem);
        }

        for (int burnedSlot : contents.burnedSlots()) {
            if (burnedSlot < 0 || burnedSlot >= BOWL_SLOTS_COUNT) continue;
            handler.setStackInSlot(burnedSlot, new ItemStack(ModItems.BURNED_TOBACCO.get()));
        }
    }

    private void updateBowlItem() {
        if (!isServerSide || bowlSlots == null) return;
        ItemStack bowlItem = findBowlItem(player);
        if (bowlItem.isEmpty()) return;

        List<ModDataComponents.BowlTobaccoGroup> groups = new ArrayList<>();
        Map<Integer, Integer> slotAssignments = new TreeMap<>();
        Set<Integer> burnedSlots = new TreeSet<>();

        for (int i = 0; i < BOWL_SLOTS_COUNT; i++) {
            ItemStack slotStack = bowlSlots.getStackInSlot(i);
            if (slotStack.isEmpty()) continue;

            if (slotStack.is(ModItems.BURNED_TOBACCO.get())) {
                burnedSlots.add(i);
                continue;
            }

            ModDataComponents.BowlTobaccoGroup groupData;
            if (slotStack.is(ModItems.ONE_GRAM_TOBACCO.get()) && slotStack.has(ModDataComponents.TOBACCO_FLAVOR.get())) {
                String flavor = slotStack.get(ModDataComponents.TOBACCO_FLAVOR.get());
                groupData = new ModDataComponents.BowlTobaccoGroup("regular", flavor, "regular", 0.0f, List.of());
            } else if (slotStack.is(ModItems.ONE_GRAM_SELF_MIXED_TOBACCO.get()) && slotStack.has(ModDataComponents.TOBACCO_BASE.get())) {
                String base = slotStack.get(ModDataComponents.TOBACCO_BASE.get());
                float strength = slotStack.getOrDefault(ModDataComponents.TOBACCO_DAMAGE.get(), 0f);
                List<MobEffectInstance> effects = slotStack.getOrDefault(ModDataComponents.SELF_MIXED_EFFECTS.get(), List.of());
                int essenceAmount = slotStack.getOrDefault(ModDataComponents.ESSENCE_AMOUNT.get(), 0);
                int molassesAmount = slotStack.getOrDefault(ModDataComponents.MOLASSES_AMOUNT.get(), 0);
                String flavor = slotStack.getOrDefault(ModDataComponents.ESSENCE_TYPE.get(), "mixed");
                boolean toxic = slotStack.getOrDefault(ModDataComponents.TOXIC.get(), false);
                groupData = new ModDataComponents.BowlTobaccoGroup("self_mixed", flavor, base, strength, effects,
                        essenceAmount, molassesAmount, toxic);
            } else {
                continue;
            }

            int groupIdx = -1;
            for (int g = 0; g < groups.size(); g++) {
                if (groups.get(g).equals(groupData)) {
                    groupIdx = g;
                    break;
                }
            }
            if (groupIdx < 0) {
                groups.add(groupData);
                groupIdx = groups.size() - 1;
            }
            slotAssignments.put(i, groupIdx);
        }

        boolean newHeated = isHeated;
        if (slotAssignments.isEmpty() && burnedSlots.isEmpty()) {
            newHeated = false;
        }

        ModDataComponents.HookahBowlContents newContents = new ModDataComponents.HookahBowlContents(groups, slotAssignments, burnedSlots, newHeated);
        bowlItem.set(ModDataComponents.HOOKAH_BOWL_CONTENTS.get(), newContents);
    }

    private ItemStack findBowlItem(Player player) {
        // Возвращаем именно ту чашу, что была открыта, и только если она ещё в руке.
        ItemStack inHand = player.getItemInHand(hand);
        if (inHand == openedStack && !inHand.isEmpty() && inHand.is(ModItems.HOOKAH_BOWL.get())) {
            return inHand;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (isServerSide && slotId >= BOWL_SLOTS_START && slotId < BOWL_SLOTS_START + BOWL_SLOTS_COUNT
                && !isHeated && getCarried().getItem() instanceof SelfMixedTobaccoItem) {
            ItemStack source = getCarried();
            int weight = source.getOrDefault(ModDataComponents.TOBACCO_WEIGHT.get(), 0);
            Slot target = this.slots.get(slotId);
            if (weight > 0 && !target.hasItem() && target.mayPlace(source)) {
                target.setByPlayer(SelfMixedTobaccoItem.createOneGram(source));
                source.set(ModDataComponents.TOBACCO_WEIGHT.get(), weight - 1);
                if (weight <= 1) setCarried(ItemStack.EMPTY);
                updateBowlItem();
                return;
            }
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        result = stack.copy();

        if (index >= BOWL_SLOTS_START && index < BOWL_SLOTS_START + BOWL_SLOTS_COUNT) {
            if (isHeated && !stack.isEmpty() && !stack.is(ModItems.BURNED_TOBACCO.get())) {
                // Несгоревший табак из нагретой чаши: уничтожаем и не переносим в инвентарь.
                slot.setByPlayer(ItemStack.EMPTY);
                slot.setChanged();
                return ItemStack.EMPTY;
            }
            if (!this.moveItemStackTo(stack, PLAYER_INV_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (index >= PLAYER_INV_START && index < HOTBAR_END) {
            if (isHeated) return ItemStack.EMPTY;
            if (stack.getItem() instanceof SelfMixedTobaccoItem) {
                int targetIndex = -1;
                for (int i = BOWL_SLOTS_START; i < BOWL_SLOTS_START + BOWL_SLOTS_COUNT; i++) {
                    if (!this.slots.get(i).hasItem()) {
                        targetIndex = i;
                        break;
                    }
                }
                int weight = stack.getOrDefault(ModDataComponents.TOBACCO_WEIGHT.get(), 0);
                if (targetIndex < 0 || weight <= 0) return ItemStack.EMPTY;
                this.slots.get(targetIndex).setByPlayer(SelfMixedTobaccoItem.createOneGram(stack));
                stack.set(ModDataComponents.TOBACCO_WEIGHT.get(), weight - 1);
                if (weight <= 1) slot.setByPlayer(ItemStack.EMPTY);
                else slot.setChanged();
                updateBowlItem();
                return result;
            }
            if (stack.is(ModItems.ONE_GRAM_TOBACCO.get()) || stack.is(ModItems.ONE_GRAM_SELF_MIXED_TOBACCO.get())) {
                if (!this.moveItemStackTo(stack, BOWL_SLOTS_START, BOWL_SLOTS_START + BOWL_SLOTS_COUNT, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
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
        }

        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();

        if (stack.getCount() == result.getCount()) return ItemStack.EMPTY;

        slot.onTake(player, stack);
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        // GUI действителен, только пока открытая чаша остаётся в той же руке.
        ItemStack inHand = player.getItemInHand(hand);
        return inHand == openedStack && !inHand.isEmpty() && inHand.is(ModItems.HOOKAH_BOWL.get());
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (isServerSide) {
            updateBowlItem();
        }
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 8 + l * 18, 140 + i * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 198));
        }
    }

    public ItemStackHandler getBowlSlots() {
        return bowlSlots;
    }

    public InteractionHand getHand() {
        return hand;
    }

    public boolean isHeated() {
        return isHeated;
    }
}
