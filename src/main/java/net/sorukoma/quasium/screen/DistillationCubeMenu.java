package net.sorukoma.quasium.screen;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.sorukoma.quasium.Config;
import net.sorukoma.quasium.advancements.triggers.SmokecraftTrigger;
import net.sorukoma.quasium.block.entity.DistillationCubeBlockEntity;
import net.sorukoma.quasium.compat.SableCompat;
import net.sorukoma.quasium.component.ModDataComponents;
import net.sorukoma.quasium.item.EssenceBottleItem;
import net.sorukoma.quasium.magic.NoteProfiles;
import net.sorukoma.quasium.magic.NoteType;
import net.sorukoma.quasium.sound.ModSoundEvents;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DistillationCubeMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOTS = 2;
    private static final int PLAYER_SLOTS_END = MACHINE_SLOTS + 36;

    @Nullable
    private final DistillationCubeBlockEntity blockEntity;
    private final ContainerData data;

    public DistillationCubeMenu(int containerId, Inventory inventory, FriendlyByteBuf buf) {
        this(containerId, inventory,
                inventory.player.level().getBlockEntity(buf.readBlockPos()) instanceof DistillationCubeBlockEntity cube ? cube : null,
                new SimpleContainerData(DistillationCubeBlockEntity.DATA_COUNT));
    }

    public DistillationCubeMenu(int containerId, Inventory inventory, @Nullable DistillationCubeBlockEntity blockEntity, ContainerData data) {
        super(ModMenuTypes.DISTILLATION_CUBE_MENU.get(), containerId);
        this.blockEntity = blockEntity;
        this.data = data;
        ItemStackHandler items = blockEntity != null ? blockEntity.getItems() : new ItemStackHandler(MACHINE_SLOTS);
        addSlot(new SlotItemHandler(items, DistillationCubeBlockEntity.INPUT_SLOT, 44, 17));
        addSlot(new SlotItemHandler(items, DistillationCubeBlockEntity.FUEL_SLOT, 44, 53));
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

    public int value(int index) {
        return data.get(index);
    }

    public int note(NoteType note) {
        return data.get(DistillationCubeBlockEntity.DATA_NOTES + note.ordinal());
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (blockEntity == null || id < 0 || id >= NoteType.values().length) return false;
        NoteType note = NoteType.values()[id];
        ItemStack carried = getCarried();
        int bottleCapacity = Config.NOTE_BOTTLE_CAPACITY.getAsInt();
        if (carried.is(Items.GLASS_BOTTLE)) {
            int taken = blockEntity.extractNote(note, bottleCapacity);
            if (taken <= 0) return false;
            ItemStack filled = EssenceBottleItem.of(note.id(), taken);
            carried.shrink(1);
            if (carried.isEmpty()) {
                setCarried(filled);
            } else if (!player.getInventory().add(filled)) {
                player.drop(filled, false);
            }
        } else if (EssenceBottleItem.typeOf(carried).equals(note.id()) && EssenceBottleItem.amountOf(carried) < bottleCapacity) {
            int taken = blockEntity.extractNote(note, bottleCapacity - EssenceBottleItem.amountOf(carried));
            if (taken <= 0) return false;
            carried.set(ModDataComponents.ESSENCE_AMOUNT.get(), EssenceBottleItem.amountOf(carried) + taken);
        } else {
            return false;
        }
        player.level().playSound(null, blockEntity.getBlockPos(), ModSoundEvents.NOTE_JAR_CLINK.get(), SoundSource.BLOCKS, 0.7f, 1.1f);
        if (player instanceof ServerPlayer serverPlayer) SmokecraftTrigger.fire(serverPlayer, "distill");
        return true;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(stack, MACHINE_SLOTS, PLAYER_SLOTS_END, true)) return ItemStack.EMPTY;
        } else if (DistillationCubeBlockEntity.distillable(stack) && !fuelFirst(stack)) {
            if (!moveItemStackTo(stack, DistillationCubeBlockEntity.INPUT_SLOT, DistillationCubeBlockEntity.INPUT_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getBurnTime(RecipeType.SMELTING) > 0) {
            if (!moveItemStackTo(stack, DistillationCubeBlockEntity.FUEL_SLOT, DistillationCubeBlockEntity.FUEL_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    private static boolean fuelFirst(ItemStack stack) {
        return stack.getBurnTime(RecipeType.SMELTING) > 0 && NoteProfiles.explicitFor(stack) == null;
    }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity != null && !blockEntity.isRemoved()
                && SableCompat.distanceSqrToBlockCenter(player, blockEntity.getBlockPos()) <= 64.0;
    }
}
