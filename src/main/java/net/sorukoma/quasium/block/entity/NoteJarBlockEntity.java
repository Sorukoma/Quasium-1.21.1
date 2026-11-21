package net.sorukoma.quasium.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.sorukoma.quasium.Config;
import net.sorukoma.quasium.component.ModDataComponents;
import net.sorukoma.quasium.draft.NoteNode;
import net.sorukoma.quasium.magic.NoteType;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class NoteJarBlockEntity extends BlockEntity implements NoteNode {
    @Nullable
    private NoteType note;
    private int amount;

    public NoteJarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NOTE_JAR_BE.get(), pos, state);
    }

    @Nullable
    public NoteType note() {
        return note;
    }

    public int amount() {
        return amount;
    }

    public static int capacity() {
        return Config.NOTE_JAR_CAPACITY.getAsInt();
    }

    public float fill() {
        return Math.min(1f, amount / (float) capacity());
    }

    public int pour(NoteType incoming, int ml) {
        if (note != null && note != incoming) return 0;
        int accepted = Math.min(ml, capacity() - amount);
        if (accepted <= 0) return 0;
        note = incoming;
        amount += accepted;
        changed();
        return accepted;
    }

    @Override
    public int acceptNote(NoteType incoming, int ml, boolean simulate) {
        if (note != null && note != incoming) return 0;
        int room = Math.max(0, Math.min(ml, capacity() - amount));
        return simulate ? room : pour(incoming, room);
    }

    @Override
    public Map<NoteType, Integer> noteSupply() {
        return note == null || amount <= 0 ? Map.of() : Map.of(note, amount);
    }

    @Override
    public int takeNote(NoteType wanted, int ml) {
        return wanted == note ? take(ml) : 0;
    }

    @Override
    public boolean isStorage() {
        return true;
    }

    public int take(int maxMl) {
        int taken = Math.min(maxMl, amount);
        if (taken <= 0) return 0;
        amount -= taken;
        if (amount == 0) note = null;
        changed();
        return taken;
    }

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (note != null) {
            tag.putString("Note", note.id());
            tag.putInt("Amount", amount);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        note = NoteType.byId(tag.getString("Note"));
        amount = note == null ? 0 : tag.getInt("Amount");
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        note = NoteType.byId(input.getOrDefault(ModDataComponents.ESSENCE_TYPE.get(), ""));
        amount = note == null ? 0 : input.getOrDefault(ModDataComponents.ESSENCE_AMOUNT.get(), 0);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (note != null && amount > 0) {
            components.set(ModDataComponents.ESSENCE_TYPE.get(), note.id());
            components.set(ModDataComponents.ESSENCE_AMOUNT.get(), amount);
        }
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        tag.remove("Note");
        tag.remove("Amount");
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
