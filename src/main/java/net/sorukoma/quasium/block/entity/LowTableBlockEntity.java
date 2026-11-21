package net.sorukoma.quasium.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.sorukoma.quasium.block.LoungeBlocks;
import org.jetbrains.annotations.Nullable;

public class LowTableBlockEntity extends BlockEntity {
    public static final int SLOTS = 4;
    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);

    public LowTableBlockEntity(BlockPos pos, BlockState state) {
        super(LoungeBlocks.LOW_TABLE_BE.get(), pos, state);
    }

    public NonNullList<ItemStack> items() {
        return items;
    }

    public boolean isFull() {
        return items.stream().noneMatch(ItemStack::isEmpty);
    }

    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    public boolean place(ItemStack stack) {
        for (int slot = 0; slot < SLOTS; slot++) {
            if (items.get(slot).isEmpty()) {
                items.set(slot, stack);
                changed();
                return true;
            }
        }
        return false;
    }

    public ItemStack takeLast() {
        for (int slot = SLOTS - 1; slot >= 0; slot--) {
            if (!items.get(slot).isEmpty()) {
                ItemStack taken = items.set(slot, ItemStack.EMPTY);
                changed();
                return taken;
            }
        }
        return ItemStack.EMPTY;
    }

    public void spill() {
        if (isEmpty() || level == null) return;
        Containers.dropContents(level, worldPosition, items);
        items.clear();
        changed();
    }

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.clear();
        ContainerHelper.loadAllItems(tag, items, registries);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
