package net.sorukoma.quasium.mind.research;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.sorukoma.quasium.item.EssenceBottleItem;
import net.sorukoma.quasium.magic.NoteType;
import net.sorukoma.quasium.magic.Spells;
import net.sorukoma.quasium.mind.MindConfig;
import net.sorukoma.quasium.mind.MindMath;
import net.sorukoma.quasium.mind.MindRegistry;
import net.sorukoma.quasium.progression.ConfigArgs;
import net.sorukoma.quasium.progression.ProgressionArticle;
import net.sorukoma.quasium.progression.ProgressionCategory;
import net.sorukoma.quasium.progression.ResearchNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public final class ResearchRules {
    public static final String LIVED = "overdose_2";
    public static final String DREAMT = "dreamt";
    public static final String MIRROR_SEEN = "mirror_seen";

    public record Cost(int treatises, int insight, Map<NoteType, Integer> notes) {
    }

    public record Requirement(Component text, boolean met) {
    }

    private ResearchRules() {
    }

    public static boolean priced(ResearchNode node) {
        return node.insight() != null;
    }

    public static Cost cost(ResearchNode node, MindView view) {
        boolean fixed = priced(node);
        int treatises = node.treatises() != null ? node.treatises() : MindConfig.tier(MindConfig.TIER_TREATISES, node.tier());
        int insight = node.insight() != null ? node.insight() : MindConfig.tier(MindConfig.TIER_INSIGHT, node.tier());
        int noteMl = node.noteMl() != null ? node.noteMl() : MindConfig.tier(MindConfig.TIER_NOTE_ML, node.tier());
        if (!fixed && view.tea()) insight = discounted(insight);
        if (!fixed && view.hookah()) noteMl = discounted(noteMl);
        Map<NoteType, Integer> notes = new LinkedHashMap<>();
        for (String id : node.notes()) {
            NoteType note = NoteType.byId(id);
            if (note != null && noteMl > 0) notes.put(note, noteMl);
        }
        return new Cost(treatises, insight, notes);
    }

    private static int discounted(int amount) {
        return MindMath.discounted(amount, ConfigArgs.synced(MindConfig.SCHOOL_DISCOUNT_PCT));
    }

    public static boolean needsSchool(ResearchNode node, MindView view) {
        return !priced(node) && node.tier() >= ConfigArgs.synced(MindConfig.SCHOOL_TIER) && view.school().isEmpty();
    }

    public static boolean mirror(ProgressionArticle article) {
        return ProgressionCategory.RESEARCH_MIRROR.id().equals(article.getCategory());
    }

    public static boolean lived(MindView view) {
        return view.flag(LIVED) || view.flag(DREAMT) || view.kaif() >= ConfigArgs.synced(MindConfig.LIVED_KAIF);
    }

    public static List<Requirement> requirements(ProgressionArticle article, MindView view, Function<String, Component> titles) {
        ResearchNode node = article.getResearch();
        List<Requirement> list = new ArrayList<>();
        for (String required : node.requires()) {
            list.add(new Requirement(Component.translatable("quasium.research.need.node", titles.apply(required)), view.researched(required)));
        }
        if (node.kaif() != null && !view.tea()) {
            list.add(new Requirement(Component.translatable("quasium.research.need.kaif", node.kaif(), view.kaif()), view.kaif() >= node.kaif()));
        }
        if (mirror(article)) {
            int limit = ConfigArgs.synced(MindConfig.PERCEPTION_MIRROR);
            list.add(new Requirement(Component.translatable("quasium.research.need.mirror", limit, view.kaif()), view.kaif() <= limit));
        }
        if (node.lived()) {
            list.add(new Requirement(Component.translatable("quasium.research.need.lived", ConfigArgs.synced(MindConfig.LIVED_KAIF)), lived(view)));
        }
        if (node.dream() != null) {
            list.add(new Requirement(Component.translatable("quasium.research.need.dream",
                    Component.translatable("quasium.dream." + node.dream())), view.flag(DREAMT + ":" + node.dream())));
        }
        Cost cost = cost(node, view);
        Inventory inventory = view.player().getInventory();
        if (cost.treatises() > 0) {
            int have = inventory.countItem(MindRegistry.TREATISE.get());
            list.add(new Requirement(Component.translatable("quasium.research.need.treatises", have, cost.treatises()), have >= cost.treatises()));
        }
        if (cost.insight() > 0) {
            list.add(new Requirement(Component.translatable("quasium.research.need.insight", view.insight(), cost.insight()),
                    view.insight() >= cost.insight()));
        }
        cost.notes().forEach((note, ml) -> {
            int have = noteMl(inventory, note);
            list.add(new Requirement(Component.translatable("quasium.research.need.note", note.displayName(), have, ml), have >= ml));
        });
        for (String spec : node.items()) {
            stack(spec).ifPresent(wanted -> {
                int have = inventory.countItem(wanted.getItem());
                list.add(new Requirement(Component.translatable("quasium.research.need.item", wanted.getHoverName(), have, wanted.getCount()),
                        have >= wanted.getCount()));
            });
        }
        return list;
    }

    public static Optional<ItemStack> stack(String spec) {
        int star = spec.indexOf('*');
        String count = star < 0 ? "1" : spec.substring(star + 1);
        ResourceLocation id = ResourceLocation.tryParse(star < 0 ? spec : spec.substring(0, star));
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id) || count.isEmpty() || count.length() > 2
                || !count.chars().allMatch(Character::isDigit)) {
            return Optional.empty();
        }
        return Optional.of(new ItemStack(BuiltInRegistries.ITEM.get(id), Integer.parseInt(count)));
    }

    public static boolean available(ResearchNode node, Predicate<ResourceLocation> recipeExists) {
        if (!node.items().stream().allMatch(spec -> stack(spec).isPresent()) || !node.gives().stream().allMatch(spec -> stack(spec).isPresent())) {
            return false;
        }
        if (node.perk() != null || !node.gives().isEmpty() || node.spells().isEmpty() && node.recipes().isEmpty()) return true;
        for (String spell : node.spells()) {
            ResourceLocation id = ResourceLocation.tryParse(spell);
            if (id != null && Spells.get(id) != null) return true;
        }
        for (String recipe : node.recipes()) {
            ResourceLocation id = ResourceLocation.tryParse(recipe);
            if (id != null && recipeExists.test(id)) return true;
        }
        return false;
    }

    public static int noteMl(Inventory inventory, NoteType note) {
        int total = 0;
        for (ItemStack stack : inventory.items) {
            if (note.id().equals(EssenceBottleItem.typeOf(stack))) total += EssenceBottleItem.amountOf(stack);
        }
        return total;
    }

    static void drainNote(Inventory inventory, NoteType note, int ml) {
        int left = ml;
        for (int slot = 0; slot < inventory.items.size() && left > 0; slot++) {
            ItemStack stack = inventory.items.get(slot);
            if (!note.id().equals(EssenceBottleItem.typeOf(stack))) continue;
            int amount = EssenceBottleItem.amountOf(stack);
            int taken = Math.min(amount, left);
            left -= taken;
            inventory.items.set(slot, EssenceBottleItem.drained(stack, taken));
        }
    }
}
