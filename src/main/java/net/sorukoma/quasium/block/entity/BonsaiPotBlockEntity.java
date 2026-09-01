package net.sorukoma.quasium.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.sorukoma.quasium.block.BonsaiPotBlock;
import org.jetbrains.annotations.Nullable;

public class BonsaiPotBlockEntity extends BlockEntity {
    private final ItemStackHandler output = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            if (getStackInSlot(slot).isEmpty() && growth >= 100) growth = 1;
            setChangedAndSync();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }
    };
    private int growth;
    private String treeType = "";

    public BonsaiPotBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BONSAI_POT_BE.get(), pos, state);
    }

    public IItemHandler getOutput() { return output; }
    public int getGrowth() { return growth; }
    public String getTreeType() { return treeType; }

    public static IItemHandler getCapability(BonsaiPotBlockEntity entity, Direction side) {
        return side == Direction.DOWN || side == null ? entity.output : null;
    }

    public void plant(String type) {
        treeType = type;
        growth = 1;
        setChangedAndSync();
    }

    public void clearTree() {
        output.setStackInSlot(0, ItemStack.EMPTY);
        treeType = "";
        growth = 0;
        setChangedAndSync();
    }

    public void harvest() {
        output.setStackInSlot(0, ItemStack.EMPTY);
        growth = 1;
        setChangedAndSync();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, BonsaiPotBlockEntity entity) {
        if (level.isClientSide || entity.treeType.isEmpty() || entity.growth >= 100) return;
        if (level.getGameTime() % 20 == 0) {
            entity.growth = Math.min(100, entity.growth + 1);
            if (entity.growth == 100) {
                entity.output.setStackInSlot(0, new ItemStack(BonsaiPotBlock.getFruit(entity.treeType)));
            }
            entity.setChangedAndSync();
        }
    }

    private void setChangedAndSync() {
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("output", output.serializeNBT(registries));
        tag.putInt("growth", growth);
        tag.putString("treeType", treeType);
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        output.deserializeNBT(registries, tag.getCompound("output"));
        growth = tag.getInt("growth");
        treeType = tag.getString("treeType");
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Nullable @Override public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
