package net.sorukoma.quasium.client.gui.book;

import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.sorukoma.quasium.client.ClientHybrids;
import net.sorukoma.quasium.client.ClientOreLuckData;
import net.sorukoma.quasium.client.ClientTobaccoData;
import net.sorukoma.quasium.magic.SmokeTricks;
import net.sorukoma.quasium.mind.ClientMind;
import net.sorukoma.quasium.progression.ConfigArgs;
import net.sorukoma.quasium.progression.ProgressionArticle;
import net.sorukoma.quasium.progression.ProgressionPage;
import net.sorukoma.quasium.recipe.AnvilRecipes;
import net.sorukoma.quasium.ritual.RitualRecipe;
import net.sorukoma.quasium.ritual.RitualRecipes;
import net.sorukoma.quasium.ritual.RitualSummary;
import net.sorukoma.quasium.selection.genetics.HybridCollection;
import net.sorukoma.quasium.selection.genetics.Lineage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class BookContent {
    private static final BookPiece GAP = new BookPiece.Gap();
    private static final int MAX_ICONS = 8;

    private BookContent() {
    }

    public static List<BookPiece> unlocked(ProgressionArticle article, Font font, int width) {
        List<BookPiece> pieces = new ArrayList<>();
        for (ProgressionPage page : article.getPages()) {
            if (page.getText() != null) lines(pieces, font, Component.translatable(page.getText(), args(page)), width, BookArt.palette().body());
            block(page).ifPresent(pieces::add);
            ritual(pieces, font, page, width);
            pieces.add(GAP);
        }
        personal(article).ifPresent(text -> lines(pieces, font, text, width, BookArt.palette().accent()));
        return pieces;
    }

    public static List<BookPiece> locked(ProgressionArticle article, Optional<DisplayInfo> unlock, Font font, int width) {
        List<BookPiece> pieces = new ArrayList<>();
        pieces.add(new BookPiece.Notice(Component.translatable(article.isResearch() ? "quasium.research.locked" : "quasium.progression.locked")));
        if (article.getHint() != null) lines(pieces, font, Component.translatable(article.getHint()), width, BookArt.palette().muted());
        if (article.isResearch()) {
            pieces.add(GAP);
            pieces.addAll(BookResearch.requirements(article, font, width));
            return pieces;
        }
        unlock.ifPresent(display -> {
            pieces.add(GAP);
            lines(pieces, font, Component.translatable("quasium.progression.unlock_by", display.getTitle()), width, BookArt.palette().accent());
            lines(pieces, font, display.getDescription(), width, BookArt.palette().muted());
        });
        return pieces;
    }

    private static void lines(List<BookPiece> pieces, Font font, Component text, int width, int color) {
        font.split(text, width).forEach(line -> pieces.add(new BookPiece.Line(line, color)));
    }

    private static Optional<BookPiece> block(ProgressionPage page) {
        if (page.getRecipe() != null) {
            ResourceLocation id = ResourceLocation.tryParse(page.getRecipe());
            if (id == null || Minecraft.getInstance().getConnection() == null) return Optional.empty();
            return Minecraft.getInstance().getConnection().getRecipeManager().byKey(id)
                    .filter(holder -> BookRecipes.supports(holder.value()))
                    .map(BookPiece.Recipe::new);
        }
        if (page.getAnvil() != null) {
            Item output = item(page.getAnvil());
            return AnvilRecipes.getRecipes().stream().filter(recipe -> recipe.output().get() == output).findFirst().map(BookPiece.Anvil::new);
        }
        List<ItemStack> stacks = page.getItems().stream().map(BookContent::item).filter(item -> item != Items.AIR)
                .limit(MAX_ICONS).map(ItemStack::new).toList();
        return stacks.isEmpty() ? Optional.empty() : Optional.of(new BookPiece.Items(stacks));
    }

    private static void ritual(List<BookPiece> pieces, Font font, ProgressionPage page, int width) {
        RitualRecipe recipe = page.getRitual() == null ? null : RitualRecipes.get(ResourceLocation.tryParse(page.getRitual()));
        if (recipe == null) return;
        pieces.add(new BookPiece.Ritual(recipe));
        RitualSummary.lines(recipe).forEach(line -> lines(pieces, font, line, width, BookArt.palette().muted()));
    }

    private static Item item(String id) {
        ResourceLocation location = ResourceLocation.tryParse(id);
        return location == null ? Items.AIR : BuiltInRegistries.ITEM.get(location);
    }

    private static Object[] args(ProgressionPage page) {
        return page.getArgs().stream().map(ConfigArgs::format).toArray();
    }

    private static Optional<Component> personal(ProgressionArticle article) {
        return switch (article.getId().getPath()) {
            case "regular_tobacco" -> preferences();
            case "hybrids" -> hybrids();
            case "quasium_ore" -> ClientOreLuckData.getAchievedFortuneLevels().stream().max(Integer::compare)
                    .map(best -> Component.translatable("quasium.progression.quasium_ore.luck", best, best + 1));
            case "smoke_rings" -> Optional.of(tricks());
            default -> Optional.empty();
        };
    }

    private static Component tricks() {
        List<Component> names = Arrays.stream(SmokeTricks.Trick.values()).<Component>map(trick -> ClientMind.flag(trick.id())
                ? Component.translatable("quasium.trick." + trick.key()) : Component.literal("???")).toList();
        return Component.translatable("quasium.progression.smoke_rings.tricks", ComponentUtils.formatList(names, Component.literal(", ")));
    }

    private static Optional<Component> preferences() {
        Map<String, Boolean> tasted = ClientTobaccoData.getFlavorPreferences();
        if (tasted.isEmpty()) return Optional.empty();
        return Optional.of(Component.translatable("quasium.progression.regular_tobacco.preferences", flavors(tasted, true), flavors(tasted, false)));
    }

    private static Optional<Component> hybrids() {
        Set<String> known = ClientHybrids.known();
        if (known.isEmpty()) return Optional.empty();
        MutableComponent text = Component.translatable("quasium.progression.hybrids.collection");
        known.stream().sorted().forEach(key -> text.append("\n").append(Lineage.displayName(List.of(key.split("\\+")))));
        List<String> pairs = HybridCollection.wildPairs();
        if (pairs.isEmpty()) return Optional.of(text);
        long found = pairs.stream().filter(known::contains).count();
        return Optional.of(text.append("\n").append(Component.translatable("quasium.progression.hybrids.pairs", found, pairs.size())));
    }

    private static Component flavors(Map<String, Boolean> tasted, boolean liked) {
        List<Component> names = tasted.entrySet().stream().filter(entry -> entry.getValue() == liked).map(Map.Entry::getKey).sorted()
                .<Component>map(flavor -> Component.translatable("quasium.flavor." + flavor)).toList();
        return names.isEmpty() ? Component.translatable("quasium.progression.none") : ComponentUtils.formatList(names, Component.literal(", "));
    }
}
