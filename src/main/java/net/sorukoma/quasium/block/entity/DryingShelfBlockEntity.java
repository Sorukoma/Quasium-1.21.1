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
import net.sorukoma.quasium.recipe.DryingRecipes;
import org.jetbrains.annotations.Nullable;

public class DryingShelfBlockEntity extends BlockEntity {

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
            return 1;
        }
    };

    private int dryingProgress = 0;
    private int dryingTotalTime = 0;

    public DryingShelfBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DRYING_SHELF_BE.get(), pos, state);
    }

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    public int getDryingProgress() {
        return dryingProgress;
    }

    public int getDryingTotalTime() {
        return dryingTotalTime;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, DryingShelfBlockEntity entity) {
        if (level.isClientSide) return;

        ItemStack input = entity.itemHandler.getStackInSlot(0);
        if (input.isEmpty()) {
            if (entity.dryingProgress > 0) {
                entity.dryingProgress = 0;
                entity.dryingTotalTime = 0;
                entity.setChanged();
            }
            return;
        }

        DryingRecipes.DryingRecipe recipe = DryingRecipes.find(input);
        if (recipe == null) {
            if (entity.dryingProgress > 0) {
                entity.dryingProgress = 0;
                entity.dryingTotalTime = 0;
                entity.setChanged();
            }
            return;
        }

        if (entity.dryingTotalTime == 0) {
            entity.dryingTotalTime = recipe.dryingTime();
            entity.dryingProgress = 0;
        }

        entity.dryingProgress++;

        if (entity.dryingProgress >= entity.dryingTotalTime) {
            ItemStack output = new ItemStack(recipe.output().get(), 1);
            entity.itemHandler.setStackInSlot(0, output);
            entity.dryingProgress = 0;
            entity.dryingTotalTime = 0;
        }

        entity.setChanged();
    }

    // --- Client sync for BlockEntityRenderer ---

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.put("inventory", itemHandler.serializeNBT(registries));
        tag.putInt("dryingProgress", dryingProgress);
        tag.putInt("dryingTotalTime", dryingTotalTime);
        return tag;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
        dryingProgress = tag.getInt("dryingProgress");
        dryingTotalTime = tag.getInt("dryingTotalTime");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", itemHandler.serializeNBT(registries));
        tag.putInt("dryingProgress", dryingProgress);
        tag.putInt("dryingTotalTime", dryingTotalTime);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}