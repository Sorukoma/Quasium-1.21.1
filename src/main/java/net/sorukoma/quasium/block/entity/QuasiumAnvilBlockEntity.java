package net.sorukoma.quasium.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.sorukoma.quasium.data.PlayerKaifData;
import net.sorukoma.quasium.recipe.AnvilRecipes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class QuasiumAnvilBlockEntity extends BlockEntity {

    private int hitCount = 0;

    private final ItemStackHandler itemHandler = new ItemStackHandler(3) {
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

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            // Only accept items if slot is empty
            return getStackInSlot(slot).isEmpty();
        }
    };

    public QuasiumAnvilBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.QUASIUM_ANVIL_BE.get(), pos, state);
    }

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    public int getHitCount() {
        return hitCount;
    }

    public void incrementHitCount() {
        hitCount++;
        setChanged();
    }

    public void resetHitCount() {
        hitCount = 0;
        setChanged();
    }

    public enum Strike {
        NO_RECIPE, UNSTUDIED, LOW_KAIF, MAXED, READY, HIT, CRAFTED
    }

    public Strike readiness(Player striker) {
        AnvilRecipes.AnvilRecipe recipe = getCurrentRecipe();
        if (recipe == null) return Strike.NO_RECIPE;
        if (!recipe.studiedBy(striker)) return Strike.UNSTUDIED;
        if (!recipe.allows(PlayerKaifData.getKaifValue(striker))) return Strike.LOW_KAIF;
        return hitCount < recipe.hitsRequired() ? Strike.READY : Strike.MAXED;
    }

    public Strike strike(Player striker, Runnable onHit, Consumer<ItemStack> output) {
        Strike readiness = readiness(striker);
        if (readiness != Strike.READY) return readiness;
        AnvilRecipes.AnvilRecipe recipe = getCurrentRecipe();
        onHit.run();
        level.playSound(null, worldPosition, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0f, 1.0f);
        incrementHitCount();
        if (hitCount < recipe.hitsRequired()) return Strike.HIT;
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            itemHandler.setStackInSlot(i, ItemStack.EMPTY);
        }
        output.accept(new ItemStack(recipe.output().get()));
        resetHitCount();
        return Strike.CRAFTED;
    }

    /**
     * Check if current slot contents match any anvil recipe.
     */
    public AnvilRecipes.AnvilRecipe getCurrentRecipe() {
        List<ItemStack> slotContents = new ArrayList<>();
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            slotContents.add(itemHandler.getStackInSlot(i));
        }
        return AnvilRecipes.find(slotContents);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", itemHandler.serializeNBT(registries));
        tag.putInt("HitCount", hitCount);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
        hitCount = tag.getInt("HitCount");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.put("inventory", itemHandler.serializeNBT(registries));
        tag.putInt("HitCount", hitCount);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
