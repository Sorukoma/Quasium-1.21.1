package net.sorukoma.quasium.magic;

import com.mojang.logging.LogUtils;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.sorukoma.quasium.Config;
import net.sorukoma.quasium.item.ModItemTags;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Predicate;

public final class NoteDerivation {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_RECIPE_PASSES = 32;

    private enum RuleStage { STRONG, RAW_MATERIALS, EVERYTHING_LEFT }

    public record Tuning(int recipePct, int cap, int totalCap, int cookingEmber) {
        public static Tuning fromConfig() {
            return new Tuning(Config.DERIVED_RECIPE_PCT.getAsInt(), Config.DERIVED_NOTE_CAP.getAsInt(),
                    Config.DERIVED_TOTAL_CAP.getAsInt(), Config.DERIVED_COOKING_EMBER.getAsInt());
        }

        public Map<NoteType, Integer> limit(Map<NoteType, Integer> notes) {
            return NoteMix.limit(notes, cap, totalCap);
        }
    }

    private final List<NoteRule> rules;
    private final Tuning tuning;
    private final RecipeNotes recipes;
    private final Map<Item, Map<NoteType, Integer>> known = new HashMap<>();
    private final Map<EntityType<?>, Map<NoteType, Integer>> mobs = new HashMap<>();
    private final Map<ResourceLocation, NoteProfile> derived = new TreeMap<>();
    private final List<Item> pending = new ArrayList<>();

    private NoteDerivation(Collection<RecipeHolder<?>> recipes, HolderLookup.Provider registries, List<NoteRule> rules, Tuning tuning) {
        this.rules = rules;
        this.tuning = tuning;
        this.recipes = new RecipeNotes(recipes, registries, tuning);
    }

    public static void refresh(MinecraftServer server) {
        Map<ResourceLocation, NoteProfile> derived = derive(BuiltInRegistries.ITEM, livingMobs(), server.getRecipeManager().getRecipes(),
                server.registryAccess(), NoteRules.all(), Tuning.fromConfig());
        DerivedNotes.set(derived);
        LOGGER.info("Derived {} note profiles from {} rules", derived.size(), NoteRules.all().size());
    }

    public static List<EntityType<?>> livingMobs() {
        return BuiltInRegistries.ENTITY_TYPE.stream().filter(DefaultAttributes::hasSupplier).toList();
    }

    public static Map<ResourceLocation, NoteProfile> derive(Iterable<Item> items, Iterable<EntityType<?>> mobTypes,
                                                            Collection<RecipeHolder<?>> recipes, HolderLookup.Provider registries,
                                                            List<NoteRule> rules, Tuning tuning) {
        NoteDerivation run = new NoteDerivation(recipes, registries, rules, tuning);
        mobTypes.forEach(run::deriveMob);
        items.forEach(run::classify);
        run.pending.sort(Comparator.comparing(BuiltInRegistries.ITEM::getKey));
        run.deriveItems();
        return run.derived;
    }

    private void deriveMob(EntityType<?> type) {
        Map.Entry<ResourceLocation, NoteProfile> explicit = NoteProfiles.explicitFor(type);
        if (explicit != null) {
            mobs.put(type, explicit.getValue().notes());
            return;
        }
        NoteRule rule = firstRule(candidate -> candidate.forMobs() && candidate.matches(type));
        if (rule == null) return;
        Map<NoteType, Integer> notes = tuning.limit(rule.notes());
        mobs.put(type, notes);
        derived.put(DerivedNotes.idOf(type), new NoteProfile(Optional.empty(), Optional.empty(), Optional.of(type), notes));
    }

    private void classify(Item item) {
        if (item == Items.AIR || item.builtInRegistryHolder().is(ModItemTags.NOTES_IGNORED)) return;
        Map.Entry<ResourceLocation, NoteProfile> explicit = NoteProfiles.explicitFor(new ItemStack(item));
        if (explicit != null) {
            known.put(item, explicit.getValue().notes());
        } else {
            pending.add(item);
        }
    }

    private void deriveItems() {
        pending.forEach(this::hatchFromEgg);
        applyRules(RuleStage.STRONG);
        recipePasses();
        applyRules(RuleStage.RAW_MATERIALS);
        recipePasses();
        applyRules(RuleStage.EVERYTHING_LEFT);
    }

    private void hatchFromEgg(Item item) {
        if (!(item instanceof SpawnEggItem egg)) return;
        Map<NoteType, Integer> notes = mobs.get(egg.getType(new ItemStack(item)));
        if (notes != null) accept(item, tuning.limit(NoteMix.weaken(notes, tuning.recipePct())));
    }

    private void applyRules(RuleStage stage) {
        for (Item item : pending) {
            if (known.containsKey(item) || stage == RuleStage.RAW_MATERIALS && recipes.produces(item)) continue;
            NoteRule rule = firstRule(candidate -> !candidate.forMobs() && (stage != RuleStage.STRONG || !candidate.fallback())
                    && candidate.matches(item));
            if (rule != null) accept(item, tuning.limit(rule.notes()));
        }
        pending.removeIf(known::containsKey);
    }

    private void recipePasses() {
        for (int pass = 0; pass < MAX_RECIPE_PASSES; pass++) {
            boolean grew = false;
            for (Item item : pending) {
                if (known.containsKey(item)) continue;
                Map<NoteType, Integer> notes = recipes.mix(item, known);
                if (notes == null) continue;
                accept(item, notes);
                grew = true;
            }
            pending.removeIf(known::containsKey);
            if (!grew) return;
        }
    }

    private void accept(Item item, Map<NoteType, Integer> notes) {
        if (notes.isEmpty() || known.containsKey(item)) return;
        known.put(item, notes);
        derived.put(DerivedNotes.idOf(item), new NoteProfile(Optional.of(item), Optional.empty(), Optional.empty(), notes));
    }

    @Nullable
    private NoteRule firstRule(Predicate<NoteRule> fits) {
        for (NoteRule rule : rules) {
            if (fits.test(rule)) return rule;
        }
        return null;
    }
}
