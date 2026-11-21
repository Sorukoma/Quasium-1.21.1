package net.sorukoma.quasium.golem;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.sorukoma.quasium.Config;
import net.sorukoma.quasium.magic.NoteType;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

final class ClayUpgrades {
    static final Set<NoteType> USEFUL = EnumSet.of(NoteType.MENTHOL, NoteType.NECTAR, NoteType.DEW, NoteType.ASH, NoteType.RESIN);
    private static final int SLOTS = 2;

    private final NoteType[] notes = new NoteType[SLOTS];
    private final int[] ml = new int[SLOTS];

    boolean active(NoteType note) {
        for (NoteType slotted : notes) {
            if (slotted == note) return true;
        }
        return false;
    }

    boolean hasRoomFor(NoteType note) {
        return slotFor(note) >= 0;
    }

    int pour(NoteType note, int available) {
        int slot = slotFor(note);
        int perCharge = Config.CLAY_UPGRADE_ML_PER_CHARGE.getAsInt();
        int taken = available / perCharge * perCharge;
        if (slot < 0 || taken <= 0) return 0;
        notes[slot] = note;
        ml[slot] += taken;
        return taken;
    }

    void spendTask() {
        int cost = Config.CLAY_UPGRADE_ML_PER_TASK.getAsInt();
        for (int slot = 0; slot < SLOTS; slot++) {
            if (notes[slot] == null) continue;
            ml[slot] = Math.max(0, ml[slot] - cost);
            if (ml[slot] == 0) notes[slot] = null;
        }
    }

    List<Component> describe(boolean sommelier, boolean toolkeeper) {
        int perCharge = Config.CLAY_UPGRADE_ML_PER_CHARGE.getAsInt();
        List<Component> lines = new ArrayList<>();
        for (int slot = 0; slot < SLOTS; slot++) {
            lines.add(notes[slot] == null
                    ? Component.translatable("quasium.clay_golem.slot_empty", slot + 1)
                    : Component.translatable("quasium.clay_golem.slot", slot + 1, notes[slot].displayName(), ml[slot],
                    Mth.positiveCeilDiv(ml[slot], perCharge)));
        }
        if (sommelier) lines.add(Component.translatable("quasium.clay_golem.sommelier_on"));
        if (toolkeeper) lines.add(Component.translatable("quasium.clay_golem.toolkeeper_on"));
        return lines;
    }

    private int slotFor(NoteType note) {
        for (int slot = 0; slot < SLOTS; slot++) {
            if (notes[slot] == note) return slot;
        }
        for (int slot = 0; slot < SLOTS; slot++) {
            if (notes[slot] == null) return slot;
        }
        return -1;
    }

    ListTag save() {
        ListTag list = new ListTag();
        for (int slot = 0; slot < SLOTS; slot++) {
            CompoundTag entry = new CompoundTag();
            if (notes[slot] != null) {
                entry.putString("note", notes[slot].id());
                entry.putInt("ml", ml[slot]);
            }
            list.add(entry);
        }
        return list;
    }

    void load(ListTag list) {
        for (int slot = 0; slot < SLOTS; slot++) {
            CompoundTag entry = list.getCompound(slot);
            int amount = entry.getInt("ml");
            notes[slot] = amount > 0 ? NoteType.byId(entry.getString("note")) : null;
            ml[slot] = notes[slot] == null ? 0 : amount;
        }
    }
}
