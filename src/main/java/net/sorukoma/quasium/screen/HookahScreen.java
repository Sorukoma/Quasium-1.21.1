package net.sorukoma.quasium.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.component.ModDataComponents;
import net.sorukoma.quasium.network.PacketHookahPuffDataClientHandler;
import net.sorukoma.quasium.network.PacketHookahPuffDataClientHandler.ClientHookahData;
import net.sorukoma.quasium.block.entity.HookahBlockEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Класс экрана GUI для кальяна.
 *
 * ИСПРАВЛЕНО:
 *
 * #1 — Двойной вызов renderBackground():
 *   БЫЛО: render() вызывал renderBackground() вруч перед super.render(),
 *   но AbstractContainerScreen.render() УЖЕ вызывает renderBackground() внутри.
 *   Результат: двойное затемнение фона экрана.
 *   ИСПРАВЛЕНО: убран ручной вызов, оставлен только super.render().
 *
 * #2 — Двойный вызов renderTooltip():
 *   БЫЛО: render() вызывал renderTooltip() вруч, но super.render() уже вызывает его.
 *   ИСПРАВЛЕНО: кастомные тултипы перенесены в override renderTooltip(),
 *   который вызывается после super.renderTooltip().
 *
 * #3 — Ручной расчёт ( вместо leftPos/topPos:
 *   БЫЛО: renderBg() пересчитывал x = (width - imageWidth) / 2 вруч.
 *   ПОЧЕХУ: AbstractContainerScreen.init() УЖЕ рассчитывает leftPos и topPos.
 *   Ручной пересчёт мог лишним и потенциально некорректным при нестандартных размерах.
 *   ИСПРАВЛЕНО: использу this.leftPos и this.topPos.
 *
 * #4 — RenderSystem.setShaderColor() избыточен в 1.21.1:
 *   БЫЛО: renderBg() вызывал RenderSystem.setShaderColor(1,1,1,1).
 *   ПОЧЕХУ: в 1.21.1 GuiGraphics управляет цветом через буферизацию,
 *   setShaderColor() не нужен и может влиять на последующие blit-вызовы.
 *   ИСПРАВЛЕНО: вызов убран.
 *
 * #5 — Текстура полностью opaque в зоне инвентаря (y84-166):
 *   Текстура из git HEAD — 256x256, 100% opaque во ВСЕЙ зоне инвентаря,
 *   включая внутри позиций слотов (0 прозрачных пикселей).
 *   В vanilla Minecraft текстура контейнера содержит серые границы слотов
 *   с прозрачными/полупрозрачными интерьерами, чтобы предметы были видны.
 *   ИСПРАВЛЕНО: drawAllSlotOutlines() рисует рамки 18x18 для ВСЕХ слотов
 *   (кальян + инвентарь игрока) поверх opaque-текстуры, используя SLOT_TEXTURE.
 *
 * #6 — Порядок слотов в HookahMenu:
 *   БЫЛО: слоты инвентаря игрока (0-35) ДО слотов кальяна (36-37).
 *   В vanilla convention контейнерные слоты идут ПЕРВЫМИ (нижние индексы).
 *   ИСПРАВЛЕНО: слоты кальяна (0-1) первыми, потом инвентарь (2-37).
 *   См. HookahMenu.quickMoveStack() обновлен.
 */
public class HookahScreen extends AbstractContainerScreen<HookahMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "textures/gui/hookah_gui.png");

    // SLOT_TEXTURE больше не нужен — рамки встроены в TEXTURE

    private static final int TICKS_PER_SECOND = 20;
    private static final int BAR_WIDTH = 5;
    private static final int BAR_HEIGHT = 16;

    private final int maxTobaccoWeight;

    private ClientHookahData hookahData;

    public HookahScreen(HookahMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.maxTobaccoWeight = HookahBlockEntity.MAX_TOBACCO_WEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = this.imageHeight - 94;
        this.titleLabelY = 6;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        // ИСПРАВЛЕНИЕ #3: использу leftPos/topPos вместо ручного пересчёта
 int x = this.leftPos;
        int y = this.topPos;

        // ИСПРАВЛЕНИЕ #4: setShaderColor убран — GuiGraphics управляет цветом

        // Основная текстура фона
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        // ИСПРАВЛЕНИЕ #5: текстура обновлена — теперь содержит vanilla-стиль
        // рамки слотов (скопировано из furnace.png для инвентаря + hookah slots).
        // drawAllSlotOutlines() убран — рамки уже встроены в текстуру.

        // Обновляем cached hookah data
        this.hookahData = PacketHookahPuffDataClientHandler.getClientHookahData(menu.blockEntity.getBlockPos());

        float totalHeat = hookahData != null ? hookahData.totalHeat() : 0f;
        int totalTobaccoWeight = hookahData != null ? hookahData.totalTobaccoWeight() : 0;

        // Шкала жара (max = 3.0 для махорки, но визуально нормируем к 4.0)
        int heatBarX = x + 80 + 20;
        int heatBarY = y + 17;
        guiGraphics.fill(heatBarX, heatBarY, heatBarX + BAR_WIDTH, heatBarY + BAR_HEIGHT, 0xFF8B8B8B);
        if (totalHeat > 0) {
            float maxVisualHeat = 4.0f;
            int filledHeight = (int) (Math.min(totalHeat / maxVisualHeat, 1.0f) * BAR_HEIGHT);
            guiGraphics.fill(heatBarX, heatBarY + BAR_HEIGHT - filledHeight, heatBarX + BAR_WIDTH, heatBarY + BAR_HEIGHT, 0xFFFFA500);
        }

        // Шкала содержимого (табак)
        int contentBarX = x + 80 + 20;
        int contentBarY = y + 53;
        guiGraphics.fill(contentBarX, contentBarY, contentBarX + BAR_WIDTH, contentBarY + BAR_HEIGHT, 0xFF8B8B8B);
        if (totalTobaccoWeight > 0) {
            int filledHeight = (int) ((float) totalTobaccoWeight / maxTobaccoWeight * BAR_HEIGHT);
            guiGraphics.fill(contentBarX, contentBarY + BAR_HEIGHT - filledHeight, contentBarX + BAR_WIDTH, contentBarY + BAR_HEIGHT, 0xFF00FF00);
        }
    }

    /**
     * ИСПРАВЛЕНИЕ #5: раньше рисовал рамки 18x18 отдельно для каждого слота,
     * что давало кривой не-vanilla вид. Теперь текстура hookah_gui.png содержит
     * встроенные vanilla-стиль рамки (инвентарь скопирован из furnace.png,
     * hookah slots используют тот же vanilla template). Этот метод удалён.
     */

    // ИСПРАВЛЕНИЕ #1: renderBackground() не вызывается вручную — super.render() вызывает его.
    // ИСПРАВЛЕНИЕ #2: renderTooltip() ДОЛЖЕН вызываться вручную после super.render()!
    //   AbstractContainerScreen.render() НЕ вызывает renderTooltip() внутри.
    //   Это НЕ баг — это design. Vanilla InventoryScreen тоже вызывает renderTooltip() вручную.
    //   Без этого вызова тултипы предметов и шкал не отображаются вообще.
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    /**
     * Кастомные тултипы для шкал прогресса (уголь, содержимое).
     * Вызывается после стандартных slot tooltips.
     */
    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Стандартные тултипы слотов (при наведении на слот с предметом)
        super.renderTooltip(guiGraphics, mouseX, mouseY);
        // Кастомные тултипы шкал
        renderBarTooltips(guiGraphics, mouseX, mouseY);
    }

    private void renderBarTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (hookahData == null) return;

        int x = this.leftPos;
        int y = this.topPos;

        float totalHeat = hookahData.totalHeat();
        int totalTobaccoWeight = hookahData.totalTobaccoWeight();
        ModDataComponents.HookahBowlContents bowlContents = hookahData.bowlContents();

        // Тултип шкалы содержимого
        int contentBarX = x + 80 + 20;
        int contentBarY = y + 53;
        if (mouseX >= contentBarX && mouseX < contentBarX + BAR_WIDTH
                && mouseY >= contentBarY && mouseY < contentBarY + BAR_HEIGHT) {
            List<Component> tooltipLines = new ArrayList<>();
            if (totalTobaccoWeight > 0) {
                Component tooltipText;
                if (bowlContents != null && !bowlContents.isCompatible()) {
                    tooltipText = Component.translatable("quasium.tooltip.incompatible_mix").withStyle(net.minecraft.ChatFormatting.RED);
                } else if (bowlContents != null && bowlContents.groups().size() > 1) {
                    StringBuilder sb = new StringBuilder();
                    Map<Integer, Integer> groupPercentages = bowlContents.getGroupPercentages();
                    for (int i = 0; i < bowlContents.groups().size(); i++) {
                        ModDataComponents.BowlTobaccoGroup group = bowlContents.groups().get(i);
                        Integer pct = groupPercentages.get(i);
                        if (pct == null) continue;
                        if (!sb.isEmpty()) sb.append(" / ");
                        if (group.isRegular()) {
                            sb.append(pct).append("% ")
                              .append(Component.translatable("quasium.flavor." + group.flavor()).getString());
                        } else {
                            sb.append(pct).append("% ")
                              .append(Component.translatable("quasium.base." + group.base()).getString());
                        }
                    }
                    tooltipText = Component.literal(sb.toString());
                } else if (bowlContents != null && bowlContents.groups().size() == 1) {
                    ModDataComponents.BowlTobaccoGroup group = bowlContents.groups().get(0);
                    if (group.isRegular()) {
                        tooltipText = Component.translatable("quasium.flavor." + group.flavor());
                    } else {
                        tooltipText = Component.translatable("quasium.base." + group.base());
                    }
                } else {
                    tooltipText = Component.translatable("gui.quasium.empty");
                }
                int tobaccoGrams = totalTobaccoWeight / HookahBlockEntity.TOBACCO_TICKS_PER_GRAM;
                float tobaccoSeconds = (float) totalTobaccoWeight / TICKS_PER_SECOND;
                tooltipLines.add(Component.translatable("quasium.gui.hookah.contents").append(": ").append(tooltipText));
                tooltipLines.add(Component.literal(String.format("%dg / %dg (~%.0fs)", tobaccoGrams,
                        HookahBlockEntity.MAX_TOBACCO_WEIGHT / HookahBlockEntity.TOBACCO_TICKS_PER_GRAM,
                        tobaccoSeconds)));
            } else {
                tooltipLines.add(Component.translatable("quasium.gui.hookah.contents").append(": ").append(Component.translatable("gui.quasium.empty")));
            }
            guiGraphics.renderTooltip(this.font, tooltipLines.stream().map(Component::getVisualOrderText).collect(Collectors.toList()), mouseX, mouseY);
        }

        // Тултип шкалы жара
        int heatBarX = x + 80 + 20;
        int heatBarY = y + 17;
        if (mouseX >= heatBarX && mouseX < heatBarX + BAR_WIDTH
                && mouseY >= heatBarY && mouseY < heatBarY + BAR_HEIGHT) {
            guiGraphics.renderTooltip(this.font, List.of(Component.translatable("quasium.gui.hookah.heat", String.format("%.2f", totalHeat))).stream().map(Component::getVisualOrderText).collect(Collectors.toList()), mouseX, mouseY);
        }
    }
}