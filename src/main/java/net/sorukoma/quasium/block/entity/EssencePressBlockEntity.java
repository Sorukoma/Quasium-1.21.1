package net.sorukoma.quasium.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class EssencePressBlockEntity extends BlockEntity {
    private String essenceType = "";
    private int essenceAmount = 0;
    private String inputType = "";
    private int inputCount = 0;

    public EssencePressBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ESSENCE_PRESS_BE.get(), pos, state);
    }

    public String getEssenceType() { return essenceType; }
    public int getEssenceAmount() { return essenceAmount; }
    public int getInputCount() { return inputCount; }
    public String getInputType() { return inputType; }

    public int addIngredients(String type, int amount) {
        if (amount <= 0 || essenceAmount > 0 || (!inputType.isEmpty() && !inputType.equals(type))) return 0;
        inputType = type;
        int accepted = Math.min(amount, 4 - inputCount);
        inputCount += accepted;
        if (inputCount >= 4) {
            essenceType = inputType;
            essenceAmount = 20;
            inputType = "";
            inputCount = 0;
        }
        sync();
        return accepted;
    }

    public void consumeEssence() {
        essenceAmount = 0;
        essenceType = "";
        sync();
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("essenceType", essenceType);
        tag.putInt("essenceAmount", essenceAmount);
        tag.putString("inputType", inputType);
        tag.putInt("inputCount", inputCount);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        essenceType = tag.getString("essenceType");
        essenceAmount = tag.getInt("essenceAmount");
        inputType = tag.getString("inputType");
        inputCount = tag.getInt("inputCount");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
