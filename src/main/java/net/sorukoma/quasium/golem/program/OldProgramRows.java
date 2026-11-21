package net.sorukoma.quasium.golem.program;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class OldProgramRows {
    private static final int ROWS = 8;
    private static final int SLOTS_PER_ROW = 3;

    record Upgrade(Program program, List<ItemStack> markers) {
    }

    private OldProgramRows() {
    }

    static boolean present(CompoundTag tag) {
        return tag.contains("Rows", Tag.TAG_LIST);
    }

    static Upgrade read(CompoundTag tag, HolderLookup.Provider registries) {
        ItemStackHandler slots = new ItemStackHandler(ROWS * SLOTS_PER_ROW);
        slots.deserializeNBT(registries, tag.getCompound("Slots"));
        ListTag rows = tag.getList("Rows", Tag.TAG_COMPOUND);
        List<ProgramBlock> blocks = new ArrayList<>();
        List<ItemStack> markers = new ArrayList<>();
        int areaRow = -2;
        for (int row = 0; row < ROWS; row++) {
            ItemStack a = slots.getStackInSlot(row * SLOTS_PER_ROW);
            ItemStack b = slots.getStackInSlot(row * SLOTS_PER_ROW + 1);
            if (!a.isEmpty()) markers.add(a.copy());
            if (!b.isEmpty()) markers.add(b.copy());
            BlockKind kind = kind(rows.getCompound(row).getString("Type"));
            if (kind == null) continue;
            ProgramBlock block = ProgramBlock.of(kind).withNumber(rows.getCompound(row).getInt("Number"));
            if (kind.usesFilter()) block = block.withFilter(ProgramTableBlockEntity.ghost(slots.getStackInSlot(row * SLOTS_PER_ROW + 2), registries));
            if (kind != BlockKind.FOR_EACH && areaRow == row - 1) {
                blocks.set(blocks.size() - 1, blocks.getLast().withBody(0, List.of(block)));
                continue;
            }
            block = block.withPlace(ProgramBlock.CORNER_A, kind.usesA() ? place(a) : Optional.empty())
                    .withPlace(ProgramBlock.CORNER_B, kind.usesB() ? place(b) : Optional.empty());
            blocks.add(block);
            areaRow = kind == BlockKind.FOR_EACH ? row : -2;
        }
        return new Upgrade(new Program(blocks), markers);
    }

    @Nullable
    private static BlockKind kind(String id) {
        return "area".equals(id) ? BlockKind.FOR_EACH : BlockKind.CODEC.byName(id);
    }

    private static Optional<MarkedPlace> place(ItemStack marker) {
        return Optional.ofNullable(marker.get(ProgramBlocks.MARKED_PLACE.get()));
    }
}
