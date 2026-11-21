package net.sorukoma.quasium.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.sorukoma.quasium.client.ClientProgressionData;
import net.sorukoma.quasium.client.QuasiumClientConfig;
import net.sorukoma.quasium.client.gui.book.BookArt;
import net.sorukoma.quasium.client.gui.book.BookContent;
import net.sorukoma.quasium.client.gui.book.BookHover;
import net.sorukoma.quasium.client.gui.book.BookLayout;
import net.sorukoma.quasium.client.gui.book.BookPalette;
import net.sorukoma.quasium.client.gui.book.BookPiece;
import net.sorukoma.quasium.client.gui.book.BookResearch;
import net.sorukoma.quasium.client.gui.book.BookReward;
import net.sorukoma.quasium.client.gui.book.BookUnlocks;
import net.sorukoma.quasium.client.hint.Hints;
import net.sorukoma.quasium.mind.ClientMind;
import net.sorukoma.quasium.progression.ProgressionArticle;
import net.sorukoma.quasium.progression.ProgressionCategory;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ProgressionBookScreen extends Screen {
    private static final int MAX_WIDTH = 340;
    public static final int MAX_HEIGHT = BookArt.TAB_H * Math.max(ProgressionCategory.side(false).size(), ProgressionCategory.side(true).size());
    private static final int PAD = 6;
    private static final int FOOTER = 17;
    private static final int ARROW_W = 12;
    private static final int ARROW_H = 17;
    private static final int HINT_H = 14;
    private static final int THEME_SIZE = 11;

    private static ProgressionCategory lastCategory = ProgressionCategory.START;
    private static ResourceLocation lastArticle;
    private static int lastPage;

    private final BookUnlocks unlocks = new BookUnlocks(() -> dirty = true);
    private final List<ProgressionArticle> listed = new ArrayList<>();
    private final BookListPane list = new BookListPane(this);
    private final BookResearch research = new BookResearch();
    private final BookReward reward = new BookReward(HINT_H);
    private final BookTabs categoryTabs = new BookTabs(unlocks::isUnlocked);
    private int mindVersion = -1;
    private List<List<BookPiece>> pages = List.of(List.of());
    private List<FormattedCharSequence> titleLines = List.of();
    private ProgressionCategory category = lastCategory;
    private ProgressionArticle article;
    private int page;
    private boolean dirty = true;
    private BookHover hover = new BookHover(0, 0);
    private ImageButton back;
    private ImageButton forward;
    private Button hint;
    private int frameX;
    private int frameY;
    private int frameW;
    private int frameH;
    private int foldX;
    private int pageX;
    private int pageY;
    private int pageW;
    private int pageH;

    public ProgressionBookScreen() {
        super(Component.translatable("gui.quasium.progression_book.title"));
    }

    public static ProgressionBookScreen openAt(ResourceLocation articleId) {
        ProgressionArticle target = ClientProgressionData.getArticles().get(articleId);
        if (target != null) {
            lastCategory = categoryOf(target);
            lastArticle = articleId;
            lastPage = 0;
        }
        return new ProgressionBookScreen();
    }

    @Override
    protected void init() {
        int tabs = BookArt.TAB_W - BookArt.TAB_OVERLAP;
        frameW = Math.min(MAX_WIDTH, width - 2 * tabs - 16);
        frameH = Math.min(MAX_HEIGHT, height - 8);
        frameX = (width - frameW) / 2;
        frameY = (height - frameH) / 2;
        int innerX = frameX + BookArt.FRAME_SIDE;
        int innerY = frameY + BookArt.FRAME_TOP;
        int innerW = frameW - 2 * BookArt.FRAME_SIDE;
        int innerH = frameH - BookArt.FRAME_TOP - BookArt.FRAME_SIDE;
        int listW = Mth.clamp(innerW * 34 / 100, 80, 108);
        list.place(innerX + PAD, innerY + PAD, listW, innerH - 2 * PAD);
        foldX = innerX + PAD + listW + 5;
        pageX = foldX + 7;
        pageY = innerY + PAD + 1;
        pageW = innerX + innerW - PAD - 1 - pageX;
        pageH = innerH - 2 * PAD - 2;
        back = addRenderableWidget(new ImageButton(pageX, pageY + pageH - ARROW_H, ARROW_W, ARROW_H,
                new WidgetSprites(BookArt.PAGE_BACKWARD, BookArt.PAGE_BACKWARD_LIT), button -> turnWithSound(-1)));
        forward = addRenderableWidget(new ImageButton(pageX + pageW - ARROW_W, pageY + pageH - ARROW_H, ARROW_W, ARROW_H,
                new WidgetSprites(BookArt.PAGE_FORWARD, BookArt.PAGE_FORWARD_LIT), button -> turnWithSound(1)));
        Component hintLabel = Component.translatable("quasium.progression.hint_button");
        int hintW = font.width(hintLabel) + 10;
        hint = addRenderableWidget(Button.builder(hintLabel, button -> openHint()).bounds(pageX + pageW - hintW, pageY + 1, hintW, HINT_H).build());
        addRenderableWidget(reward.button());
        reward.place(pageX, pageY + pageH - HINT_H - 1, pageW);
        research.buttons().forEach(this::addRenderableWidget);
        research.place(pageX, pageY + pageH - FOOTER - BookResearch.BUTTON_H, pageW);
        categoryTabs.place(frameX, frameY, frameW);
        addThemeToggle();
        unlocks.attach();
        refresh();
    }

    private void addThemeToggle() {
        BookPalette palette = BookArt.palette();
        ImageButton toggle = addRenderableWidget(new ImageButton(frameX + frameW - BookArt.FRAME_SIDE - THEME_SIZE, frameY + 4,
                THEME_SIZE, THEME_SIZE, new WidgetSprites(palette.toggle(), palette.toggleLit()), button -> flipTheme()));
        boolean dark = QuasiumClientConfig.DARK_BOOK.get();
        toggle.setTooltip(Tooltip.create(Component.translatable(dark ? "quasium.progression.theme_light" : "quasium.progression.theme_dark")));
    }

    private void flipTheme() {
        QuasiumClientConfig.flipDarkBook();
        rebuildWidgets();
    }

    @Override
    public void removed() {
        unlocks.detach();
        super.removed();
    }

    List<ProgressionArticle> listed() {
        return listed;
    }

    ProgressionArticle selected() {
        return article;
    }

    boolean isUnlocked(ProgressionArticle candidate) {
        return unlocks.isUnlocked(candidate);
    }

    void select(ProgressionArticle candidate) {
        if (candidate == article) return;
        article = candidate;
        page = 0;
        layoutPages();
    }

    private void refresh() {
        dirty = false;
        listed.clear();
        ClientProgressionData.getArticles().values().stream()
                .filter(candidate -> categoryOf(candidate) == category && (!candidate.isSecret() || unlocks.isUnlocked(candidate)))
                .filter(BookResearch::visible)
                .sorted(Comparator.comparingInt((ProgressionArticle candidate) -> candidate.isResearch() ? candidate.getResearch().tier() : 0)
                        .thenComparingInt(ProgressionArticle::getOrder).thenComparing(candidate -> candidate.getId().toString()))
                .forEach(listed::add);
        if (article == null || !listed.contains(article)) {
            article = listed.stream().filter(candidate -> candidate.getId().equals(lastArticle)).findFirst()
                    .orElse(listed.isEmpty() ? null : listed.get(0));
            page = article != null && article.getId().equals(lastArticle) ? lastPage : 0;
        }
        list.reveal(listed.indexOf(article));
        layoutPages();
    }

    private ResourceLocation scene() {
        if (article == null || article.getScene() == null || !unlocks.isUnlocked(article)) return null;
        ResourceLocation scene = ResourceLocation.tryParse(article.getScene());
        return scene != null && Hints.has(scene) ? scene : null;
    }

    private void openHint() {
        ResourceLocation scene = scene();
        if (scene != null) Hints.open(scene, this);
    }

    private void layoutPages() {
        int titleRoom = pageW - 20 - (scene() != null && hint != null ? hint.getWidth() + 4 : 0);
        titleLines = article == null ? List.of() : font.split(Component.translatable(article.getTitle()), titleRoom);
        if (titleLines.size() > 2) titleLines = titleLines.subList(0, 2);
        List<BookPiece> pieces = article == null ? List.of()
                : unlocks.isUnlocked(article) ? BookContent.unlocked(article, font, pageW)
                : BookContent.locked(article, unlocks.unlockAdvancement(article), font, pageW);
        int body = pageH - FOOTER - (BookResearch.pending(article) ? BookResearch.BUTTON_H + 3 : 0);
        pages = BookLayout.paginate(pieces, BookPiece::height, piece -> piece instanceof BookPiece.Gap, body - headerHeight(), body);
        page = Mth.clamp(page, 0, pages.size() - 1);
        remember();
    }

    private int headerHeight() {
        return Math.max(16, titleLines.size() * BookPiece.LINE) + 8;
    }

    private void turn(int direction) {
        page = Mth.clamp(page + direction, 0, pages.size() - 1);
        remember();
    }

    private void turnWithSound(int direction) {
        int before = page;
        turn(direction);
        if (page != before) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1f));
    }

    private void remember() {
        lastCategory = category;
        lastArticle = article == null ? null : article.getId();
        lastPage = page;
        if (back == null) return;
        back.visible = page > 0;
        forward.visible = page < pages.size() - 1;
        hint.visible = page == 0 && scene() != null;
        research.show(article, page == pages.size() - 1, font);
        reward.show(article, article != null && unlocks.isUnlocked(article), page == pages.size() - 1, font);
    }

    public void refreshReward() {
        reward.answered();
        remember();
    }

    static ProgressionCategory categoryOf(ProgressionArticle candidate) {
        return ProgressionCategory.byId(candidate.getCategory()).orElse(ProgressionCategory.START);
    }

    @Override
    public void tick() {
        if (BookResearch.pending(article) && minecraft.player != null && minecraft.player.tickCount % 20 == 0) layoutPages();
        if (reward.insightChanged()) remember();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        hover = new BookHover(mouseX, mouseY);
        super.render(graphics, mouseX, mouseY, partialTick);
        hover.render(graphics, font);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        if (dirty || mindVersion != ClientMind.version()) {
            mindVersion = ClientMind.version();
            refresh();
        }
        graphics.blitSprite(BookArt.palette().frame(), frameX, frameY, frameW, frameH);
        graphics.drawString(font, Component.translatable(category.translationKey()), frameX + 8, frameY + 6, BookArt.palette().label(), false);
        categoryTabs.render(graphics, hover, category);
        BookArt.fold(graphics, foldX, frameY + BookArt.FRAME_TOP + 2, frameY + frameH - BookArt.FRAME_SIDE - 2);
        list.render(graphics, font, hover);
        renderPage(graphics);
    }

    private void renderPage(GuiGraphics graphics) {
        if (article == null) {
            graphics.drawString(font, Component.translatable("quasium.progression.empty"), pageX, pageY + 4, BookArt.palette().muted(), false);
            return;
        }
        int y = pageY;
        if (page == 0) {
            boolean open = unlocks.isUnlocked(article);
            ResourceLocation icon = article.getIcon() == null ? null : ResourceLocation.tryParse(article.getIcon());
            if (icon != null) graphics.renderFakeItem(new ItemStack(BuiltInRegistries.ITEM.get(icon)), pageX, y);
            if (!open) BookArt.shadeIcon(graphics, pageX, y);
            int titleY = y + (titleLines.size() > 1 ? 0 : 4);
            for (FormattedCharSequence line : titleLines) {
                graphics.drawString(font, line, pageX + 20, titleY, open ? BookArt.palette().title() : BookArt.palette().muted(), false);
                titleY += BookPiece.LINE;
            }
            y += headerHeight();
            graphics.fill(pageX, y - 5, pageX + pageW, y - 4, BookArt.palette().rule());
        }
        for (BookPiece piece : pages.get(page)) {
            piece.draw(graphics, font, pageX, y, pageW, hover);
            y += piece.height();
        }
        if (pages.size() > 1) {
            String counter = (page + 1) + " / " + pages.size();
            graphics.drawString(font, counter, pageX + (pageW - font.width(counter)) / 2, pageY + pageH - 12, BookArt.palette().muted(), false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        ProgressionCategory tab = categoryTabs.at(mouseX, mouseY).orElse(null);
        if (tab != null) {
            openCategory(tab);
            return true;
        }
        return list.mouseClicked(mouseX, mouseY) || super.mouseClicked(mouseX, mouseY, button);
    }

    private void openCategory(ProgressionCategory target) {
        if (target == category) return;
        category = target;
        article = null;
        lastArticle = null;
        refresh();
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1f));
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return list.mouseDragged(mouseY) || super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        list.mouseReleased();
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (list.mouseScrolled(mouseX, mouseY, scrollY)) return true;
        if (scrollY != 0 && mouseX >= pageX && mouseX < pageX + pageW && mouseY >= pageY && mouseY < pageY + pageH) {
            turnWithSound(scrollY < 0 ? 1 : -1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        switch (keyCode) {
            case GLFW.GLFW_KEY_RIGHT, GLFW.GLFW_KEY_PAGE_DOWN -> turnWithSound(1);
            case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_PAGE_UP -> turnWithSound(-1);
            case GLFW.GLFW_KEY_DOWN -> list.step(1);
            case GLFW.GLFW_KEY_UP -> list.step(-1);
            default -> {
                return super.keyPressed(keyCode, scanCode, modifiers);
            }
        }
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
