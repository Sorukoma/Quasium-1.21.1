package net.sorukoma.quasium.golem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class GolemFeederBlockEntity extends SmokeMarkBlockEntity {
    public static final int SLOTS = 9;
    private static final int SEAT_TICKS = 60;

    private final ItemStackHandler food = new ItemStackHandler(SLOTS) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return GolemMood.Food.of(stack) != null;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            showApples();
        }
    };
    @Nullable
    private UUID eater;
    private long seatedUntil;

    public GolemFeederBlockEntity(BlockPos pos, BlockState state) {
        super(MarkBlocks.FEEDER_BE.get(), pos, state);
    }

    public ItemStackHandler food() {
        return food;
    }

    public boolean isEmpty() {
        for (int slot = 0; slot < SLOTS; slot++) {
            if (!food.getStackInSlot(slot).isEmpty()) return false;
        }
        return true;
    }

    @Nullable
    public UUID eater() {
        return level != null && level.getGameTime() < seatedUntil ? eater : null;
    }

    boolean seat(UUID golem, long now) {
        if (eater != null && !eater.equals(golem) && now < seatedUntil) return false;
        eater = golem;
        seatedUntil = now + SEAT_TICKS;
        return true;
    }

    void leave(UUID golem) {
        if (golem.equals(eater)) eater = null;
    }

    void dropFood() {
        for (int slot = 0; slot < SLOTS; slot++) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), food.getStackInSlot(slot));
        }
    }

    private void showApples() {
        if (level == null || level.isClientSide) return;
        BlockState state = getBlockState();
        boolean filled = !isEmpty();
        if (state.getValue(GolemFeederBlock.FILLED) != filled) {
            level.setBlock(worldPosition, state.setValue(GolemFeederBlock.FILLED, filled), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Food", food.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        food.deserializeNBT(registries, tag.getCompound("Food"));
    }
}
