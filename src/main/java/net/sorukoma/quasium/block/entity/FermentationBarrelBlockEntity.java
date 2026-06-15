package net.sorukoma.quasium.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.sorukoma.quasium.block.FermentationBarrelBlock;
import net.sorukoma.quasium.component.ModDataComponents;
import net.sorukoma.quasium.item.ModItems;
import org.jetbrains.annotations.Nullable;

public class FermentationBarrelBlockEntity extends BlockEntity {

    private final ItemStackHandler itemHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }

        @Override
        public int getSlotLimit(int slot) {
            return 16;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (fermenting) return false;
            if (stack.has(ModDataComponents.FERMENTED.get())) return false;
            ItemStack current = getStackInSlot(slot);
            if (!current.isEmpty()) {
                return stack.getItem() == current.getItem();
            }
            return stack.getItem() == ModItems.TOBACCO_LEAVES.get()
                    || stack.getItem() == ModItems.MAKHORKA_LEAVES.get();
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (fermenting) return ItemStack.EMPTY;
            return super.extractItem(slot, amount, simulate);
        }
    };

    private boolean fermenting = false;
    private int fermentationProgress = 0;
    private int fermentationTotalTime = 200; // 10 seconds for testing

    public FermentationBarrelBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FERMENTATION_BARREL_BE.get(), pos, state);
    }

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    public boolean isFermenting() {
        return fermenting;
    }

    public int getFermentationProgress() {
        return fermentationProgress;
    }

    public int getFermentationTotalTime() {
        return fermentationTotalTime;
    }

    public void startFermentation() {
        this.fermenting = true;
        this.fermentationProgress = 0;
        this.fermentationTotalTime = 200;
        setChanged();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, FermentationBarrelBlockEntity entity) {
        if (level.isClientSide) return;

        if (entity.fermenting && !state.getValue(FermentationBarrelBlock.FERMENTING)) {
            level.setBlock(pos, state.setValue(FermentationBarrelBlock.FERMENTING, true), 3);
        }

        if (!entity.fermenting) return;

        entity.fermentationProgress++;

        if (entity.fermentationProgress >= entity.fermentationTotalTime) {
            ItemStack input = entity.itemHandler.getStackInSlot(0);
            if (!input.isEmpty()) {
                ItemStack result = input.copy();
                result.set(ModDataComponents.FERMENTED.get(), true);
                entity.itemHandler.setStackInSlot(0, result);
            }
            entity.fermenting = false;
            entity.fermentationProgress = 0;
            entity.fermentationTotalTime = 0;

            level.setBlock(pos, state.setValue(FermentationBarrelBlock.FERMENTING, false), 3);
        }

        entity.setChanged();
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.put("inventory", itemHandler.serializeNBT(registries));
        tag.putBoolean("fermenting", fermenting);
        tag.putInt("fermentationProgress", fermentationProgress);
        tag.putInt("fermentationTotalTime", fermentationTotalTime);
        return tag;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
        fermenting = tag.getBoolean("fermenting");
        fermentationProgress = tag.getInt("fermentationProgress");
        fermentationTotalTime = tag.contains("fermentationTotalTime") ? tag.getInt("fermentationTotalTime") : 200;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", itemHandler.serializeNBT(registries));
        tag.putBoolean("fermenting", fermenting);
        tag.putInt("fermentationProgress", fermentationProgress);
        tag.putInt("fermentationTotalTime", fermentationTotalTime);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}