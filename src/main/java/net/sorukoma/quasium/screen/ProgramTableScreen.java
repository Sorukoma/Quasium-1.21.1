package net.sorukoma.quasium.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.sorukoma.quasium.Config;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.golem.program.BlockKind;
import net.sorukoma.quasium.golem.program.Check;
import net.sorukoma.quasium.golem.program.Program;
import net.sorukoma.quasium.golem.program.ProgramBlock;
import net.sorukoma.quasium.golem.program.ProgramBlocks;
import net.sorukoma.quasium.golem.program.ProgramEdit;
import net.sorukoma.quasium.golem.program.ProgramPath;
import net.sorukoma.quasium.golem.program.ProgramTableBlockEntity;
import net.sorukoma.quasium.golem.program.ProgramUnlocks;
import net.sorukoma.quasium.screen.program.BlockDrag;
import net.sorukoma.quasium.screen.program.BlockPainter;
import net.sorukoma.quasium.screen.program.PalettePane;
import net.sorukoma.quasium.screen.program.Part;
import net.sorukoma.quasium.screen.program.Placed;
import net.sorukoma.quasium.screen.program.ProgramPane;
import net.sorukoma.quasium.screen.program.ProgramTips;
import net.sorukoma.quasium.screen.program.Spot;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProgramTableScreen extends AbstractContainerScreen<ProgramTableMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "textures/gui/program_table.png");
    private static final int SHEET_WIDTH = 512;
    private static final int SHEET_HEIGHT = 256;
    private static final int LABEL = 0x404040;
    private static final int STATUS_RIGHT = ProgramPane.LEFT + ProgramPane.WIDTH - 1;
    private static final int BUTTON_Y = 211;
    private static final int BUTTON_H = 18;
    private static final int SAVE_X = 196;
    private static final int LOAD_X = 264;
    private static final int BUTTON_W = 64;
    private static final int SCROLL_STEP = 14;
    private static final int EDGE_ZONE = 12;
    private static final int EDGE_SPEED = 6;
    private static final int PREDICTION_TICKS = 20;
    private static final int TIP_WIDTH = 190;

    @Nullable
    private PalettePane palette;
    @Nullable
    private ProgramPane program;
    @Nullable
    private BlockPainter painter;
    private ItemStack blankCard = ItemStack.EMPTY;
    private Button save;
    private Button load;
    @Nullable
    private BlockDrag drag;
    @Nullable
    private Program predicted;
    @Nullable
    private Program predictedFrom;
    private int predictedAge;

    public ProgramTableScreen(ProgramTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 420;
        imageHeight = 236;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        if (palette == null) {
            palette = new PalettePane(font);
            program = new ProgramPane(font);
            painter = new BlockPainter(font, menu);
            blankCard = new ItemStack(ProgramBlocks.PROGRAM_CARD.get());
        }
        palette.place(leftPos, topPos);
        program.place(leftPos, topPos);
        save = addRenderableWidget(Button.builder(Component.translatable("quasium.program.ui.save"), button -> send(ProgramEdit.saveCard(), Optional.empty()))
                .bounds(leftPos + SAVE_X, topPos + BUTTON_Y, BUTTON_W, BUTTON_H)
                .tooltip(Tooltip.create(Component.translatable("quasium.program.ui.save_desc"))).build());
        load = addRenderableWidget(Button.builder(Component.translatable("quasium.program.ui.load"), button -> send(ProgramEdit.loadCard(), Optional.empty()))
                .bounds(leftPos + LOAD_X, topPos + BUTTON_Y, BUTTON_W, BUTTON_H)
                .tooltip(Tooltip.create(Component.translatable("quasium.program.ui.load_desc"))).build());
        containerTick();
    }

    public ProgramPane programPane() {
        return program;
    }

    public PalettePane palettePane() {
        return palette;
    }

    private Program serverProgram() {
        return menu.program(minecraft.level);
    }

    private Program shown() {
        Program server = serverProgram();
        if (predicted != null && server != predictedFrom) predicted = null;
        return predicted != null ? predicted : server;
    }

    private void send(ProgramEdit edit, Optional<Program> prediction) {
        PacketDistributor.sendToServer(new ProgramEditPacket(menu.containerId, edit));
        if (prediction.isEmpty()) return;
        Program server = serverProgram();
        if (predicted == null || predictedFrom != server) predictedFrom = server;
        predicted = prediction.get();
        predictedAge = 0;
    }

    @Override
    protected void containerTick() {
        if (predicted != null && ++predictedAge > PREDICTION_TICKS) predicted = null;
        if (drag != null && drag.started()) program.scrollNearEdge(drag.mouseX(), drag.mouseY(), EDGE_ZONE, EDGE_SPEED);
        ItemStack card = menu.card();
        save.active = !card.isEmpty() && !shown().isEmpty();
        Program stored = card.get(ProgramBlocks.PROGRAM.get());
        load.active = stored != null && !stored.isEmpty();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        program.update(shown(), drag == null ? null : drag.lifted());
        super.render(graphics, mouseX, mouseY, partialTick);
        if (drag != null && drag.started()) {
            renderDrag(graphics);
            return;
        }
        List<Component> tips = tips(mouseX, mouseY);
        if (!tips.isEmpty()) {
            graphics.renderTooltip(font, wrap(tips), mouseX, mouseY);
            return;
        }
        renderTooltip(graphics, mouseX, mouseY);
        if (hoveredSlot == menu.getSlot(ProgramTableMenu.CARD_SLOT) && !hoveredSlot.hasItem() && menu.getCarried().isEmpty()) {
            graphics.renderTooltip(font, font.split(Component.translatable("quasium.program.ui.card"), TIP_WIDTH), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, SHEET_WIDTH, SHEET_HEIGHT);
        boolean dragging = drag != null && drag.started();
        palette.render(graphics, painter, minecraft.player, dragging ? null : palette.hit(mouseX, mouseY));
        boolean live = !dragging && predicted == null;
        program.render(graphics, painter, dragging ? null : program.hit(mouseX, mouseY), live ? menu.runningMask() : 0L,
                dragging ? target() : null, !dragging);
        if (!menu.getSlot(ProgramTableMenu.CARD_SLOT).hasItem()) ghostCard(graphics);
    }

    private void ghostCard(GuiGraphics graphics) {
        int x = leftPos + ProgramTableMenu.CARD_X;
        int y = topPos + ProgramTableMenu.CARD_Y;
        graphics.renderFakeItem(blankCard, x, y);
        graphics.fill(RenderType.guiOverlay(), x, y, x + 16, y + 16, 0x908B8B8B);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        Component status = Component.translatable("quasium.program.ui.status", menu.golems(), shown().size(), Config.PROGRAM_MAX_BLOCKS.get());
        graphics.drawString(font, status, STATUS_RIGHT - font.width(status), titleLabelY, LABEL, false);
    }

    private void renderDrag(GuiGraphics graphics) {
        boolean doomed = drag.from() != null && !program.contains(drag.mouseX(), drag.mouseY());
        int x = drag.ghostX();
        int y = drag.ghostY();
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 400);
        Placed top = drag.ghost().placed().getFirst();
        graphics.fill(x + 2, y + 2, x + 2 + top.width(), y + 2 + top.height(), 0x50000000);
        for (Placed placed : drag.ghost().placed()) painter.paint(graphics, placed, x, y, doomed ? BlockPainter.Look.DOOMED : BlockPainter.Look.NORMAL, null, false);
        ProgramPane.Target target = target();
        Component note = doomed ? Component.translatable("quasium.program.ui.remove")
                : target != null && !target.valid() ? Component.translatable("quasium.program.ui.too_big", Config.PROGRAM_MAX_BLOCKS.get(),
                Config.PROGRAM_MAX_NESTING.get()) : null;
        if (note != null) graphics.drawString(font, note, x, y + top.height() + 3, doomed ? 0xFFFF7060 : 0xFFFFD0C8, true);
        graphics.pose().popPose();
    }

    @Nullable
    private ProgramPane.Target target() {
        if (drag == null || !drag.started() || !program.contains(drag.mouseX(), drag.mouseY())) return null;
        program.update(shown(), drag.lifted());
        Spot spot = program.nearest(drag.ghostX(), drag.ghostY());
        return spot == null ? null : new ProgramPane.Target(spot, result(spot).isPresent());
    }

    private Optional<Program> result(Spot spot) {
        Program shown = shown();
        Optional<Program> result = drag.from() == null ? shown.insert(spot.path(), ProgramBlock.of(drag.block().kind()))
                : shown.move(drag.from(), spot.beforeRemoving(drag.from()));
        return result.filter(ProgramTableBlockEntity::fits);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (drag != null) return true;
        if (program.contains(mouseX, mouseY)) {
            ProgramPane.Hit hit = program.hit(mouseX, mouseY);
            if (hit != null) clickProgram(hit, mouseX, mouseY, button);
            return true;
        }
        if (palette.contains(mouseX, mouseY)) {
            PalettePane.Entry entry = palette.hit(mouseX, mouseY);
            if (entry != null && button == 0 && ProgramUnlocks.mayUse(minecraft.player, entry.kind())) {
                drag = BlockDrag.fromPalette(entry.kind(), font, palette.screenX(entry), palette.screenY(entry), mouseX, mouseY);
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void clickProgram(ProgramPane.Hit hit, double mouseX, double mouseY, int button) {
        Placed placed = hit.placed();
        Part part = hit.part();
        if (part == null) {
            if (button == 0) drag = BlockDrag.fromProgram(placed, font, program.screenX(placed), program.screenY(placed), mouseX, mouseY);
            return;
        }
        boolean left = button == 0;
        switch (part.kind()) {
            case SOCKET -> place(placed, part.corner(), left);
            case FILTER -> filter(placed, left);
            case NUMBER -> number(placed.path(), left ? step() : -step());
            case CHECK -> check(placed.path(), left ? 1 : -1);
            default -> {
            }
        }
    }

    private void place(Placed placed, int corner, boolean left) {
        ItemStack carried = menu.getCarried();
        boolean marker = carried.is(ProgramBlocks.PLACE_MARKER.get()) && carried.has(ProgramBlocks.MARKED_PLACE.get());
        boolean clear = !left && carried.isEmpty() && placed.block().place(corner).isPresent();
        if (left && marker || clear) send(ProgramEdit.place(placed.path(), corner), Optional.empty());
    }

    private void filter(Placed placed, boolean left) {
        ItemStack carried = menu.getCarried();
        boolean clear = !left && carried.isEmpty() && !placed.block().filter().isEmpty();
        if (left && !carried.isEmpty() || clear) send(ProgramEdit.filter(placed.path()), Optional.empty());
    }

    private static int step() {
        return hasShiftDown() ? 10 : 1;
    }

    private void number(ProgramPath path, int delta) {
        Program shown = shown();
        ProgramBlock block = shown.at(path);
        if (block == null) return;
        BlockKind.Number range = block.kind().number();
        int value = Mth.clamp(block.number() + delta, range.min, range.max);
        if (value == block.number()) return;
        send(ProgramEdit.number(path, value), shown.replace(path, old -> old.withNumber(value)));
    }

    private void check(ProgramPath path, int direction) {
        Program shown = shown();
        ProgramBlock block = shown.at(path);
        if (block == null) return;
        int count = Check.values().length;
        for (int shift = 1; shift < count; shift++) {
            Check next = Check.values()[Math.floorMod(block.check().ordinal() + direction * shift, count)];
            if (!ProgramUnlocks.mayUse(minecraft.player, next)) continue;
            send(ProgramEdit.check(path, next), shown.replace(path, old -> old.withCheck(next)));
            return;
        }
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (drag == null) return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        drag.moveTo(mouseX, mouseY);
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (drag == null) return super.mouseReleased(mouseX, mouseY, button);
        drag.moveTo(mouseX, mouseY);
        if (drag.started()) drop();
        drag = null;
        return true;
    }

    private void drop() {
        ProgramPath from = drag.from();
        if (from != null && !stillThere(from, drag.block())) return;
        if (from != null && !program.contains(drag.mouseX(), drag.mouseY())) {
            send(ProgramEdit.remove(from), shown().remove(from));
            return;
        }
        ProgramPane.Target target = target();
        if (target == null || !target.valid()) return;
        Spot spot = target.spot();
        if (from == null) send(ProgramEdit.add(spot.path(), drag.block().kind()), result(spot));
        else if (!spot.path().equals(from)) send(ProgramEdit.move(from, spot.beforeRemoving(from)), result(spot));
    }

    private boolean stillThere(ProgramPath path, ProgramBlock lifted) {
        ProgramBlock now = shown().at(path);
        return now != null && now.kind() == lifted.kind() && now.size() == lifted.size();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (program.contains(mouseX, mouseY)) {
            ProgramPane.Hit hit = program.hit(mouseX, mouseY);
            if (drag == null && hit != null && hit.part() != null && hit.part().kind() == Part.Kind.NUMBER) {
                number(hit.placed().path(), (int) Math.signum(scrollY) * step());
            } else {
                program.scrollBy(-scrollY * SCROLL_STEP);
            }
            return true;
        }
        if (palette.contains(mouseX, mouseY)) {
            palette.scrollBy(-scrollY * SCROLL_STEP);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private List<Component> tips(int mouseX, int mouseY) {
        PalettePane.Entry entry = palette.hit(mouseX, mouseY);
        if (entry != null) return ProgramTips.palette(entry.kind(), minecraft.player);
        ProgramPane.Hit hit = program.hit(mouseX, mouseY);
        if (hit == null) return List.of();
        if (hit.part() != null) return ProgramTips.part(hit.placed().block(), hit.part(), menu, minecraft.player);
        return ProgramTips.block(hit.placed().block(), running(hit.placed()));
    }

    private boolean running(Placed placed) {
        if (predicted != null) return false;
        int index = serverProgram().paths().indexOf(placed.path());
        return index >= 0 && index < Long.SIZE && (menu.runningMask() >>> index & 1) != 0;
    }

    private List<FormattedCharSequence> wrap(List<Component> lines) {
        List<FormattedCharSequence> wrapped = new ArrayList<>();
        for (Component line : lines) wrapped.addAll(font.split(line, TIP_WIDTH));
        return wrapped;
    }
}
