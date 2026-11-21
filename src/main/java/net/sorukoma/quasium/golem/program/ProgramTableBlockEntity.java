package net.sorukoma.quasium.golem.program;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.sorukoma.quasium.Config;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.golem.GolemBindings;
import net.sorukoma.quasium.golem.SmokeMarkBlockEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class ProgramTableBlockEntity extends SmokeMarkBlockEntity {
    private static final int GHOST_BYTES = 1024;

    private Program program = Program.EMPTY;
    private int edits;
    private final List<ItemStack> oldMarkers = new ArrayList<>();
    private final Map<UUID, Integer> running = new HashMap<>();

    public ProgramTableBlockEntity(BlockPos pos, BlockState state) {
        super(ProgramBlocks.PROGRAM_TABLE_BE.get(), pos, state);
    }

    public Program program() {
        return program;
    }

    public void setProgram(Program program) {
        this.program = program;
        running.clear();
        edits++;
        changed();
    }

    public int edits() {
        return edits;
    }

    public static boolean fits(Program program) {
        return program.size() <= Config.PROGRAM_MAX_BLOCKS.get() && program.nesting() <= Config.PROGRAM_MAX_NESTING.get();
    }

    public boolean apply(ProgramEdit edit, Player player, ItemStack carried) {
        Optional<Program> edited = switch (edit.op()) {
            case ADD -> add(edit.at(), edit.value(), player);
            case MOVE -> program.move(edit.at(), edit.to());
            case REMOVE -> program.remove(edit.at());
            case NUMBER -> program.replace(edit.at(), block -> block.withNumber(edit.value()));
            case CHECK -> check(edit.at(), edit.value(), player);
            case PLACE -> place(edit.at(), edit.value(), carried);
            case FILTER -> filter(edit.at(), carried);
            case SAVE_CARD, LOAD_CARD -> Optional.empty();
        };
        if (edited.isEmpty() || !fits(edited.get())) return false;
        setProgram(edited.get());
        return true;
    }

    private Optional<Program> add(ProgramPath spot, int kindId, Player player) {
        if (kindId < 0 || kindId >= BlockKind.values().length) return Optional.empty();
        BlockKind kind = BlockKind.values()[kindId];
        return ProgramUnlocks.mayUse(player, kind) ? program.insert(spot, ProgramBlock.of(kind)) : Optional.empty();
    }

    private Optional<Program> check(ProgramPath path, int checkId, Player player) {
        ProgramBlock block = program.at(path);
        if (block == null || !block.kind().usesCheck() || checkId < 0 || checkId >= Check.values().length) return Optional.empty();
        Check check = Check.values()[checkId];
        return ProgramUnlocks.mayUse(player, check) ? program.replace(path, old -> old.withCheck(check)) : Optional.empty();
    }

    private Optional<Program> place(ProgramPath path, int corner, ItemStack carried) {
        ProgramBlock block = program.at(path);
        if (block == null || !block.uses(corner)) return Optional.empty();
        if (carried.isEmpty()) return program.replace(path, old -> old.withPlace(corner, Optional.empty()));
        MarkedPlace place = carried.is(ProgramBlocks.PLACE_MARKER.get()) ? carried.get(ProgramBlocks.MARKED_PLACE.get()) : null;
        return place == null ? Optional.empty() : program.replace(path, old -> old.withPlace(corner, Optional.of(place)));
    }

    private Optional<Program> filter(ProgramPath path, ItemStack carried) {
        ProgramBlock block = program.at(path);
        if (block == null || !block.usesFilter()) return Optional.empty();
        ItemStack ghost = ghost(carried, level.registryAccess());
        return program.replace(path, old -> old.withFilter(ghost));
    }

    public boolean reaches(MarkedPlace place) {
        int reach = GolemBindings.markRadius();
        return place.where().dimension() == level.dimension() && place.where().pos().distSqr(worldPosition) <= (double) reach * reach;
    }

    public void running(UUID golem, int index) {
        if (index < 0) running.remove(golem);
        else running.put(golem, index);
    }

    public long runningMask() {
        long mask = 0;
        for (int index : running.values()) {
            if (index < Long.SIZE) mask |= 1L << index;
        }
        return mask;
    }

    static ItemStack ghost(ItemStack stack, HolderLookup.Provider registries) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack copy = stack.copyWithCount(1);
        boolean light = ItemStack.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), copy).result()
                .filter(tag -> tag.sizeInBytes() <= GHOST_BYTES).isPresent();
        return light ? copy : new ItemStack(stack.getItem());
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (!level.isClientSide && !oldMarkers.isEmpty()) level.scheduleTick(worldPosition, getBlockState().getBlock(), 1);
    }

    void spillOldMarkers() {
        if (oldMarkers.isEmpty()) return;
        for (ItemStack marker : oldMarkers) Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY() + 1, worldPosition.getZ(), marker);
        oldMarkers.clear();
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        Program.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), program).result()
                .ifPresent(encoded -> tag.put("Program", encoded));
        if (oldMarkers.isEmpty()) return;
        ListTag markers = new ListTag();
        for (ItemStack marker : oldMarkers) markers.add(marker.save(registries));
        tag.put("OldMarkers", markers);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        oldMarkers.clear();
        for (Tag marker : tag.getList("OldMarkers", Tag.TAG_COMPOUND)) ItemStack.parse(registries, marker).ifPresent(oldMarkers::add);
        if (OldProgramRows.present(tag)) {
            OldProgramRows.Upgrade upgrade = OldProgramRows.read(tag, registries);
            program = upgrade.program();
            oldMarkers.addAll(upgrade.markers());
            return;
        }
        Tag stored = tag.get("Program");
        program = stored == null ? Program.EMPTY : Program.CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), stored)
                .resultOrPartial(QuasiumMod.LOGGER::warn).orElse(Program.EMPTY);
    }
}
