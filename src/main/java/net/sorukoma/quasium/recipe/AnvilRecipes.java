package net.sorukoma.quasium.recipe;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredItem;
import net.sorukoma.quasium.Config;
import net.sorukoma.quasium.block.ModBlocks;
import net.sorukoma.quasium.data.PlayerKaifData;
import net.sorukoma.quasium.item.EssenceBottleItem;
import net.sorukoma.quasium.item.ModItems;
import net.sorukoma.quasium.magic.NoteType;
import net.sorukoma.quasium.mind.MindData;
import net.sorukoma.quasium.progression.ConfigArgs;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.IntSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class AnvilRecipes {

    public static final int ARMOR_KAIF = 70;
    private static final List<AnvilRecipe> recipes = new ArrayList<>();
    private static boolean initialized = false;

    public record AnvilRecipe(List<Supplier<Item>> inputs, DeferredItem<Item> output, IntSupplier hits, int kaifLevel,
                              Predicate<List<ItemStack>> check, @Nullable IntSupplier maxKaif, @Nullable ResourceLocation research,
                              List<Integer> counts) {
        public AnvilRecipe(List<Supplier<Item>> inputs, DeferredItem<Item> output, int hitsRequired, int kaifLevel) {
            this(inputs, output, hitsRequired, kaifLevel, slots -> true);
        }

        public AnvilRecipe(List<Supplier<Item>> inputs, DeferredItem<Item> output, int hitsRequired, int kaifLevel,
                           Predicate<List<ItemStack>> check) {
            this(inputs, output, () -> hitsRequired, kaifLevel, check, null, null, Collections.nCopies(inputs.size(), 1));
        }

        public int hitsRequired() {
            return hits.getAsInt();
        }

        public boolean studiedBy(Player smith) {
            return research == null || MindData.has(smith, MindData.RESEARCH, research.toString());
        }

        public boolean allows(int kaif) {
            return maxKaif == null ? kaif >= kaifLevel : kaif <= maxKaif.getAsInt();
        }

        public Component kaifRule() {
            return maxKaif == null ? Component.translatable("quasium.jei.anvil.kaif_required", kaifLevel)
                    : Component.translatable("quasium.jei.anvil.kaif_at_most", maxKaif.getAsInt());
        }

        public Component refusal() {
            return maxKaif == null ? Component.translatable("block.quasium.quasium_anvil.low_kaif", kaifLevel)
                    : Component.translatable("block.quasium.quasium_anvil.high_kaif", maxKaif.getAsInt());
        }

        public Component bookInfo() {
            return maxKaif == null ? Component.translatable("quasium.progression.anvil_info", hitsRequired(), kaifLevel)
                    : Component.translatable("quasium.progression.anvil_info_sober", hitsRequired(), maxKaif.getAsInt());
        }

        public List<ItemStack> shownIn(int slot) {
            Item item = inputs.get(slot).get();
            if (item != ModItems.ESSENCE_BOTTLE.get()) return List.of(new ItemStack(item, counts.get(slot)));
            return essenceBottles().stream().filter(bottle -> check.test(slotsWith(slot, bottle))).toList();
        }

        private List<ItemStack> slotsWith(int slot, ItemStack bottle) {
            List<ItemStack> slots = new ArrayList<>();
            for (int i = 0; i < inputs.size(); i++) slots.add(i == slot ? bottle : new ItemStack(inputs.get(i).get()));
            return slots;
        }
    }

    public static Predicate<List<ItemStack>> noteBottleIn(NoteType note, int slot) {
        return slots -> {
            ItemStack bottle = slots.get(slot);
            return NoteType.byId(EssenceBottleItem.typeOf(bottle)) == note
                    && EssenceBottleItem.amountOf(bottle) >= ConfigArgs.synced(Config.NOTE_BOTTLE_CAPACITY);
        };
    }

    private static List<ItemStack> essenceBottles() {
        List<ItemStack> bottles = new ArrayList<>();
        EssenceRecipes.createRecipes().forEach(recipe -> bottles.add(recipe.output()));
        for (NoteType note : NoteType.values()) bottles.add(EssenceBottleItem.of(note.id(), ConfigArgs.synced(Config.NOTE_BOTTLE_CAPACITY)));
        return bottles;
    }

    public static void init() {
        if (initialized) return;
        initialized = true;

        // Backpack Hookah = Hookah Block + Leather, 5 hits, kaif 50
        add(
                List.of(() -> ModBlocks.HOOKAH_BLOCK_ITEM.get(), () -> net.minecraft.world.item.Items.LEATHER),
                ModItems.BACKPACK_HOOKAH,
                5,
                50
        );
        add(List.of(() -> net.minecraft.world.item.Items.GLASS_BOTTLE, () -> ModItems.ESSENCE_BOTTLE.get()), ModItems.EMPTY_SMOKE_JAR, 3, 30);
        addStack(ModItems.QUASIUMINGOT, 5, ModItems.QUASIUM_HELMET, 5, ARMOR_KAIF);
        addStack(ModItems.QUASIUMINGOT, 8, ModItems.QUASIUM_CHESTPLATE, 8, ARMOR_KAIF);
        addStack(ModItems.QUASIUMINGOT, 7, ModItems.QUASIUM_LEGGINGS, 7, ARMOR_KAIF);
        addStack(ModItems.QUASIUMINGOT, 4, ModItems.QUASIUM_BOOTS, 4, ARMOR_KAIF);
    }

    public static void addStack(Supplier<Item> input, int count, DeferredItem<Item> output, int hitsRequired, int kaifLevel) {
        recipes.add(new AnvilRecipe(List.of(input), output, () -> hitsRequired, kaifLevel, slots -> true, null, null, List.of(count)));
    }

    public static void add(List<Supplier<Item>> inputs, DeferredItem<Item> output, int hitsRequired, int kaifLevel) {
        recipes.add(new AnvilRecipe(inputs, output, hitsRequired, kaifLevel));
    }

    public static void add(List<Supplier<Item>> inputs, DeferredItem<Item> output, int hitsRequired, int kaifLevel,
                           Predicate<List<ItemStack>> check) {
        recipes.add(new AnvilRecipe(inputs, output, hitsRequired, kaifLevel, check));
    }

    public static void addSober(List<Supplier<Item>> inputs, DeferredItem<Item> output, IntSupplier hits,
                                Predicate<List<ItemStack>> check, IntSupplier maxKaif, ResourceLocation research) {
        recipes.add(new AnvilRecipe(inputs, output, hits, PlayerKaifData.MIN_KAIF, check, maxKaif, research,
                Collections.nCopies(inputs.size(), 1)));
    }

    /**
     * Find a matching recipe for the given slot contents.
     * Slot order matters — inputs must match in sequence.
     * Empty trailing slots are allowed.
     */
    public static AnvilRecipe find(List<ItemStack> slotContents) {
        for (AnvilRecipe recipe : recipes) {
            boolean matches = true;
            for (int i = 0; i < recipe.inputs().size(); i++) {
                ItemStack slotStack = i < slotContents.size() ? slotContents.get(i) : ItemStack.EMPTY;
                if (slotStack.isEmpty() || !slotStack.is(recipe.inputs().get(i).get()) || slotStack.getCount() != recipe.counts().get(i)) {
                    matches = false;
                    break;
                }
            }
            // Remaining slots must be empty
            for (int i = recipe.inputs().size(); i < slotContents.size(); i++) {
                if (!slotContents.get(i).isEmpty()) {
                    matches = false;
                    break;
                }
            }
            if (matches && recipe.check().test(slotContents)) return recipe;
        }
        return null;
    }

    public static List<AnvilRecipe> getRecipes() {
        return Collections.unmodifiableList(recipes);
    }
}
