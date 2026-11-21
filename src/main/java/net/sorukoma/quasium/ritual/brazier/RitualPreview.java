package net.sorukoma.quasium.ritual.brazier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.sorukoma.quasium.magic.NoteType;
import net.sorukoma.quasium.ritual.RitualRecipe;
import net.sorukoma.quasium.ritual.RitualRecipes;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record RitualPreview(List<Ghost> ghosts, List<NoteBar> notes) {
    public static final RitualPreview NONE = new RitualPreview(List.of(), List.of());

    public record Ghost(BlockPos pedestal, Item item) {
    }

    public record NoteBar(NoteType note, int have, int need) {
        public float fill() {
            return need <= 0 ? 1f : Math.min(1f, have / (float) need);
        }
    }

    public boolean isEmpty() {
        return ghosts.isEmpty() && notes.isEmpty();
    }

    static RitualPreview of(ServerLevel level, RitualBrazierBlockEntity brazier) {
        if (brazier.catalyst().isEmpty()) return NONE;
        BlockPos pos = brazier.getBlockPos();
        List<RitualPedestalBlockEntity> pedestals = RitualSite.pedestals(level, pos);
        List<ItemStack> offered = RitualSite.offered(pedestals);
        Optional<Map.Entry<ResourceLocation, RitualRecipe>> target = RitualRecipes.target(brazier.catalyst(), offered);
        if (target.isEmpty()) return NONE;
        RitualRecipe recipe = target.get().getValue();
        Iterator<RitualPedestalBlockEntity> free = pedestals.stream().filter(pedestal -> pedestal.item().isEmpty()).iterator();
        List<Ghost> ghosts = new ArrayList<>();
        for (int slot : recipe.missingPedestals(offered)) {
            ItemStack[] options = recipe.pedestals().get(slot).getItems();
            if (!free.hasNext()) break;
            if (options.length > 0) ghosts.add(new Ghost(free.next().getBlockPos(), options[0].getItem()));
        }
        Map<NoteType, Integer> stock = RitualSite.stock(RitualSite.jars(level, pos));
        List<NoteBar> notes = recipe.notes().entrySet().stream()
                .map(need -> new NoteBar(need.getKey(), Math.min(stock.getOrDefault(need.getKey(), 0), need.getValue()), need.getValue()))
                .toList();
        return new RitualPreview(List.copyOf(ghosts), notes);
    }

    CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        ListTag ghostTags = new ListTag();
        for (Ghost ghost : ghosts) {
            CompoundTag entry = new CompoundTag();
            entry.putLong("Pos", ghost.pedestal().asLong());
            entry.putString("Item", BuiltInRegistries.ITEM.getKey(ghost.item()).toString());
            ghostTags.add(entry);
        }
        ListTag noteTags = new ListTag();
        for (NoteBar bar : notes) {
            CompoundTag entry = new CompoundTag();
            entry.putString("Note", bar.note().id());
            entry.putInt("Have", bar.have());
            entry.putInt("Need", bar.need());
            noteTags.add(entry);
        }
        tag.put("Ghosts", ghostTags);
        tag.put("Notes", noteTags);
        return tag;
    }

    static RitualPreview load(CompoundTag tag) {
        List<Ghost> ghosts = new ArrayList<>();
        for (Tag element : tag.getList("Ghosts", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) element;
            ResourceLocation id = ResourceLocation.tryParse(entry.getString("Item"));
            Item item = id == null ? Items.AIR : BuiltInRegistries.ITEM.get(id);
            if (item != Items.AIR) ghosts.add(new Ghost(BlockPos.of(entry.getLong("Pos")), item));
        }
        List<NoteBar> notes = new ArrayList<>();
        for (Tag element : tag.getList("Notes", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) element;
            NoteType note = NoteType.byId(entry.getString("Note"));
            if (note != null) notes.add(new NoteBar(note, entry.getInt("Have"), entry.getInt("Need")));
        }
        return new RitualPreview(List.copyOf(ghosts), List.copyOf(notes));
    }
}
