package net.sorukoma.quasium.screen;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.sorukoma.quasium.golem.program.MarkedPlace;
import net.sorukoma.quasium.golem.program.Program;
import net.sorukoma.quasium.golem.program.ProgramBlocks;
import net.sorukoma.quasium.golem.program.ProgramEdit;
import net.sorukoma.quasium.golem.program.ProgramTableBlockEntity;
import org.jetbrains.annotations.Nullable;

public class ProgramTableMenu extends AbstractContainerMenu {
    public static final int CARD_SLOT = 0;
    public static final int CARD_X = 175;
    public static final int CARD_Y = 212;
    public static final int INVENTORY_Y = 154;
    private static final int MASK_PARTS = 4;
    private static final int MASK_BITS = 16;
    private static final int GOLEMS = MASK_PARTS;
    private static final int DATA_SLOTS = GOLEMS + 1;

    @Nullable
    private final ProgramTableBlockEntity table;
    private final BlockPos pos;
    private final Container card = new SimpleContainer(1);
    private final ContainerData data;

    public ProgramTableMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, buf.readBlockPos(), null);
    }

    public ProgramTableMenu(int containerId, Inventory inventory, ProgramTableBlockEntity table) {
        this(containerId, inventory, table.getBlockPos(), table);
    }

    private ProgramTableMenu(int containerId, Inventory inventory, BlockPos pos, @Nullable ProgramTableBlockEntity table) {
        super(ModMenuTypes.PROGRAM_TABLE_MENU.get(), containerId);
        this.table = table;
        this.pos = pos;
        data = table == null ? new SimpleContainerData(DATA_SLOTS) : tableData(table);
        addSlot(new Slot(card, 0, CARD_X, CARD_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ProgramBlocks.PROGRAM_CARD.get());
            }
        });
        for (int slot = 0; slot < 27; slot++) addSlot(new Slot(inventory, slot + 9, 8 + slot % 9 * 18, INVENTORY_Y + slot / 9 * 18));
        for (int slot = 0; slot < 9; slot++) addSlot(new Slot(inventory, slot, 8 + slot * 18, INVENTORY_Y + 58));
        addDataSlots(data);
    }

    private static ContainerData tableData(ProgramTableBlockEntity table) {
        return new ContainerData() {
            @Override
            public int get(int index) {
                if (index == GOLEMS) return table.golems().size();
                return (int) (table.runningMask() >>> index * MASK_BITS & 0xFFFF);
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return DATA_SLOTS;
            }
        };
    }

    public BlockPos pos() {
        return pos;
    }

    @Nullable
    public ProgramTableBlockEntity table(Level level) {
        return level.getBlockEntity(pos) instanceof ProgramTableBlockEntity seen ? seen : null;
    }

    public Program program(Level level) {
        ProgramTableBlockEntity seen = table(level);
        return seen == null ? Program.EMPTY : seen.program();
    }

    public boolean far(Level level, MarkedPlace place) {
        ProgramTableBlockEntity seen = table(level);
        return seen != null && !seen.reaches(place);
    }

    public long runningMask() {
        long mask = 0;
        for (int part = 0; part < MASK_PARTS; part++) mask |= (long) (data.get(part) & 0xFFFF) << part * MASK_BITS;
        return mask;
    }

    public int golems() {
        return data.get(GOLEMS);
    }

    public ItemStack card() {
        return card.getItem(0);
    }

    public boolean edit(Player player, ProgramEdit edit) {
        if (table == null) return false;
        return switch (edit.op()) {
            case SAVE_CARD -> saveCard();
            case LOAD_CARD -> loadCard();
            default -> table.apply(edit, player, getCarried());
        };
    }

    private boolean saveCard() {
        ItemStack stack = card();
        if (!stack.is(ProgramBlocks.PROGRAM_CARD.get()) || table.program().isEmpty()) return false;
        stack.set(ProgramBlocks.PROGRAM.get(), table.program());
        card.setChanged();
        return true;
    }

    private boolean loadCard() {
        Program stored = card().get(ProgramBlocks.PROGRAM.get());
        if (stored == null || stored.isEmpty() || !ProgramTableBlockEntity.fits(stored)) return false;
        table.setProgram(stored);
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack moved = stack.copy();
        boolean done = index == CARD_SLOT ? moveItemStackTo(stack, CARD_SLOT + 1, slots.size(), true)
                : moveItemStackTo(stack, CARD_SLOT, CARD_SLOT + 1, false);
        if (!done) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return moved;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (table != null) clearContainer(player, card);
    }

    @Override
    public boolean stillValid(Player player) {
        return table != null && !table.isRemoved() && table.mayEdit(player) && player.canInteractWithBlock(table.getBlockPos(), 4.0);
    }
}
