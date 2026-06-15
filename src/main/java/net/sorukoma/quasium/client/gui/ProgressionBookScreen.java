package net.sorukoma.quasium.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.progression.ProgressionArticle;
import net.sorukoma.quasium.progression.ProgressionPage;
import net.sorukoma.quasium.client.ClientTobaccoData;
import net.sorukoma.quasium.client.ClientOreLuckData;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.client.renderer.GameRenderer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.HashSet;

public class ProgressionBookScreen extends Screen {

    private List<ProgressionArticle> articles = new ArrayList<>();
    private ProgressionArticle selectedArticle;
    private int currentPage = 0;

    private static final int BOOK_WIDTH = 256;
    private static final int BOOK_HEIGHT = 192;
    private static final int ARTICLE_LIST_AREA_WIDTH = 100;
    private static final int ARTICLE_BUTTON_HEIGHT = 20;
    private static final int NAV_BUTTON_SIZE = 20;

    private Button nextPageButton;
    private Button prevPageButton;
    private List<Button> articleButtons = new ArrayList<>();

    private ProgressionAdvancementListener advancementListener;

    private static final LinkedHashSet<ProgressionArticle> recentlyOpenedArticles = new LinkedHashSet<>();

    private static final String WELCOME_ARTICLE_TITLE = "quasium.progression.introduction.title";
    private static final String HOOKAH_ARTICLE_TITLE = "quasium.progression.hookah.title";
    private static final String ORE_ARTICLE_TITLE = "quasium.progression.quasium_ore.title";

    public ProgressionBookScreen() {
        super(Component.translatable("gui.quasium.progression_book.title"));
        this.minecraft = Minecraft.getInstance();
        this.advancementListener = new ProgressionAdvancementListener();
    }

    private boolean isArticleUnlocked(ProgressionArticle article) {
        if (article.getUnlockCondition() == null || article.getUnlockCondition().isEmpty()) {
            return true;
        }

        if (Minecraft.getInstance().player != null && Minecraft.getInstance().getConnection() != null) {
            ResourceLocation advancementId = ResourceLocation.parse(article.getUnlockCondition());
            ClientAdvancements clientAdvancements = Minecraft.getInstance().getConnection().getAdvancements();
            AdvancementHolder advancementHolder = clientAdvancements.get(advancementId);

            if (advancementHolder != null) {
                AdvancementProgress progress = advancementListener.getProgress(advancementHolder);
                return progress != null && progress.isDone();
            }
        }
        return false;
    }

    private List<ProgressionPage> getEffectivePages(ProgressionArticle article) {
        List<ProgressionPage> pages = new ArrayList<>(article.getPages());
        if (article.getTitle().equals(HOOKAH_ARTICLE_TITLE)) {
            for (String flavor : ClientTobaccoData.getTriedFlavors()) {
                String flavorKey = getFlavorTranslationKey(flavor);
                String effectKey = getEffectTranslationKey(flavor);
                ProgressionPage page = new ProgressionPage();
                page.setText("quasium.progression.hookah.flavor_page:" + flavorKey + ":" + effectKey);
                pages.add(page);
            }
            for (Float strength : ClientTobaccoData.getTriedSelfMixedStrengths()) {
                ProgressionPage page = new ProgressionPage();
                page.setText("quasium.progression.hookah.self_mixed_page:" + String.format("%.1f", strength));
                pages.add(page);
            }
        }
        if (article.getTitle().equals(ORE_ARTICLE_TITLE)) {
            for (Integer fortuneLevel : ClientOreLuckData.getAchievedFortuneLevels()) {
                ProgressionPage page = new ProgressionPage();
                page.setText(getOreFortunePageText(fortuneLevel));
                pages.add(page);
            }
        }
        return pages;
    }

    private static String getFlavorTranslationKey(String flavor) {
        return "quasium.flavor." + flavor;
    }

    private static String getEffectTranslationKey(String flavor) {
        return "quasium.progression.hookah.effect." + flavor;
    }

    private static String getOreFortunePageText(int fortuneLevel) {
        return "quasium.progression.quasium_ore.fortune." + fortuneLevel;
    }

    private static Component buildPageComponent(String pageText) {
        if (pageText.startsWith("quasium.progression.hookah.flavor_page:")) {
            String[] parts = pageText.split(":", 3);
            if (parts.length == 3) {
                Component flavorName = Component.translatable(parts[1]);
                Component effectDesc = Component.translatable(parts[2]);
                return Component.empty().append(flavorName).append(": ").append(effectDesc);
            }
        }
        if (pageText.startsWith("quasium.progression.hookah.self_mixed_page:")) {
            String strengthStr = pageText.substring("quasium.progression.hookah.self_mixed_page:".length());
            float strength = Float.parseFloat(strengthStr);
            float damage = strength * 8;
            String damageStr;
            if (damage == (int) damage) {
                damageStr = String.valueOf((int) damage);
            } else {
                damageStr = String.format("%.1f", damage);
            }
            return Component.translatable("quasium.progression.hookah.self_mixed", strengthStr, damageStr);
        }
        if (pageText.startsWith("quasium.progression.quasium_ore.fortune.")) {
            return Component.translatable(pageText);
        }
        if (pageText.startsWith("quasium.progression.")) {
            return Component.translatable(pageText);
        }
        return Component.literal(pageText);
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();

        if (Minecraft.getInstance().getConnection() != null) {
            Minecraft.getInstance().getConnection().getAdvancements().setListener(this.advancementListener);
        }

        List<ProgressionArticle> allAvailableArticles = new ArrayList<>();
        if (QuasiumMod.getProgressionArticleManager() != null) {
            allAvailableArticles = QuasiumMod.getProgressionArticleManager().getArticles().values().stream()
                    .filter(this::isArticleUnlocked)
                    .collect(Collectors.toList());
        }

        this.articles.clear();
        Set<ProgressionArticle> addedToSortedList = new HashSet<>();

        ProgressionArticle welcomeArticle = allAvailableArticles.stream()
                .filter(article -> article.getTitle().equals(WELCOME_ARTICLE_TITLE))
                .findFirst()
                .orElse(null);

        if (welcomeArticle != null) {
            this.articles.add(welcomeArticle);
            addedToSortedList.add(welcomeArticle);
        }

        List<ProgressionArticle> unopenedArticles = new ArrayList<>();
        List<ProgressionArticle> openedArticles = new ArrayList<>();

        for (ProgressionArticle article : allAvailableArticles) {
            if (!addedToSortedList.contains(article)) {
                if (recentlyOpenedArticles.contains(article)) {
                    openedArticles.add(article);
                } else {
                    unopenedArticles.add(article);
                }
            }
        }

        unopenedArticles.sort((a1, a2) -> a1.getTitle().compareTo(a2.getTitle()));
        this.articles.addAll(unopenedArticles);
        addedToSortedList.addAll(unopenedArticles);

        List<ProgressionArticle> tempRecentlyOpened = new ArrayList<>(recentlyOpenedArticles);
        for (int i = tempRecentlyOpened.size() - 1; i >= 0; i--) {
            ProgressionArticle article = tempRecentlyOpened.get(i);
            if (openedArticles.contains(article)) {
                this.articles.add(article);
                addedToSortedList.add(article);
            }
        }
        this.articles = this.articles.stream().distinct().collect(Collectors.toList());

        if (!articles.isEmpty()) {
            if (selectedArticle == null || !articles.contains(selectedArticle)) {
                selectedArticle = articles.get(0);
            }
        } else {
            selectedArticle = null;
        }
        currentPage = 0;

        int startX = (this.width - BOOK_WIDTH) / 2;
        int startY = (this.height - BOOK_HEIGHT) / 2;

        articleButtons.clear();
        int articleButtonX = startX + 5;
        int articleButtonY = startY + 30;

        for (ProgressionArticle article : articles) {
            Button articleButton = Button.builder(Component.translatable(article.getTitle()), (button) -> {
                selectedArticle = article;
                currentPage = 0;
                recentlyOpenedArticles.remove(article);
                recentlyOpenedArticles.add(article);
                updateButtonVisibility();
            }).pos(articleButtonX, articleButtonY).size(ARTICLE_LIST_AREA_WIDTH, ARTICLE_BUTTON_HEIGHT).build();
            this.addRenderableWidget(articleButton);
            articleButtons.add(articleButton);
            articleButtonY += ARTICLE_BUTTON_HEIGHT + 2;
        }

        nextPageButton = Button.builder(Component.literal("->"), (button) -> {
            if (selectedArticle != null && currentPage < getEffectivePages(selectedArticle).size() - 1) {
                currentPage++;
                updateButtonVisibility();
            }
        }).pos(startX + BOOK_WIDTH - NAV_BUTTON_SIZE - 5, startY + BOOK_HEIGHT - NAV_BUTTON_SIZE - 5).size(NAV_BUTTON_SIZE, NAV_BUTTON_SIZE).build();
        this.addRenderableWidget(nextPageButton);

        prevPageButton = Button.builder(Component.literal("<-"), (button) -> {
            if (currentPage > 0) {
                currentPage--;
                updateButtonVisibility();
            }
        }).pos(startX + ARTICLE_LIST_AREA_WIDTH + 10 + 5, startY + BOOK_HEIGHT - NAV_BUTTON_SIZE - 5).size(NAV_BUTTON_SIZE, NAV_BUTTON_SIZE).build();
        this.addRenderableWidget(prevPageButton);

        updateButtonVisibility();
    }

    private void updateButtonVisibility() {
        if (selectedArticle != null) {
            nextPageButton.visible = currentPage < getEffectivePages(selectedArticle).size() - 1;
            prevPageButton.visible = currentPage > 0;
        } else {
            nextPageButton.visible = false;
            prevPageButton.visible = false;
        }

        for (int i = 0; i < articles.size(); i++) {
            ProgressionArticle article = articles.get(i);
            Button button = articleButtons.get(i);
            button.visible = true;
            button.active = true;
        }
    }

    @Override
    public void render(net.minecraft.client.gui.GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        super.renderBackground(pGuiGraphics, pMouseX, pMouseY, pPartialTick);

        int startX = (this.width - BOOK_WIDTH) / 2;
        int startY = (this.height - BOOK_HEIGHT) / 2;

        pGuiGraphics.fill(startX, startY, startX + BOOK_WIDTH, startY + BOOK_HEIGHT, 0xFF222222);

        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);

        net.minecraft.client.gui.Font font = this.minecraft.font;
        pGuiGraphics.drawCenteredString(font, this.title, this.width / 2, startY + 10, 0xFFFFFF);

        if (selectedArticle != null && !getEffectivePages(selectedArticle).isEmpty()) {
            int contentX = startX + ARTICLE_LIST_AREA_WIDTH + 10 + 5;
            int contentY = startY + 30;
            int contentWidth = BOOK_WIDTH - ARTICLE_LIST_AREA_WIDTH - 20;

            Component wrappedTitleComponent = Component.translatable(selectedArticle.getTitle());
            pGuiGraphics.drawWordWrap(font, wrappedTitleComponent, contentX, contentY, contentWidth, 0xADD8E6);

            List<FormattedCharSequence> titleLines = font.split(wrappedTitleComponent, contentWidth);
            int titleHeight = titleLines.size() * font.lineHeight;
            contentY += titleHeight + 5;

            if (currentPage >= 0 && currentPage < getEffectivePages(selectedArticle).size()) {
                String pageText = getEffectivePages(selectedArticle).get(currentPage).getText();
                Component pageComponent = buildPageComponent(pageText);
                pGuiGraphics.drawWordWrap(font, pageComponent, contentX, contentY, contentWidth, 0xFFFFFF);
            }
        }

        for (Button button : articleButtons) {
            if (selectedArticle != null && button.getMessage().getString().equals(Component.translatable(selectedArticle.getTitle()).getString())) {
                pGuiGraphics.fill(button.getX() - 1, button.getY() - 1, button.getX() + button.getWidth() + 1, button.getY() + button.getHeight() + 1, 0x80FFFF00);
            }
        }
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        return super.mouseClicked(pMouseX, pMouseY, pButton);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        super.onClose();
        if (Minecraft.getInstance().getConnection() != null) {
            Minecraft.getInstance().getConnection().getAdvancements().setListener(null);
        }
    }
}