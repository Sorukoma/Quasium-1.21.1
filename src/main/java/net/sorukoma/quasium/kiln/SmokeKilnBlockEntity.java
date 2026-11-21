package net.sorukoma.quasium.kiln;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.RangedWrapper;
import net.sorukoma.quasium.Config;
import net.sorukoma.quasium.component.ModDataComponents;
import net.sorukoma.quasium.item.EssenceBottleItem;
import net.sorukoma.quasium.magic.NoteType;
import net.sorukoma.quasium.screen.SmokeKilnMenu;
import net.sorukoma.quasium.sound.ModSoundEvents;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SmokeKilnBlockEntity extends BlockEntity implements MenuProvider {
    public static final int TOOL_SLOTS = 4;
    public static final int ESSENCE_SLOT = TOOL_SLOTS;
    private static final int LOOP_TICKS = 30;

    private int progress;

    private final ItemStackHandler items = new ItemStackHandler(TOOL_SLOTS + 1) {
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return slot == ESSENCE_SLOT ? noteIn(stack) != null : NoteInfusion.infusable(stack);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private final IItemHandler automation = new RangedWrapper(items, 0, TOOL_SLOTS + 1) {
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return extractable(slot) ? super.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }
    };

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return progress * 100 / ticksPerItem();
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return 1;
        }
    };

    public SmokeKilnBlockEntity(BlockPos pos, BlockState state) {
        super(KilnBlocks.SMOKE_KILN_BE.get(), pos, state);
    }

    public ItemStackHandler getItems() {
        return items;
    }

    public IItemHandler automation() {
        return automation;
    }

    @Nullable
    private static NoteType noteIn(ItemStack bottle) {
        return EssenceBottleItem.amountOf(bottle) > 0 ? NoteType.byId(EssenceBottleItem.typeOf(bottle)) : null;
    }

    private static int ticksPerItem() {
        return Config.KILN_SECONDS_PER_ITEM.getAsInt() * 20;
    }

    @Nullable
    private NoteType workingNote() {
        ItemStack bottle = items.getStackInSlot(ESSENCE_SLOT);
        return EssenceBottleItem.amountOf(bottle) >= Config.KILN_ML_PER_ITEM.getAsInt() ? noteIn(bottle) : null;
    }

    private int nextSlot(NoteType note) {
        for (int slot = 0; slot < TOOL_SLOTS; slot++) {
            ItemStack tool = items.getStackInSlot(slot);
            if (NoteInfusion.infusable(tool) && NoteInfusion.of(tool) != note) return slot;
        }
        return -1;
    }

    private boolean extractable(int slot) {
        NoteType note = workingNote();
        if (slot == ESSENCE_SLOT) return note == null;
        NoteType infusion = NoteInfusion.of(items.getStackInSlot(slot));
        return infusion != null && (note == null || infusion == note);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SmokeKilnBlockEntity kiln) {
        NoteType note = kiln.workingNote();
        int slot = note == null ? -1 : kiln.nextSlot(note);
        boolean working = slot >= 0;
        if (working) {
            if (++kiln.progress >= ticksPerItem()) kiln.infuse(slot, note);
            if (level.getGameTime() % LOOP_TICKS == 0) {
                level.playSound(null, pos, ModSoundEvents.KILN_LOOP.get(), SoundSource.BLOCKS, 0.6f,
                        0.9f + level.random.nextFloat() * 0.2f);
            }
            kiln.setChanged();
        } else if (kiln.progress > 0) {
            kiln.progress = 0;
            kiln.setChanged();
        }
        if (state.getValue(SmokeKilnBlock.LIT) != working) {
            level.setBlock(pos, state.setValue(SmokeKilnBlock.LIT, working), Block.UPDATE_CLIENTS);
        }
    }

    private void infuse(int slot, NoteType note) {
        items.setStackInSlot(slot, NoteInfusion.infused(items.getStackInSlot(slot), note));
        ItemStack bottle = items.getStackInSlot(ESSENCE_SLOT).copy();
        int left = EssenceBottleItem.amountOf(bottle) - Config.KILN_ML_PER_ITEM.getAsInt();
        bottle.set(ModDataComponents.ESSENCE_AMOUNT.get(), left);
        items.setStackInSlot(ESSENCE_SLOT, left > 0 ? bottle : new ItemStack(Items.GLASS_BOTTLE));
        progress = 0;
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
        tag.putInt("Progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("inventory"));
        progress = tag.getInt("Progress");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.quasium.smoke_kiln");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new SmokeKilnMenu(containerId, inventory, this, data);
    }
}
