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
import net.sorukoma.quasium.item.TobaccoVarieties;
import net.sorukoma.quasium.selection.quality.LeafQuality;
import org.jetbrains.annotations.Nullable;

public class FermentationBarrelBlockEntity extends BlockEntity {

    private final ItemStackHandler itemHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            if (getStackInSlot(0).isEmpty()) sugarBoosts = 0;
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
            return TobaccoVarieties.isLeaf(stack);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (fermenting) return ItemStack.EMPTY;
            return super.extractItem(slot, amount, simulate);
        }
    };

    private boolean fermenting = false;
    private boolean ready = false;
    private boolean spoiled = false;
    private int fermentationProgress = 0;
    private int fermentationTotalTime = 1200;
    private int spoilGraceTime = 1200;
    private int sugarBoosts = 0;

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

    public boolean isReady() { return ready; }
    public boolean isSpoiled() { return spoiled; }

    public static boolean sameBatch(ItemStack inside, ItemStack leaves) {
        return inside.isEmpty() || ItemStack.isSameItemSameComponents(withoutDryingCounters(inside), withoutDryingCounters(leaves));
    }

    private static ItemStack withoutDryingCounters(ItemStack leaves) {
        ItemStack copy = leaves.copyWithCount(1);
        copy.remove(ModDataComponents.DRYING_PROGRESS.get());
        copy.remove(ModDataComponents.DRYING_RATE.get());
        return copy;
    }

    public ItemStack takeResult() {
        if (!ready || itemHandler.getStackInSlot(0).isEmpty()) return ItemStack.EMPTY;
        ItemStack result = itemHandler.getStackInSlot(0).copy();
        itemHandler.setStackInSlot(0, ItemStack.EMPTY);
        fermenting = false;
        ready = false;
        spoiled = false;
        fermentationProgress = 0;
        fermentationTotalTime = 1200;
        setChanged();
        return result;
    }

    public void startFermentation() {
        this.fermenting = true;
        this.ready = false;
        this.spoiled = false;
        this.fermentationProgress = 0;
        this.fermentationTotalTime = Math.max(600, 1200 - sugarBoosts * 200);
        setChanged();
    }

    public boolean addSugar() {
        if (itemHandler.getStackInSlot(0).isEmpty() || ready || spoiled || sugarBoosts >= 3) return false;
        sugarBoosts++;
        if (fermenting) fermentationProgress = Math.min(fermentationTotalTime, fermentationProgress + 400);
        setChanged();
        return true;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, FermentationBarrelBlockEntity entity) {
        if (level.isClientSide) return;

        if (entity.fermenting && !state.getValue(FermentationBarrelBlock.FERMENTING)) {
            level.setBlock(pos, state.setValue(FermentationBarrelBlock.FERMENTING, true), 3);
        }

        if (!entity.fermenting) return;

        entity.fermentationProgress++;

        if (entity.fermentationProgress % 20 == 0 && level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.BUBBLE_POP,
                    pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5,
                    3, 0.25, 0.05, 0.25, 0.01);
        }

        if (!entity.ready && entity.fermentationProgress >= entity.fermentationTotalTime) {
            ItemStack input = entity.itemHandler.getStackInSlot(0);
            entity.spoiled = entity.sugarBoosts > 0 && level.random.nextFloat() < 0.15f * entity.sugarBoosts;
            if (!input.isEmpty()) {
                ItemStack result = input.copy();
                result.set(ModDataComponents.FERMENTED.get(), !entity.spoiled);
                if (entity.spoiled) result.set(ModDataComponents.SPOILED.get(), true);
                if (entity.sugarBoosts == 0) LeafQuality.fermentedClean(result);
                entity.itemHandler.setStackInSlot(0, result);
            }
            entity.fermenting = false;
            entity.ready = true;
            level.setBlock(pos, state.setValue(FermentationBarrelBlock.FERMENTING, false), 3);
            level.playSound(null, pos, net.minecraft.sounds.SoundEvents.BREWING_STAND_BREW, net.minecraft.sounds.SoundSource.BLOCKS, 0.8f, 1.1f);
            entity.setChangedAndSync();
        }

        entity.setChanged();
    }

    private void setChangedAndSync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.put("inventory", itemHandler.serializeNBT(registries));
        tag.putBoolean("fermenting", fermenting);
        tag.putBoolean("ready", ready);
        tag.putBoolean("spoiled", spoiled);
        tag.putInt("fermentationProgress", fermentationProgress);
        tag.putInt("fermentationTotalTime", fermentationTotalTime);
        tag.putInt("sugarBoosts", sugarBoosts);
        return tag;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
        fermenting = tag.getBoolean("fermenting");
        ready = tag.getBoolean("ready");
        spoiled = tag.getBoolean("spoiled");
        fermentationProgress = tag.getInt("fermentationProgress");
        fermentationTotalTime = tag.contains("fermentationTotalTime") ? tag.getInt("fermentationTotalTime") : 2400;
        sugarBoosts = tag.contains("sugarBoosts") ? tag.getInt("sugarBoosts") : 0;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", itemHandler.serializeNBT(registries));
        tag.putBoolean("fermenting", fermenting);
        tag.putBoolean("ready", ready);
        tag.putBoolean("spoiled", spoiled);
        tag.putInt("fermentationProgress", fermentationProgress);
        tag.putInt("fermentationTotalTime", fermentationTotalTime);
        tag.putInt("sugarBoosts", sugarBoosts);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
