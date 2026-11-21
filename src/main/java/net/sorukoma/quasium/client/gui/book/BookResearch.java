package net.sorukoma.quasium.client.gui.book;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.sorukoma.quasium.client.ClientKaifData;
import net.sorukoma.quasium.client.ClientMagicData;
import net.sorukoma.quasium.client.ClientProgressionData;
import net.sorukoma.quasium.mind.ClientMind;
import net.sorukoma.quasium.mind.MindConfig;
import net.sorukoma.quasium.mind.MindData;
import net.sorukoma.quasium.mind.MindPackets;
import net.sorukoma.quasium.mind.research.MindView;
import net.sorukoma.quasium.mind.research.ResearchRules;
import net.sorukoma.quasium.progression.ConfigArgs;
import net.sorukoma.quasium.progression.ProgressionArticle;
import net.sorukoma.quasium.progression.ProgressionCategory;

import java.util.ArrayList;
import java.util.List;

public final class BookResearch {
    public static final int BUTTON_H = 16;
    private static final String[] TIERS = {"I", "II", "III", "IV"};

    private final Button study;
    private final Button hookah;
    private final Button tea;
    private ProgressionArticle shown;
    private int center;

    public BookResearch() {
        study = Button.builder(Component.translatable("quasium.research.study"), button -> send("research", shown.getId().toString()))
                .bounds(0, 0, 60, BUTTON_H).build();
        hookah = Button.builder(Component.translatable("quasium.research.school.hookah"), button -> send("school", MindView.HOOKAH))
                .bounds(0, 0, 60, BUTTON_H).build();
        tea = Button.builder(Component.translatable("quasium.research.school.tea"), button -> send("school", MindView.TEA))
                .bounds(0, 0, 60, BUTTON_H).build();
    }

    public List<Button> buttons() {
        return List.of(study, hookah, tea);
    }

    public void place(int x, int y, int width) {
        int half = (width - 4) / 2;
        center = x + width / 2;
        study.setPosition(center - study.getWidth() / 2, y);
        hookah.setRectangle(half, BUTTON_H, x, y);
        tea.setRectangle(half, BUTTON_H, x + half + 4, y);
    }

    public void show(ProgressionArticle article, boolean lastPage, Font font) {
        shown = article;
        boolean open = article != null && article.isResearch() && !ClientMind.researched(article.getId()) && lastPage;
        boolean choose = open && ResearchRules.needsSchool(article.getResearch(), view());
        hookah.visible = choose;
        tea.visible = choose;
        study.visible = open && !choose;
        if (!study.visible) return;
        study.setWidth(font.width(study.getMessage()) + 16);
        study.setX(center - study.getWidth() / 2);
        study.active = ResearchRules.requirements(article, view(), BookResearch::title).stream().allMatch(ResearchRules.Requirement::met);
    }

    public static boolean pending(ProgressionArticle article) {
        return article != null && article.isResearch() && !ClientMind.researched(article.getId());
    }

    public static boolean visible(ProgressionArticle article) {
        if (!article.isResearch()) return true;
        if (ResearchRules.mirror(article) && !ClientMind.flag(ResearchRules.MIRROR_SEEN)) return false;
        return ResearchRules.available(article.getResearch(), BookResearch::recipeExists);
    }

    public static boolean tabVisible(ProgressionCategory category) {
        return category != ProgressionCategory.RESEARCH_MIRROR || ClientMind.flag(ResearchRules.MIRROR_SEEN);
    }

    public static Component listTitle(ProgressionArticle article) {
        Component title = Component.translatable(article.getTitle());
        return article.isResearch() ? Component.literal(TIERS[article.getResearch().tier() - 1] + " ").append(title) : title;
    }

    public static List<BookPiece> requirements(ProgressionArticle article, Font font, int width) {
        List<BookPiece> pieces = new ArrayList<>();
        MindView view = view();
        lines(font, Component.translatable("quasium.research.tier", TIERS[article.getResearch().tier() - 1]), BookArt.palette().accent(), width, pieces);
        if (ResearchRules.needsSchool(article.getResearch(), view)) {
            lines(font, Component.translatable("quasium.research.school_hint", ConfigArgs.synced(MindConfig.SCHOOL_DISCOUNT_PCT)), BookArt.palette().body(),
                    width, pieces);
            return pieces;
        }
        if (!view.school().isEmpty() && !ResearchRules.priced(article.getResearch())) {
            lines(font, Component.translatable("quasium.research.school_bonus." + view.school(),
                    ConfigArgs.synced(MindConfig.SCHOOL_DISCOUNT_PCT)), BookArt.palette().muted(), width, pieces);
        }
        for (ResearchRules.Requirement requirement : ResearchRules.requirements(article, view, BookResearch::title)) {
            lines(font, requirement.text(), requirement.met() ? BookArt.palette().met() : BookArt.palette().unmet(), width, pieces);
        }
        return pieces;
    }

    private static void lines(Font font, Component text, int color, int width, List<BookPiece> pieces) {
        font.split(text, width).forEach(line -> pieces.add(new BookPiece.Line(line, color)));
    }

    private static MindView view() {
        return new MindView(Minecraft.getInstance().player, ClientKaifData.getKaifValue(), ClientMagicData.insight(),
                ClientMind.strings(MindData.RESEARCH), ClientMind.school(), ClientMind.strings(MindData.FLAGS));
    }

    private static Component title(String id) {
        ResourceLocation location = ResourceLocation.tryParse(id);
        ProgressionArticle article = location == null ? null : ClientProgressionData.getArticles().get(location);
        return article == null ? Component.literal(id) : Component.translatable(article.getTitle());
    }

    private static boolean recipeExists(ResourceLocation id) {
        return Minecraft.getInstance().getConnection() != null && Minecraft.getInstance().getConnection().getRecipeManager().byKey(id).isPresent();
    }

    private static void send(String action, String argument) {
        MindPackets.act(action, argument);
    }
}
