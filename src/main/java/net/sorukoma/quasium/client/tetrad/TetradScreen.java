package net.sorukoma.quasium.client.tetrad;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.sorukoma.quasium.magic.page.PageCasting;
import net.sorukoma.quasium.magic.page.PageConfig;
import net.sorukoma.quasium.magic.page.PageLabels;
import net.sorukoma.quasium.magic.page.PagePackets;
import net.sorukoma.quasium.magic.page.PageRegistry;
import net.sorukoma.quasium.magic.page.SpellPage;
import net.sorukoma.quasium.magic.page.SpellTetradItem;
import net.sorukoma.quasium.magic.page.TetradContents;
import net.sorukoma.quasium.magic.page.TetradPackets;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Optional;

public class TetradScreen extends Screen {
    public static final int WIDTH = 300;
    public static final int HEIGHT = 216;
    private static final int LIST_X = 7;
    private static final int LIST_Y = 17;
    private static final int LIST_W = 90;
    private static final int LIST_H = 160;
    private static final int RIGHT_X = 104;
    private static final int RIGHT_W = 188;
    private static final int PYRAMID_Y = 24;
    private static final int SHEET_Y = 94;
    private static final int LOWER_Y = 92;
    private static final int BUTTON_H = 14;
    private static final int SAVE_DELAY = 12;
    private static final int UNSAVED_AFTER = 30;
    private static final int CLEAR_ARM_TICKS = 60;

    private final InteractionHand hand;
    private final TetradDraft draft = new TetradDraft();
    private final PartPicker picker = new PartPicker();
    private final PyramidPane pyramid = new PyramidPane();
    private final PageSheet sheet = new PageSheet();
    private final PageList list = new PageList();
    private boolean opened;
    private int selected;
    private int left;
    private int top;
    private int ticks;
    private boolean pending;
    private int idleTicks;
    private int sinceSent = UNSAVED_AFTER;
    private int clearArmedUntil;
    private EditBox nameBox;
    private Button clearButton;
    private Button overflowButton;
    private Button tearButton;
    @Nullable
    private Button copyButton;

    public TetradScreen(InteractionHand hand, Component title) {
        super(title);
        this.hand = hand;
    }

    public int left() {
        return left;
    }

    public int top() {
        return top;
    }

    @Override
    protected void init() {
        left = (width - WIDTH) / 2;
        top = (height - HEIGHT) / 2;
        TetradContents contents = contents();
        if (!opened) {
            opened = true;
            selected = contents.active();
            draft.reset(contents.page(selected));
        }
        picker.close();
        list.place(left + LIST_X, top + LIST_Y, LIST_W, LIST_H, contents.size());
        int rightX = left + RIGHT_X;
        nameBox = addRenderableWidget(new EditBox(font, rightX, top + 5, 132, BUTTON_H, Component.translatable("quasium.tetrad.name")));
        nameBox.setMaxLength(SpellPage.MAX_NAME);
        showName();
        clearButton = addRenderableWidget(Button.builder(Component.empty(), button -> clear())
                .bounds(rightX + 136, top + 5, RIGHT_W - 136, BUTTON_H).build());
        overflowButton = addRenderableWidget(Button.builder(Component.empty(), button -> {
            draft.cycleOverflow();
            edited(true);
        }).bounds(rightX, top + SHEET_Y + 12, 130, BUTTON_H).tooltip(Tooltip.create(overflowHint())).build());
        tearButton = addRenderableWidget(Button.builder(Component.empty(), button -> tearOrPaste())
                .bounds(left + LIST_X, top + LIST_Y + LIST_H + 3, LIST_W, BUTTON_H).build());
        copyButton = null;
        if (contents.size() > PageRegistry.SMALL_PAGES) {
            copyButton = addRenderableWidget(Button.builder(Component.translatable("quasium.tetrad.copy"), button -> TetradPackets.sendCopyOver(hand))
                    .bounds(left + LIST_X, top + LIST_Y + LIST_H + 5 + BUTTON_H, LIST_W, BUTTON_H)
                    .tooltip(Tooltip.create(Component.translatable("quasium.tetrad.copy.hint"))).build());
        }
        pyramid.place(rightX, top + PYRAMID_Y);
        sheet.place(rightX, top + SHEET_Y, RIGHT_W, HEIGHT - SHEET_Y - 6, pyramid.right() + 8, top + PYRAMID_Y);
        refreshWidgets();
    }

    @Override
    public void tick() {
        ticks++;
        if (!(player().getItemInHand(hand).getItem() instanceof SpellTetradItem)) {
            onClose();
            return;
        }
        draft.follow(contents().page(selected));
        if (!nameBox.isFocused() && !nameBox.getValue().equals(draft.page().name())) showName();
        idleTicks++;
        sinceSent++;
        if (pending && idleTicks >= SAVE_DELAY) save();
        refreshWidgets();
    }

    @Override
    public void removed() {
        save();
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blitSprite(TetradArt.PANEL, left, top, WIDTH, HEIGHT);
        graphics.drawString(font, Component.translatable("quasium.tetrad.pages"), left + 8, top + 6, TetradArt.INK, false);
        list.render(graphics, font, contents(), selected, draft.page(), mouseX, mouseY);
        pyramid.render(graphics, font, player(), draft, picker, mouseX, mouseY);
        Optional<Component> problem = draft.problem(player());
        sheet.render(graphics, font, player(), draft, problem, problem.isEmpty() && draft.dirty() && !pending && sinceSent >= UNSAVED_AFTER);
        picker.render(graphics, font, player(), draft, mouseX, mouseY);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        List<FormattedCharSequence> tip = picker.isOpen() ? picker.tooltip(font, player(), draft, mouseX, mouseY) : null;
        if (tip == null && !picker.covers(mouseX, mouseY)) tip = sheet.tooltip(font, draft, mouseX, mouseY);
        if (tip == null) tip = pyramid.tooltip(font, player(), draft, mouseX, mouseY);
        if (tip == null) tip = list.tooltip(font, contents(), selected, mouseX, mouseY);
        if (tip != null) graphics.renderTooltip(font, tip, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (picker.isOpen()) {
            PartPicker.Cell cell = picker.at(mouseX, mouseY);
            if (cell != null) {
                choose(cell);
                return true;
            }
            if (picker.covers(mouseX, mouseY)) return true;
            picker.close();
        }
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        PyramidPane.Slot slot = pyramid.at(mouseX, mouseY);
        if (slot != null) {
            slotClicked(slot, button);
            return true;
        }
        int row = list.rowAt(mouseX, mouseY);
        if (row < 0) return false;
        selectPage(row);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && picker.isOpen()) {
            picker.close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    public void pick(PartPicker.Kind kind, int slot) {
        PyramidPane.Slot target = pyramid.slot(kind, slot);
        if (target != null) slotClicked(target, 0);
    }

    public void selectPage(int row) {
        save();
        selected = row;
        draft.reset(contents().page(row));
        picker.close();
        clearArmedUntil = 0;
        showName();
        if (!draft.page().isEmpty() && row != contents().active()) PagePackets.sendSelect(row);
        click();
    }

    private void slotClicked(PyramidPane.Slot slot, int button) {
        String part = PyramidPane.partIn(draft.page(), slot);
        if (PyramidPane.layerRefusal(player(), draft, slot.kind()) != null) return;
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            if (part == null || slot.kind() == PartPicker.Kind.FORM) return;
            if (slot.kind() == PartPicker.Kind.HEART) draft.heart(slot.index(), null);
            if (slot.kind() == PartPicker.Kind.TOP) draft.top(slot.index(), null);
            edited(true);
            click();
            return;
        }
        boolean removable = part != null && slot.kind() != PartPicker.Kind.FORM;
        picker.open(slot.kind(), PyramidPane.freeIndex(draft.page(), slot), removable, left + RIGHT_X, top + LOWER_Y, RIGHT_W, HEIGHT - LOWER_Y - 6);
        click();
    }

    private void choose(PartPicker.Cell cell) {
        PartPicker.Kind kind = picker.kind();
        if (kind == null || cell.id() != null && picker.refusal(player(), draft, cell.id()) != null) return;
        switch (kind) {
            case FORM -> draft.form(cell.id());
            case HEART -> draft.heart(picker.slot(), cell.id());
            case TOP -> draft.top(picker.slot(), cell.id());
        }
        picker.close();
        edited(true);
        click();
    }

    private void clear() {
        if (ticks >= clearArmedUntil) {
            clearArmedUntil = ticks + CLEAR_ARM_TICKS;
            return;
        }
        clearArmedUntil = 0;
        draft.clear();
        picker.close();
        showName();
        edited(true);
    }

    private void tearOrPaste() {
        if (!contents().page(selected).isEmpty()) {
            TetradPackets.sendTear(hand, selected);
            draft.reset(contents().page(selected));
            pending = false;
            return;
        }
        int slot = tornPageSlot(player());
        if (slot >= 0) TetradPackets.sendPaste(hand, selected, slot);
    }

    private void rename(String name) {
        draft.name(name);
        edited(false);
    }

    private void edited(boolean now) {
        pending = true;
        idleTicks = now ? SAVE_DELAY : 0;
    }

    private void save() {
        if (draft.dirty() && draft.problem(player()).isPresent()) return;
        pending = false;
        if (!draft.dirty()) return;
        PagePackets.sendEdit(hand, selected, draft.page());
        sinceSent = 0;
    }

    private void showName() {
        nameBox.setResponder(value -> {
        });
        nameBox.setValue(draft.page().name());
        nameBox.setHint(autoName(draft.page()));
        nameBox.setResponder(this::rename);
    }

    private void refreshWidgets() {
        boolean armed = ticks < clearArmedUntil;
        clearButton.setMessage(Component.translatable(armed ? "quasium.tetrad.clear.sure" : "quasium.tetrad.clear"));
        clearButton.active = !draft.page().equals(SpellPage.EMPTY);
        String overflow = draft.page().overflow();
        overflowButton.setMessage(Component.translatable("quasium.tetrad.overflow",
                overflow.isEmpty() ? Component.translatable("quasium.tetrad.overflow.power") : PageLabels.top(overflow)));
        overflowButton.visible = !picker.isOpen();
        overflowButton.active = draft.overflowOptions().size() > 1;
        boolean written = !contents().page(selected).isEmpty();
        tearButton.setMessage(Component.translatable(written ? "quasium.tetrad.tear" : "quasium.tetrad.paste"));
        tearButton.setTooltip(Tooltip.create(Component.translatable(written ? "quasium.tetrad.tear.hint" : "quasium.tetrad.paste.hint")));
        tearButton.active = written || tornPageSlot(player()) >= 0;
        if (copyButton != null) {
            ItemStack small = TetradPackets.smallTetrad(player());
            copyButton.active = TetradPackets.firstFree(contents()) >= 0 && !small.isEmpty()
                    && SpellTetradItem.contents(small).pages().stream().anyMatch(page -> !page.isEmpty());
        }
    }

    private static Component overflowHint() {
        return Component.translatable("quasium.tetrad.overflow.hint", PageConfig.OVERFLOW_STEP_ML.getAsInt(),
                PageConfig.OVERFLOW_STEP_PCT.getAsInt(), PageConfig.OVERFLOW_MAX_STEPS.getAsInt());
    }

    private static Component autoName(SpellPage page) {
        if (page.isEmpty()) return Component.translatable("quasium.tetrad.name");
        return PageLabels.of(PageCasting.label(page.named("")));
    }

    static int tornPageSlot(Player player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(PageRegistry.TORN_PAGE.get()) && !stack.getOrDefault(PageRegistry.PAGE.get(), SpellPage.EMPTY).isEmpty()) return slot;
        }
        return -1;
    }

    private TetradContents contents() {
        return SpellTetradItem.contents(player().getItemInHand(hand));
    }

    private Player player() {
        return minecraft.player;
    }

    private void click() {
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.4f, 0.5f));
    }
}
