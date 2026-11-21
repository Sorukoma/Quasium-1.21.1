package net.sorukoma.quasium.golem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class SmokeMarkBlockEntity extends BlockEntity {
    private final Set<UUID> golems = new HashSet<>();
    private final List<BlockPos> links = new ArrayList<>();
    @Nullable
    private UUID owner;
    @Nullable
    private BlockPos corner;

    public SmokeMarkBlockEntity(BlockPos pos, BlockState state) {
        this(MarkBlocks.MARK_BE.get(), pos, state);
    }

    protected SmokeMarkBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public MarkType type() {
        return ((SmokeMarkBlock) getBlockState().getBlock()).type();
    }

    public Set<UUID> golems() {
        return Collections.unmodifiableSet(golems);
    }

    public List<BlockPos> links() {
        return Collections.unmodifiableList(links);
    }

    @Nullable
    public BlockPos corner() {
        return corner;
    }

    void setCorner(Player by, BlockPos pos) {
        corner = pos.immutable();
        owner = by.getUUID();
        changed();
    }

    public boolean isOwnedBy(Player player) {
        return player.getUUID().equals(owner);
    }

    public boolean mayEdit(Player player) {
        return owner == null || isOwnedBy(player);
    }

    void claim(Player player) {
        owner = player.getUUID();
        changed();
    }

    boolean toggleGolem(Player by, UUID golem) {
        boolean bound = !golems.remove(golem);
        if (bound) golems.add(golem);
        owner = by.getUUID();
        changed();
        return bound;
    }

    void forget(UUID golem) {
        if (golems.remove(golem)) changed();
    }

    boolean toggleLink(Player by, BlockPos store) {
        boolean linked = !links.remove(store);
        if (linked) links.add(store.immutable());
        owner = by.getUUID();
        changed();
        return linked;
    }

    boolean hasLinks() {
        return !links.isEmpty();
    }

    List<BlockPos> liveLinks() {
        pruneLinks();
        return links.stream().filter(level::isLoaded).toList();
    }

    void pruneLinks() {
        if (links.removeIf(store -> level.isLoaded(store) && level.getCapability(Capabilities.ItemHandler.BLOCK, store, null) == null)) {
            changed();
        }
    }

    protected void changed() {
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (owner != null) tag.putUUID("Owner", owner);
        ListTag bound = new ListTag();
        for (UUID golem : golems) bound.add(NbtUtils.createUUID(golem));
        tag.put("Golems", bound);
        tag.putLongArray("Links", links.stream().mapToLong(BlockPos::asLong).toArray());
        if (corner != null) tag.putLong("Corner", corner.asLong());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        golems.clear();
        for (Tag golem : tag.getList("Golems", Tag.TAG_INT_ARRAY)) golems.add(NbtUtils.loadUUID(golem));
        links.clear();
        for (long store : tag.getLongArray("Links")) links.add(BlockPos.of(store));
        corner = tag.contains("Corner") ? BlockPos.of(tag.getLong("Corner")) : null;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
