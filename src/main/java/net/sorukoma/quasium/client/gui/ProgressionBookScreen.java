package net.sorukoma.quasium.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.progression.ProgressionArticle;
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
import java.util.HashSet; // Import HashSet

public class ProgressionBookScreen extends Screen {

    private List<ProgressionArticle> articles = new ArrayList<>();
    private ProgressionArticle selectedArticle;
    private int currentPage = 0;

    // Constants for layout
    private static final int BOOK_WIDTH = 256;
    private static final int BOOK_HEIGHT = 192;
    private static final int ARTICLE_LIST_AREA_WIDTH = 100; // Increased width for article titles/buttons
    private static final int ARTICLE_BUTTON_HEIGHT = 20; // Height for each article button
    private static final int NAV_BUTTON_SIZE = 20; // Size for square navigation buttons

    private Button nextPageButton;
    private Button prevPageButton;
    private List<Button> articleButtons = new ArrayList<>(); // List to hold article selection buttons

    private ProgressionAdvancementListener advancementListener; // Новый слушатель

    // Static LinkedHashSet to maintain the order of recently opened articles
    private static final LinkedHashSet<ProgressionArticle> recentlyOpenedArticles = new LinkedHashSet<>();

    // Constant for the "Welcome" article title
    private static final String WELCOME_ARTICLE_TITLE = "Добро пожаловать в Квазиум!";

    public ProgressionBookScreen() {
        super(Component.translatable("gui.quasium.progression_book.title"));
        this.advancementListener = new ProgressionAdvancementListener(); // Инициализация слушателя
        // Articles are filtered in init() to ensure player data is available
    }

    // Method to check if an article is unlocked based on advancements
    private boolean isArticleUnlocked(ProgressionArticle article) {
        if (article.getUnlockCondition() == null || article.getUnlockCondition().isEmpty()) {
            return true; // Articles without a condition are always unlocked
        }

        // Check player advancements
        if (Minecraft.getInstance().player != null && Minecraft.getInstance().getConnection() != null) {
            // Correctly parse the ResourceLocation
            ResourceLocation advancementId = ResourceLocation.parse(article.getUnlockCondition());
            ClientAdvancements clientAdvancements = Minecraft.getInstance().getConnection().getAdvancements();

            // get() returns AdvancementHolder
            AdvancementHolder advancementHolder = clientAdvancements.get(advancementId);

            if (advancementHolder != null) {
                // Используем наш слушатель для получения прогресса
                AdvancementProgress progress = advancementListener.getProgress(advancementHolder);
                return progress != null && progress.isDone();
            }
        }
        return false; // If player is null, connection is null, or advancement not found/not done
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets(); // Clear existing widgets on re-init (e.g., screen resize)

        // Зарегистрировать слушателя достижений
        if (Minecraft.getInstance().getConnection() != null) {
            Minecraft.getInstance().getConnection().getAdvancements().setListener(this.advancementListener);
        }

        // Populate ALL articles first, then filter and sort for display
        List<ProgressionArticle> allAvailableArticles = new ArrayList<>();
        if (QuasiumMod.getProgressionArticleManager() != null) {
            allAvailableArticles = QuasiumMod.getProgressionArticleManager().getArticles().values().stream()
                    .filter(this::isArticleUnlocked) // Filter only unlocked articles
                    .collect(Collectors.toList());
        }

        this.articles.clear();
        Set<ProgressionArticle> addedToSortedList = new HashSet<>(); // To keep track of articles already added

        // 1. Add the "Welcome" article first, if it's available
        ProgressionArticle welcomeArticle = allAvailableArticles.stream()
                .filter(article -> article.getTitle().equals(WELCOME_ARTICLE_TITLE))
                .findFirst()
                .orElse(null);

        if (welcomeArticle != null) {
            this.articles.add(welcomeArticle);
            addedToSortedList.add(welcomeArticle);
        }

        // 2. Collect unopened and opened articles (excluding welcome article)
        List<ProgressionArticle> unopenedArticles = new ArrayList<>();
        List<ProgressionArticle> openedArticles = new ArrayList<>();

        for (ProgressionArticle article : allAvailableArticles) {
            if (!addedToSortedList.contains(article)) { // If not the welcome article
                if (recentlyOpenedArticles.contains(article)) {
                    openedArticles.add(article);
                } else {
                    unopenedArticles.add(article);
                }
            }
        }

        // 3. Sort unopened articles alphabetically
        unopenedArticles.sort((a1, a2) -> a1.getTitle().compareTo(a2.getTitle()));
        this.articles.addAll(unopenedArticles);
        addedToSortedList.addAll(unopenedArticles);

        // 4. Sort opened articles by recency (most recent first)
        // Iterate recentlyOpenedArticles in reverse to get most recent first
        List<ProgressionArticle> tempRecentlyOpened = new ArrayList<>(recentlyOpenedArticles);
        for (int i = tempRecentlyOpened.size() - 1; i >= 0; i--) {
            ProgressionArticle article = tempRecentlyOpened.get(i);
            if (openedArticles.contains(article)) { // Only add if it's an opened and available article
                this.articles.add(article);
                addedToSortedList.add(article);
            }
        }
        // Remove duplicates that might have been added from openedArticles list if they were also in recentlyOpenedArticles
        // This step is crucial to ensure no duplicates and correct order.
        this.articles = this.articles.stream().distinct().collect(Collectors.toList());


        if (!articles.isEmpty()) {
            // If no article was previously selected, or the selected one is no longer in the list, select the first one
            if (selectedArticle == null || !articles.contains(selectedArticle)) {
                selectedArticle = articles.get(0);
            }
        } else {
            selectedArticle = null;
        }
        currentPage = 0; // Reset page on init

        int startX = (this.width - BOOK_WIDTH) / 2;
        int startY = (this.height - BOOK_HEIGHT) / 2;

        // Article selection buttons
        articleButtons.clear(); // Clear previous buttons
        int articleButtonX = startX + 5; // A bit of padding from the left edge of the book area
        int articleButtonY = startY + 30; // Below the main title

        for (ProgressionArticle article : articles) {
            Button articleButton = Button.builder(Component.literal(article.getTitle()), (button) -> {
                selectedArticle = article;
                currentPage = 0; // Reset page when a new article is selected
                // Update recently opened articles: remove and re-add to put it at the end
                recentlyOpenedArticles.remove(article);
                recentlyOpenedArticles.add(article);
                updateButtonVisibility(); // This will also update article button states
            }).pos(articleButtonX, articleButtonY).size(ARTICLE_LIST_AREA_WIDTH, ARTICLE_BUTTON_HEIGHT).build();
            this.addRenderableWidget(articleButton);
            articleButtons.add(articleButton);
            articleButtonY += ARTICLE_BUTTON_HEIGHT + 2; // Space between buttons
        }

        // Page navigation buttons
        nextPageButton = Button.builder(Component.literal("->"), (button) -> {
            if (selectedArticle != null && currentPage < selectedArticle.getPages().size() - 1) {
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

        updateButtonVisibility(); // Initial call to set states for all buttons
    }

    private void updateButtonVisibility() {
        // Update page navigation buttons
        if (selectedArticle != null) {
            nextPageButton.visible = currentPage < selectedArticle.getPages().size() - 1;
            prevPageButton.visible = currentPage > 0;
        } else {
            nextPageButton.visible = false;
            prevPageButton.visible = false;
        }

        // Update article selection buttons visibility and activity
        // The articles list is already filtered and sorted in init(), so all articles here are unlocked and visible.
        // We just need to ensure their active state is correct (which it should be if they are in the list).
        for (int i = 0; i < articles.size(); i++) {
            ProgressionArticle article = articles.get(i);
            Button button = articleButtons.get(i);
            // Since `articles` now only contains unlocked articles, they should all be active and visible.
            button.visible = true;
            button.active = true;
        }
    }

    @Override
    public void render(net.minecraft.client.gui.GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        // 1. Отрисовываем размытый фон игрового мира
        super.renderBackground(pGuiGraphics, pMouseX, pMouseY, pPartialTick);

        int startX = (this.width - BOOK_WIDTH) / 2;
        int startY = (this.height - BOOK_HEIGHT) / 2;

        // 2. Отрисовываем непрозрачный фон для области книги, чтобы закрыть размытие под ней
        pGuiGraphics.fill(startX, startY, startX + BOOK_WIDTH, startY + BOOK_HEIGHT, 0xFF222222); // Темно-серый фон

        // 3. Отрисовываем все виджеты (кнопки). Это может сбросить состояние размытия.
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);

        // 4. Теперь отрисовываем пользовательские текстовые элементы
        // Основной заголовок
        pGuiGraphics.drawCenteredString(this.font, this.title, this.width / 2, startY + 10, 0xFFFFFF);

        // Отрисовываем содержимое выбранной статьи справа
        if (selectedArticle != null && !selectedArticle.getPages().isEmpty()) {
            int contentX = startX + ARTICLE_LIST_AREA_WIDTH + 10 + 5; // Справа от области списка статей + отступ
            int contentY = startY + 30;
            int contentWidth = BOOK_WIDTH - ARTICLE_LIST_AREA_WIDTH - 20; // Корректировка для отступов

            // Отрисовываем заголовок статьи (как часть содержимого, а не кнопки)
            Component wrappedTitleComponent = Component.literal(selectedArticle.getTitle());
            pGuiGraphics.drawWordWrap(this.font, wrappedTitleComponent, contentX, contentY, contentWidth, 0xADD8E6);
            
            // Вычисляем высоту, занимаемую перенесенным заголовком
            List<FormattedCharSequence> titleLines = this.font.split(wrappedTitleComponent, contentWidth);
            int titleHeight = titleLines.size() * this.font.lineHeight;
            contentY += titleHeight + 5; // Корректируем contentY на основе фактической высоты заголовка

            // Отрисовываем текст текущей страницы
            if (currentPage >= 0 && currentPage < selectedArticle.getPages().size()) {
                String pageText = selectedArticle.getPages().get(currentPage).getText();
                pGuiGraphics.drawWordWrap(this.font, Component.literal(pageText), contentX, contentY, contentWidth, 0xFFFFFF);
            }
        }

        // 5. Отрисовываем подсветку для выбранной кнопки статьи (это должно быть после отрисовки кнопок)
        for (Button button : articleButtons) {
            if (selectedArticle != null && button.getMessage().getString().equals(selectedArticle.getTitle())) {
                pGuiGraphics.fill(button.getX() - 1, button.getY() - 1, button.getX() + button.getWidth() + 1, button.getY() + button.getHeight() + 1, 0x80FFFF00); // Желтая прозрачная подсветка
            }
        }
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        // super.mouseClicked will handle button clicks
        return super.mouseClicked(pMouseX, pMouseY, pButton);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        super.onClose();
        // Отменить регистрацию слушателя при закрытии экрана
        if (Minecraft.getInstance().getConnection() != null) {
            Minecraft.getInstance().getConnection().getAdvancements().setListener(null);
        }
    }
}
