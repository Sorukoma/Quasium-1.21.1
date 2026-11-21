package net.sorukoma.quasium.client.hint;

import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.sorukoma.quasium.hint.HintScene;
import net.sorukoma.quasium.progression.ConfigArgs;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class HintScreen extends Screen {
    private static final int SCENE_TOP = 24;
    private static final int SCENE_BOTTOM = 60;
    private static final int SCENE_SIDE = 16;
    private static final int TITLE_WIDTH = 180;
    private static final int CAPTION_WIDTH = 180;
    private static final int FINISH_WARMUP = 30;
    private static final float CHASE = 0.1f;
    private static final float FADE_TICKS = 5f;
    private static final float DRAG_DEGREES = 1.5f;

    private final ResourceLocation id;
    private final HintScene scene;
    @Nullable
    private final Screen parent;
    private final ItemStack subject;
    private final HintStage stage;
    private final int[] starts;
    private final List<HintButton> buttons = new ArrayList<>();
    private int step;
    private int stepTicks;
    private int ticks;
    private boolean playing = true;
    private boolean identify;
    private int finishWarmup;
    private float flash;
    private float flashBefore;
    private float fadeIn;
    private float fadeInBefore;
    private float dragYaw;
    private boolean dragging;
    private int titleWidth;
    private int titleHeight;
    private Matrix4f view = new Matrix4f();
    private HintButton prev;
    private HintButton next;
    private HintButton identifyButton;
    private HintProgressBar bar;

    HintScreen(ResourceLocation id, HintScene scene, @Nullable Screen parent, ItemStack subject) {
        super(Component.translatable(HintScene.titleKey(id)));
        this.id = id;
        this.scene = scene;
        this.parent = parent;
        this.subject = subject;
        this.stage = new HintStage(scene);
        this.starts = new int[scene.steps().size() + 1];
        for (int index = 0; index < scene.steps().size(); index++) starts[index + 1] = starts[index] + scene.steps().get(index).duration();
        stage.show(0);
    }

    public ResourceLocation id() {
        return id;
    }

    @Override
    protected void init() {
        buttons.clear();
        titleWidth = Math.min(font.width(title), TITLE_WIDTH);
        titleHeight = font.wordWrapHeight(title, TITLE_WIDTH);
        Options options = minecraft.options;
        int x = (width - HintButton.SIZE) / 2;
        int y = height - 51;
        int barX = width / 2 - 110;
        bar = addRenderableWidget(new HintProgressBar(barX, y + 24, width - 2 * barX, starts, this::seek));
        identifyButton = button(x - 86, y, "identify", options.keyDrop, () -> {
            identify = !identify;
            syncButtons();
        });
        prev = button(x - 28, y, "prev", options.keyLeft, () -> go(step - 1));
        button(x, y, "close", options.keyInventory, this::onClose);
        next = button(x + 28, y, "next", options.keyRight, () -> go(step + 1));
        button(x + 86, y, "replay", options.keyDown, () -> go(0));
        syncButtons();
    }

    private HintButton button(int x, int y, String name, KeyMapping shortcut, Runnable action) {
        HintButton button = addRenderableWidget(new HintButton(x, y, name, shortcut, height - 16, action));
        buttons.add(button);
        return button;
    }

    private void syncButtons() {
        if (prev == null) return;
        prev.visible = step > 0;
        next.visible = step < lastStep();
        identifyButton.light(identify);
    }

    private int lastStep() {
        return scene.steps().size() - 1;
    }

    private HintScene.Step current() {
        return scene.steps().get(step);
    }

    private int time() {
        return starts[step] + Math.min(stepTicks, current().duration());
    }

    private void go(int target) {
        step = Mth.clamp(target, 0, lastStep());
        stepTicks = 0;
        playing = true;
        identify = false;
        finishWarmup = 0;
        stage.show(step);
        syncButtons();
    }

    private void seek(int target) {
        if (target <= lastStep()) {
            go(target);
            return;
        }
        go(lastStep());
        stepTicks = current().duration();
        finish();
    }

    private void finish() {
        playing = false;
        finishWarmup = FINISH_WARMUP;
    }

    @Override
    public void tick() {
        fadeInBefore = fadeIn;
        fadeIn += (1f - fadeIn) * CHASE;
        flashBefore = flash;
        flash -= flash * CHASE;
        if (finishWarmup > 0 && --finishWarmup == 0) flash = flashBefore = 1f;
        buttons.forEach(HintButton::tick);
        if (!identify) advance();
        bar.tick(time());
    }

    private void advance() {
        ticks++;
        stepTicks++;
        if (!playing || stepTicks < current().duration()) return;
        if (step < lastStep()) {
            go(step + 1);
        } else {
            finish();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        float frozen = identify ? 0f : partialTick;
        renderMenuBackground(graphics);
        renderBackground(graphics, mouseX, mouseY, partialTick);
        float sceneHeight = height - SCENE_TOP - SCENE_BOTTOM;
        float scale = HintStageRenderer.scale(stage.bounds(), width - 2 * SCENE_SIDE, sceneHeight);
        float fade = captionFade(frozen);
        view = HintStageRenderer.render(graphics, stage, current(), width / 2f, SCENE_TOP + sceneHeight / 2f, scale,
                HintStageRenderer.START_YAW + dragYaw, stepTicks + frozen, ticks + frozen, fade, Mth.lerp(partialTick, flashBefore, flash));
        title(graphics, Mth.lerp(partialTick, fadeInBefore, fadeIn));
        if (!identify) caption(graphics, fade);
        HintTheme.navigationArrows(graphics, width / 2, height - 51);
        for (Renderable renderable : renderables) renderable.render(graphics, mouseX, mouseY, partialTick);
        if (identify) identifyTooltip(graphics, mouseX, mouseY);
    }

    private float captionFade(float partialTick) {
        float shown = stepTicks + partialTick;
        float fade = shown / FADE_TICKS;
        if (playing && step < lastStep()) fade = Math.min(fade, (current().duration() - shown) / FADE_TICKS);
        fade = Mth.clamp(fade, 0f, 1f);
        return fade * fade;
    }

    private void title(GuiGraphics graphics, float fade) {
        int streak = 26 + titleHeight;
        HintTheme.streak(graphics, 0, 55, 19 + streak / 2, streak, (int) ((70 + titleWidth) * fade));
        HintTheme.streak(graphics, 180, 55, 19 + streak / 2, streak, (int) (30 * fade));
        HintTheme.box(graphics, 21, 21, 30, 30, HintTheme.FLAT, HintTheme.IDLE_TOP, HintTheme.IDLE_BOTTOM);
        graphics.pose().pushPose();
        graphics.pose().translate(20, 20, 0);
        graphics.pose().scale(2, 2, 2);
        graphics.renderItem(subject, 0, 0);
        graphics.pose().popPose();
        graphics.drawString(font, Component.translatable("quasium.hint.pondering"), 59, 25, HintTheme.TEXT_DARKER, false);
        int color = HintTheme.alpha(HintTheme.TEXT, Math.max(fade, 5 / 255f));
        int lineY = 39;
        for (FormattedCharSequence line : font.split(title, TITLE_WIDTH)) {
            graphics.drawString(font, line, 59, lineY, color, false);
            lineY += font.lineHeight;
        }
    }

    private void caption(GuiGraphics graphics, float fade) {
        if (fade < 1 / 16f) return;
        Vector3f anchor = HintStageRenderer.anchor(view, current().highlight());
        float anchorX = anchor == null ? width / 2f : anchor.x;
        float anchorY = anchor == null ? (height - 200) / 2f - 8 : anchor.y;
        float spread = (height / 2f - anchorY - 10) / 100f;
        int boxX = (int) (width * Mth.lerp(spread * spread, 0.75f, 0.625f));
        if (anchor != null) boxX = Math.min(boxX, (int) anchorX + 50);
        List<FormattedCharSequence> lines = font.split(caption(step), Math.min(width - boxX, CAPTION_WIDTH));
        int boxWidth = lines.stream().mapToInt(font::width).max().orElse(0);
        int boxHeight = lines.size() * font.lineHeight;
        int top = Mth.clamp((int) anchorY, 4, height - SCENE_BOTTOM - boxHeight - 4);
        if (boxX - 14 < 59 + titleWidth && top - 1 < 45 + titleHeight) top = 48 + titleHeight;
        HintTheme.box(graphics, boxX - 10, top + 3, boxWidth, boxHeight - 1, HintTheme.FLAT, HintTheme.CAPTION_TOP, HintTheme.CAPTION_BOTTOM);
        if (anchor != null) connector(graphics, anchorX, top, anchorX + (boxX - anchorX) * fade);
        int color = HintTheme.alpha(HintTheme.CAPTION_TEXT, Math.max(fade, 5 / 255f));
        for (int index = 0; index < lines.size(); index++) {
            graphics.drawString(font, lines.get(index), boxX - 10, top + 3 + index * font.lineHeight, color, false);
        }
    }

    private void connector(GuiGraphics graphics, float fromX, int y, float toX) {
        graphics.pose().pushPose();
        graphics.pose().translate(Math.min(fromX, toX), y, 0);
        graphics.pose().scale(Math.abs(toX - fromX), 1, 1);
        graphics.fill(0, 0, 1, 1, HintTheme.CAPTION_TEXT);
        graphics.fillGradient(0, 1, 1, 2, HintTheme.CAPTION_SHADOW_TOP, HintTheme.CAPTION_SHADOW_BOTTOM);
        graphics.pose().popPose();
    }

    private void identifyTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (mouseY >= height - 80 || children().stream().anyMatch(child -> child.isMouseOver(mouseX, mouseY))) return;
        BlockPos pos = HintStageRenderer.pick(stage, view, mouseX, mouseY);
        BlockState state = pos == null ? null : stage.blocks().get(pos);
        if (state == null) {
            Component key = minecraft.options.keyDrop.getTranslatedKeyMessage().copy().withStyle(ChatFormatting.WHITE);
            Component text = Component.translatable("quasium.hint.identify_mode", key).withStyle(ChatFormatting.GRAY);
            graphics.renderTooltip(font, font.split(text, width / 3), mouseX, mouseY);
            return;
        }
        ItemStack stack = new ItemStack(state.getBlock());
        if (stack.isEmpty()) {
            graphics.renderTooltip(font, state.getBlock().getName(), mouseX, mouseY);
        } else {
            graphics.renderTooltip(font, stack, mouseX, mouseY);
        }
    }

    private Component caption(int index) {
        List<String> args = scene.steps().get(index).args();
        return Component.translatable(HintScene.captionKey(id, index), args.stream().map(ConfigArgs::format).toArray());
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (super.keyPressed(key, scan, modifiers)) return true;
        for (HintButton button : buttons) {
            if (!button.matches(key, scan)) continue;
            button.onPress();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        dragging = button == 0;
        return dragging;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!dragging) return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        dragYaw += (float) dragX * DRAG_DEGREES;
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int target = Mth.clamp(step + (int) Math.signum(scrollY), 0, lastStep());
        if (target == step) return false;
        go(target);
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
