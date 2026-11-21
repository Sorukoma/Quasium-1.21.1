package net.sorukoma.quasium.draft;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.ForwardingItemHandler;
import net.sorukoma.quasium.Config;
import net.sorukoma.quasium.block.entity.NoteJarBlockEntity;
import net.sorukoma.quasium.component.ModDataComponents;
import net.sorukoma.quasium.item.EssenceBottleItem;
import net.sorukoma.quasium.magic.NoteType;
import net.sorukoma.quasium.screen.EssencePumpMenu;
import org.jetbrains.annotations.Nullable;

public class EssencePumpBlockEntity extends BlockEntity implements DraftNode, MenuProvider {
    public static final int INPUT = 0;
    public static final int OUTPUT = 1;

    private final ItemStackHandler items = createItems(this::setChanged);
    private final IItemHandler automation = new ForwardingItemHandler(items) {
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot != OUTPUT || stillFilling(getStackInSlot(OUTPUT))) return ItemStack.EMPTY;
            return super.extractItem(slot, amount, simulate);
        }
    };
    private float strokes;

    public EssencePumpBlockEntity(BlockPos pos, BlockState state) {
        super(MachineBlocks.ESSENCE_PUMP_BE.get(), pos, state);
    }

    public static ItemStackHandler createItems(Runnable changed) {
        return new ItemStackHandler(2) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return slot == INPUT && (stack.is(Items.GLASS_BOTTLE) || NoteType.isNote(EssenceBottleItem.typeOf(stack)));
            }

            @Override
            protected void onContentsChanged(int slot) {
                changed.run();
            }
        };
    }

    public ItemStackHandler getItems() {
        return items;
    }

    public IItemHandler automation() {
        return automation;
    }

    private static int bottleCapacity() {
        return Config.NOTE_BOTTLE_CAPACITY.getAsInt();
    }

    @Nullable
    private NoteJarBlockEntity jar() {
        if (level == null) return null;
        for (Direction direction : Direction.values()) {
            if (level.getBlockEntity(worldPosition.relative(direction)) instanceof NoteJarBlockEntity jar) return jar;
        }
        return null;
    }

    @Override
    public int draftDemand(Direction side) {
        NoteJarBlockEntity jar = jar();
        return jar != null && move(jar, 1, true) > 0 ? Config.PUMP_DEMAND.getAsInt() : 0;
    }

    @Override
    public void draftSupplied(Direction side, float efficiency) {
        NoteJarBlockEntity jar = jar();
        if (jar == null) return;
        strokes += Config.PUMP_ML_PER_SECOND.getAsInt() * efficiency / 20f;
        int ml = (int) strokes;
        if (ml <= 0) return;
        strokes -= ml;
        move(jar, ml, false);
    }

    private int move(NoteJarBlockEntity jar, int ml, boolean simulate) {
        int filled = fill(jar, ml, simulate);
        return filled > 0 ? filled : drain(jar, ml, simulate);
    }

    private int fill(NoteJarBlockEntity jar, int ml, boolean simulate) {
        NoteType note = jar.note();
        if (note == null) return 0;
        ItemStack bottle = items.getStackInSlot(OUTPUT);
        boolean fresh = bottle.isEmpty();
        boolean fits = fresh ? items.getStackInSlot(INPUT).is(Items.GLASS_BOTTLE) : EssenceBottleItem.typeOf(bottle).equals(note.id());
        if (!fits) return 0;
        int held = EssenceBottleItem.amountOf(bottle);
        int amount = Math.min(ml, Math.min(bottleCapacity() - held, jar.amount()));
        if (simulate || amount <= 0) return Math.max(0, amount);
        jar.take(amount);
        if (fresh) items.extractItem(INPUT, 1, false);
        items.setStackInSlot(OUTPUT, EssenceBottleItem.of(note.id(), held + amount));
        return amount;
    }

    private int drain(NoteJarBlockEntity jar, int ml, boolean simulate) {
        ItemStack bottle = items.getStackInSlot(INPUT);
        ItemStack empties = items.getStackInSlot(OUTPUT);
        NoteType note = NoteType.byId(EssenceBottleItem.typeOf(bottle));
        boolean room = empties.isEmpty() || (empties.is(Items.GLASS_BOTTLE) && empties.getCount() < empties.getMaxStackSize());
        if (note == null || !room) return 0;
        int left = EssenceBottleItem.amountOf(bottle);
        int poured = jar.acceptNote(note, Math.min(ml, left), simulate);
        if (simulate || poured <= 0) return poured;
        if (poured < left) {
            ItemStack rest = bottle.copy();
            rest.set(ModDataComponents.ESSENCE_AMOUNT.get(), left - poured);
            items.setStackInSlot(INPUT, rest);
        } else {
            items.setStackInSlot(INPUT, ItemStack.EMPTY);
            items.setStackInSlot(OUTPUT, new ItemStack(Items.GLASS_BOTTLE, empties.getCount() + 1));
        }
        return poured;
    }

    private boolean stillFilling(ItemStack bottle) {
        NoteType note = NoteType.byId(EssenceBottleItem.typeOf(bottle));
        if (note == null || EssenceBottleItem.amountOf(bottle) >= bottleCapacity()) return false;
        NoteJarBlockEntity jar = jar();
        return jar != null && (jar.note() == null || jar.note() == note);
    }

    public int progress() {
        ItemStack input = items.getStackInSlot(INPUT);
        int ml = NoteType.isNote(EssenceBottleItem.typeOf(input))
                ? bottleCapacity() - EssenceBottleItem.amountOf(input)
                : EssenceBottleItem.amountOf(items.getStackInSlot(OUTPUT));
        return Math.clamp(ml * 100L / bottleCapacity(), 0, 100);
    }

    public void dropContents() {
        for (int slot = 0; slot < items.getSlots(); slot++) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), items.getStackInSlot(slot));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", items.serializeNBT(registries));
        tag.putFloat("Strokes", strokes);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("inventory"));
        strokes = tag.getFloat("Strokes");
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new EssencePumpMenu(containerId, inventory, this);
    }
}
