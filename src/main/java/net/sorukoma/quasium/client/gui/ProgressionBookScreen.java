package net.sorukoma.quasium.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.sorukoma.quasium.progression.ProgressionArticle;
import net.sorukoma.quasium.progression.ProgressionPage;
import net.sorukoma.quasium.client.ClientTobaccoData;
import net.sorukoma.quasium.client.ClientOreLuckData;

import java.util.*;

public class ProgressionBookScreen extends Screen {

    private enum Category {
        START("quasium.progression.category.start", 0x4CAF50, List.of(
            "quasium.progression.introduction.title",
            "quasium.progression.tobacco_crops.title",
            "quasium.progression.fermentation.title",
            "quasium.progression.drying_shelf.title",
            "quasium.progression.molasses_chain.title",
            "quasium.progression.regular_tobacco.title",
            "quasium.progression.soaking_bowl.title"
        )),
        HOOKAH("quasium.progression.category.hookah", 0xFF9800, List.of(
            "quasium.progression.charcoal.title",
            "quasium.progression.calloud.title",
            "quasium.progression.heater.title",
            "quasium.progression.hookah_bowl.title",
            "quasium.progression.hookah.title",
            "quasium.progression.backpack_hookah.title"
        )),
        KAIF("quasium.progression.category.kaif", 0x9C27B0, List.of(
            "quasium.progression.kaif_system.title",
            "quasium.progression.kaif_mastery.title"
        )),
        QUASIUM("quasium.progression.category.quasium", 0x2196F3, List.of(
            "quasium.progression.quasium_ore.title",
            "quasium.progression.smelting_quasium.title",
            "quasium.progression.quasium_anvil.title",
            "quasium.progression.quasium_armor.title"
        ));

        final String translationKey;
        final int color;
        final List<String> articleOrder;

        Category(String key, int color, List<String> order) {
            this.translationKey = key;
            this.color = color;
            this.articleOrder = order;
        }
    }

    private static final int BOOK_WIDTH = 340;
    private static final int BOOK_HEIGHT = 210;
    private static final int TAB_AREA_WIDTH = 28;
    private static final int TAB_WIDTH = 22;
    private static final int TAB_EXPANDED_WIDTH = 85;
    private static final int TAB_HEIGHT = 20;
    private static final int TAB_GAP = 2;
    private static final int ARTICLE_LIST_WIDTH = 92;
    private static final int ARTICLE_BUTTON_HEIGHT = 17;
    private static final int ARTICLE_BUTTON_GAP = 1;
    private static final int NAV_BUTTON_SIZE = 16;
    private static final int SCROLL_BUTTON_SIZE = 12;
    private static final int MAX_VISIBLE_ARTICLES = 8;

    private Map<String, ProgressionArticle> articleMap = new HashMap<>();
    private List<ProgressionArticle> categoryArticles = new ArrayList<>();
    private ProgressionArticle selectedArticle;
    private Category selectedCategory = Category.START;
    private int currentPage = 0;
    private int articleListScrollOffset = 0;

    private Button nextPageButton;
    private Button prevPageButton;
    private Button scrollUpButton;
    private Button scrollDownButton;
    private final Button[] articleButtons = new Button[MAX_VISIBLE_ARTICLES];

    private ProgressionAdvancementListener advancementListener;

    private static final LinkedHashSet<ProgressionArticle> recentlyOpenedArticles = new LinkedHashSet<>();

    private static final String WELCOME_ARTICLE_TITLE = "quasium.progression.introduction.title";
    private static final String TOBACCO_ARTICLE_TITLE = "quasium.progression.regular_tobacco.title";
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

    private boolean hasHint(ProgressionArticle article) {
        return article.getHint() != null && !article.getHint().isEmpty();
    }

    private boolean isArticleVisible(ProgressionArticle article) {
        return isArticleUnlocked(article) || hasHint(article);
    }

    private List<ProgressionPage> getEffectivePages(ProgressionArticle article) {
        if (!isArticleUnlocked(article)) {
            if (hasHint(article)) {
                ProgressionPage hintPage = new ProgressionPage();
                hintPage.setText(article.getHint());
                return List.of(hintPage);
            }
            return List.of();
        }
        List<ProgressionPage> pages = new ArrayList<>(article.getPages());
        if (article.getTitle().equals(TOBACCO_ARTICLE_TITLE)) {
            for (Map.Entry<String, Boolean> preferenceEntry : ClientTobaccoData.getFlavorPreferences().entrySet()) {
                String flavor = preferenceEntry.getKey();
                ProgressionPage page = new ProgressionPage();
                page.setText("quasium.progression.regular_tobacco.preference_page:" + flavor + ":" + preferenceEntry.getValue());
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

    private static String getOreFortunePageText(int fortuneLevel) {
        return "quasium.progression.quasium_ore.fortune." + fortuneLevel;
    }

    private static Component buildPageComponent(String pageText) {
        if (pageText.startsWith("quasium.progression.regular_tobacco.preference_page:")) {
            String[] parts = pageText.split(":", 3);
            if (parts.length == 3) {
                Component flavorName = Component.translatable(getFlavorTranslationKey(parts[1]));
                Component preference = Component.translatable(Boolean.parseBoolean(parts[2])
                        ? "quasium.progression.regular_tobacco.liked"
                        : "quasium.progression.regular_tobacco.disliked");
                return Component.translatable("quasium.progression.regular_tobacco.preference", flavorName, preference);
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

    private void buildCategoryArticles() {
        categoryArticles.clear();
        for (String titleKey : selectedCategory.articleOrder) {
            ProgressionArticle article = articleMap.get(titleKey);
            if (article != null && isArticleVisible(article)) {
                categoryArticles.add(article);
            }
        }
    }

    private void selectCategory(Category category) {
        this.selectedCategory = category;
        this.articleListScrollOffset = 0;
        buildCategoryArticles();
        if (!categoryArticles.isEmpty()) {
            selectedArticle = categoryArticles.get(0);
        } else {
            selectedArticle = null;
        }
        currentPage = 0;
        updateArticleList();
        updateButtonVisibility();
    }

    private void selectArticle(ProgressionArticle article) {
        selectedArticle = article;
        currentPage = 0;
        if (isArticleUnlocked(article)) {
            recentlyOpenedArticles.remove(article);
            recentlyOpenedArticles.add(article);
        }
        updateButtonVisibility();
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();

        if (Minecraft.getInstance().getConnection() != null) {
            Minecraft.getInstance().getConnection().getAdvancements().setListener(this.advancementListener);
        }

        // Статьи приходят с сервера (ProgressionArticlesSyncPacket) и хранятся в ClientProgressionData.
        // Серверный ProgressionArticleManager недоступен клиенту на выделенном сервере, поэтому
        // читаем именно из клиентского хранилища (работает и в одиночке, и в мультиплеере).
        articleMap.clear();
        articleMap.putAll(net.sorukoma.quasium.client.ClientProgressionData.getArticles());

        int startX = (this.width - BOOK_WIDTH) / 2;
        int startY = (this.height - BOOK_HEIGHT) / 2;
        int listX = startX + TAB_AREA_WIDTH + 4;
        int listStartY = startY + 28;

        buildCategoryArticles();
        if (selectedArticle == null || !categoryArticles.contains(selectedArticle)) {
            selectedArticle = categoryArticles.isEmpty() ? null : categoryArticles.get(0);
        }
        currentPage = 0;

        for (int i = 0; i < MAX_VISIBLE_ARTICLES; i++) {
            final int posIndex = i;
            Button btn = Button.builder(Component.literal(""), (button) -> {
                int articleIdx = articleListScrollOffset + posIndex;
                if (articleIdx >= 0 && articleIdx < categoryArticles.size()) {
                    selectArticle(categoryArticles.get(articleIdx));
                }
            }).pos(listX, listStartY + i * (ARTICLE_BUTTON_HEIGHT + ARTICLE_BUTTON_GAP))
              .size(ARTICLE_LIST_WIDTH, ARTICLE_BUTTON_HEIGHT)
              .build();
            this.addRenderableWidget(btn);
            articleButtons[i] = btn;
        }

        scrollUpButton = Button.builder(Component.literal("^"), (button) -> {
            if (articleListScrollOffset > 0) {
                articleListScrollOffset--;
                updateArticleList();
            }
        }).pos(listX + ARTICLE_LIST_WIDTH / 2 - SCROLL_BUTTON_SIZE / 2, startY + 10)
          .size(SCROLL_BUTTON_SIZE, SCROLL_BUTTON_SIZE)
          .build();
        this.addRenderableWidget(scrollUpButton);

        int scrollDownY = listStartY + MAX_VISIBLE_ARTICLES * (ARTICLE_BUTTON_HEIGHT + ARTICLE_BUTTON_GAP) + 2;
        scrollDownButton = Button.builder(Component.literal("v"), (button) -> {
            if (articleListScrollOffset + MAX_VISIBLE_ARTICLES < categoryArticles.size()) {
                articleListScrollOffset++;
                updateArticleList();
            }
        }).pos(listX + ARTICLE_LIST_WIDTH / 2 - SCROLL_BUTTON_SIZE / 2, scrollDownY)
          .size(SCROLL_BUTTON_SIZE, SCROLL_BUTTON_SIZE)
          .build();
        this.addRenderableWidget(scrollDownButton);

        int contentAreaStartX = startX + TAB_AREA_WIDTH + ARTICLE_LIST_WIDTH + 10;
        nextPageButton = Button.builder(Component.literal("->"), (button) -> {
            if (selectedArticle != null && currentPage < getEffectivePages(selectedArticle).size() - 1) {
                currentPage++;
                updateButtonVisibility();
            }
        }).pos(startX + BOOK_WIDTH - NAV_BUTTON_SIZE - 8, startY + BOOK_HEIGHT - NAV_BUTTON_SIZE - 8)
          .size(NAV_BUTTON_SIZE, NAV_BUTTON_SIZE)
          .build();
        this.addRenderableWidget(nextPageButton);

        prevPageButton = Button.builder(Component.literal("<-"), (button) -> {
            if (currentPage > 0) {
                currentPage--;
                updateButtonVisibility();
            }
        }).pos(contentAreaStartX, startY + BOOK_HEIGHT - NAV_BUTTON_SIZE - 8)
          .size(NAV_BUTTON_SIZE, NAV_BUTTON_SIZE)
          .build();
        this.addRenderableWidget(prevPageButton);

        updateArticleList();
        updateButtonVisibility();
    }

    private void updateArticleList() {
        for (int i = 0; i < MAX_VISIBLE_ARTICLES; i++) {
            int idx = articleListScrollOffset + i;
            if (idx < categoryArticles.size()) {
                articleButtons[i].visible = true;
                articleButtons[i].setMessage(Component.translatable(categoryArticles.get(idx).getTitle()));
            } else {
                articleButtons[i].visible = false;
            }
        }
        scrollUpButton.visible = articleListScrollOffset > 0;
        scrollDownButton.visible = articleListScrollOffset + MAX_VISIBLE_ARTICLES < categoryArticles.size();
    }

    private void updateButtonVisibility() {
        if (selectedArticle != null) {
            List<ProgressionPage> effectivePages = getEffectivePages(selectedArticle);
            nextPageButton.visible = currentPage < effectivePages.size() - 1;
            prevPageButton.visible = currentPage > 0;
        } else {
            nextPageButton.visible = false;
            prevPageButton.visible = false;
        }
    }

    private int getTabRightX() {
        return (this.width - BOOK_WIDTH) / 2 + TAB_AREA_WIDTH - 2;
    }

    private int getTabY(int index) {
        return (this.height - BOOK_HEIGHT) / 2 + 10 + index * (TAB_HEIGHT + TAB_GAP);
    }

    @Override
    public void render(net.minecraft.client.gui.GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);

        int startX = (this.width - BOOK_WIDTH) / 2;
        int startY = (this.height - BOOK_HEIGHT) / 2;
        int listAreaX = startX + TAB_AREA_WIDTH;
        int sepX = listAreaX + ARTICLE_LIST_WIDTH + 8;
        net.minecraft.client.gui.Font font = this.minecraft.font;

        // Book background (opaque, covers everything under the book)
        pGuiGraphics.fill(startX, startY, startX + BOOK_WIDTH, startY + BOOK_HEIGHT, 0xFF1A1A1A);

        // Tab area background
        pGuiGraphics.fill(startX, startY, startX + TAB_AREA_WIDTH, startY + BOOK_HEIGHT, 0xFF252525);

        // Article list area background
        pGuiGraphics.fill(listAreaX, startY, listAreaX + ARTICLE_LIST_WIDTH + 8, startY + BOOK_HEIGHT, 0xFF202020);

        // Separator line between list and content
        pGuiGraphics.fill(sepX, startY + 5, sepX + 1, startY + BOOK_HEIGHT - 5, 0xFF3A3A3A);

        // Draw tabs (expanding to the left so they never overlap the article list)
        Category[] categories = Category.values();
        for (int i = 0; i < categories.length; i++) {
            Category cat = categories[i];
            int rightX = getTabRightX();
            int tabY = getTabY(i);
            boolean isSelected = cat == selectedCategory;
            int currentWidth = TAB_WIDTH;

            boolean isHovered = pMouseX >= rightX - TAB_EXPANDED_WIDTH
                    && pMouseX <= rightX
                    && pMouseY >= tabY
                    && pMouseY <= tabY + TAB_HEIGHT;

            if (isSelected || isHovered) {
                currentWidth = TAB_EXPANDED_WIDTH;
            }

            int leftX = rightX - currentWidth;
            int bgColor = isSelected ? cat.color : darken(cat.color, 0.4f);
            if (isHovered && !isSelected) {
                bgColor = darken(cat.color, 0.6f);
            }
            pGuiGraphics.fill(leftX, tabY, rightX, tabY + TAB_HEIGHT, bgColor | 0xFF000000);

            // Tab border
            pGuiGraphics.fill(leftX, tabY, rightX, tabY + 1, 0xFF555555);
            pGuiGraphics.fill(leftX, tabY + TAB_HEIGHT - 1, rightX, tabY + TAB_HEIGHT, 0xFF555555);
            pGuiGraphics.fill(leftX, tabY, leftX + 1, tabY + TAB_HEIGHT, 0xFF555555);
            pGuiGraphics.fill(rightX - 1, tabY, rightX, tabY + TAB_HEIGHT, 0xFF555555);

            // Tab label (only when expanded)
            if (currentWidth == TAB_EXPANDED_WIDTH) {
                Component tabLabel = Component.translatable(cat.translationKey);
                int textWidth = font.width(tabLabel);
                int labelX = leftX + (currentWidth - textWidth) / 2;
                int labelY = tabY + (TAB_HEIGHT - font.lineHeight) / 2;
                pGuiGraphics.drawString(font, tabLabel, labelX, labelY, 0xFFFFFF);
            }
        }

        // Category header above article list
        int listX = startX + TAB_AREA_WIDTH + 4;
        Component catHeader = Component.translatable(selectedCategory.translationKey);
        pGuiGraphics.drawString(font, catHeader, listX, startY + 4, 0xBBBBBB);

        // Re-render buttons on top of our custom backgrounds
        for (Renderable renderable : this.renderables) {
            renderable.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        }

        // Article button highlights
        for (int i = 0; i < MAX_VISIBLE_ARTICLES; i++) {
            int idx = articleListScrollOffset + i;
            if (idx < categoryArticles.size() && articleButtons[i].visible) {
                ProgressionArticle article = categoryArticles.get(idx);
                Button button = articleButtons[i];

                if (selectedArticle == article) {
                    boolean unlocked = isArticleUnlocked(article);
                    int highlightColor = unlocked ? 0x60FFFF00 : 0x60888888;
                    pGuiGraphics.fill(button.getX() - 1, button.getY() - 1,
                            button.getX() + button.getWidth() + 1, button.getY() + button.getHeight() + 1, highlightColor);
                }

                if (!isArticleUnlocked(article)) {
                    pGuiGraphics.drawString(font, "\uD83D\uDD12",
                            button.getX() + button.getWidth() - 10, button.getY() + 3, 0xAAAAAA);
                }
            }
        }

        // Content area
        if (selectedArticle != null && !getEffectivePages(selectedArticle).isEmpty()) {
            int contentX = sepX + 6;
            int contentY = startY + 12;
            int contentWidth = BOOK_WIDTH - TAB_AREA_WIDTH - ARTICLE_LIST_WIDTH - 24;

            boolean unlocked = isArticleUnlocked(selectedArticle);

            Component wrappedTitleComponent = Component.translatable(selectedArticle.getTitle());
            pGuiGraphics.drawWordWrap(font, wrappedTitleComponent, contentX, contentY, contentWidth,
                    unlocked ? 0xADD8E6 : 0x808080);

            List<FormattedCharSequence> titleLines = font.split(wrappedTitleComponent, contentWidth);
            int titleHeight = titleLines.size() * font.lineHeight;
            contentY += titleHeight + 4;

            if (!unlocked) {
                Component lockLabel = Component.translatable("quasium.progression.locked");
                pGuiGraphics.drawString(font, lockLabel, contentX, contentY, 0xFF5555);
                contentY += font.lineHeight + 3;
            }

            if (currentPage >= 0 && currentPage < getEffectivePages(selectedArticle).size()) {
                String pageText = getEffectivePages(selectedArticle).get(currentPage).getText();
                Component pageComponent = buildPageComponent(pageText);
                int textColor = unlocked ? 0xDDDDDD : 0xAAAAAA;
                pGuiGraphics.drawWordWrap(font, pageComponent, contentX, contentY, contentWidth, textColor);
            }

            // Page counter
            int totalPages = getEffectivePages(selectedArticle).size();
            if (totalPages > 1) {
                String pageIndicator = (currentPage + 1) + "/" + totalPages;
                int piWidth = font.width(pageIndicator);
                pGuiGraphics.drawString(font, pageIndicator,
                        startX + BOOK_WIDTH - piWidth - 8,
                        startY + BOOK_HEIGHT - font.lineHeight - 4, 0x888888);
            }
        }
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        // Check tab clicks (tabs expand left from right edge, click area uses expanded width)
        Category[] categories = Category.values();
        for (int i = 0; i < categories.length; i++) {
            int rightX = getTabRightX();
            int tabY = getTabY(i);
            if (pMouseX >= rightX - TAB_EXPANDED_WIDTH
                    && pMouseX <= rightX
                    && pMouseY >= tabY && pMouseY <= tabY + TAB_HEIGHT) {
                if (categories[i] != selectedCategory) {
                    selectCategory(categories[i]);
                }
                return true;
            }
        }
        return super.mouseClicked(pMouseX, pMouseY, pButton);
    }

    @Override
    public boolean mouseScrolled(double pMouseX, double pMouseY, double pScrollX, double pScrollY) {
        int startX = (this.width - BOOK_WIDTH) / 2;
        int startY = (this.height - BOOK_HEIGHT) / 2;
        int listAreaX = startX + TAB_AREA_WIDTH;
        int listAreaRight = listAreaX + ARTICLE_LIST_WIDTH + 8;

        if (pMouseX >= listAreaX && pMouseX <= listAreaRight
                && pMouseY >= startY && pMouseY <= startY + BOOK_HEIGHT) {
            if (pScrollY < 0 && articleListScrollOffset + MAX_VISIBLE_ARTICLES < categoryArticles.size()) {
                articleListScrollOffset++;
                updateArticleList();
                return true;
            } else if (pScrollY > 0 && articleListScrollOffset > 0) {
                articleListScrollOffset--;
                updateArticleList();
                return true;
            }
        }
        return super.mouseScrolled(pMouseX, pMouseY, pScrollX, pScrollY);
    }

    private static int darken(int color, float factor) {
        int r = (int) (((color >> 16) & 0xFF) * factor);
        int g = (int) (((color >> 8) & 0xFF) * factor);
        int b = (int) ((color & 0xFF) * factor);
        return (r << 16) | (g << 8) | b;
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
