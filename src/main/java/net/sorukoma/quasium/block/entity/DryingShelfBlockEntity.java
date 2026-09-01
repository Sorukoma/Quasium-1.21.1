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
import net.sorukoma.quasium.component.ModDataComponents;
import net.sorukoma.quasium.item.JerkyItem;
import net.sorukoma.quasium.item.TobaccoVarieties;
import net.sorukoma.quasium.recipe.DryingRecipes;
import org.jetbrains.annotations.Nullable;

public class DryingShelfBlockEntity extends BlockEntity {

    private final ItemStackHandler itemHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            dryingProgress = 0;
            dryingTotalTime = 0;
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

        boolean isLeaves = TobaccoVarieties.isLeaf(input);

        if (isLeaves) {
            if (entity.dryingTotalTime == 0) {
                int base = 1200;
                entity.dryingTotalTime = base;
                int currentPercent = input.getOrDefault(ModDataComponents.DRYING_PROGRESS.get(), 0);
                entity.dryingProgress = (int)((currentPercent / 100.0f) * entity.dryingTotalTime);
                if (!input.has(ModDataComponents.DRYING_STAGE.get())) {
                    input.set(ModDataComponents.DRYING_STAGE.get(), 1);
                }
            }
            entity.dryingProgress = Math.min(entity.dryingProgress + 1, entity.dryingTotalTime * 2);
            int percent = entity.dryingTotalTime > 0 ? (int)((entity.dryingProgress / (float)entity.dryingTotalTime) * 100f) : 0;
            input.set(ModDataComponents.DRYING_PROGRESS.get(), percent);

            int stage = 1;
            if (percent >= 100) stage = 2;
            if (percent >= 140) stage = 3;

            int previousStage = input.getOrDefault(ModDataComponents.DRYING_STAGE.get(), 0);
            if (previousStage != stage) {
                input.set(ModDataComponents.DRYING_STAGE.get(), stage);
                level.sendBlockUpdated(pos, state, state, 3);
            }
            entity.setChanged();
            return;
        }

        DryingRecipes.DryingRecipe recipe = DryingRecipes.find(input);
        if (recipe != null && recipe.output().get() instanceof JerkyItem jerkyItem) {
            ItemStack jerky = new ItemStack(jerkyItem, 1);
            jerky.set(ModDataComponents.DRYING_PROGRESS.get(), 0);
            jerky.set(ModDataComponents.DRYING_RATE.get(), jerkyItem.getDryingTime());
            entity.itemHandler.setStackInSlot(0, jerky);
            input = jerky;
        }

        boolean isJerky = input.getItem() instanceof JerkyItem;
        if (isJerky) {
            JerkyItem jerkyItem = (JerkyItem) input.getItem();
            int progress = input.getOrDefault(ModDataComponents.DRYING_PROGRESS.get(), 0);
            int rate = input.getOrDefault(ModDataComponents.DRYING_RATE.get(), jerkyItem.getDryingTime());
            if (progress < JerkyItem.MAX_DRYING_PERCENT) {
                if (entity.dryingTotalTime == 0) {
                    entity.dryingProgress = progress * rate / 100;
                    entity.dryingTotalTime = rate;
                }
                int elapsed = Math.min(rate * JerkyItem.MAX_DRYING_PERCENT / 100, entity.dryingProgress + 1);
                int nextProgress = Math.min(JerkyItem.MAX_DRYING_PERCENT,
                        elapsed * 100 / Math.max(1, rate));
                input.set(ModDataComponents.DRYING_PROGRESS.get(), nextProgress);
                entity.dryingProgress = elapsed;
                entity.dryingTotalTime = rate;
                entity.setChanged();
                if (nextProgress != progress) level.sendBlockUpdated(pos, state, state, 3);
            }
            return;
        }

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
