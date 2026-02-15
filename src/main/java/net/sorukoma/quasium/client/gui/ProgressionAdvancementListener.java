package net.sorukoma.quasium.client.gui;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@OnlyIn(Dist.CLIENT)
public class ProgressionAdvancementListener implements ClientAdvancements.Listener {

    private final Map<AdvancementHolder, AdvancementProgress> advancementProgressMap = new ConcurrentHashMap<>();

    public ProgressionAdvancementListener() {
        // Конструктор
    }

    @Override
    public void onUpdateAdvancementProgress(AdvancementNode advancementNode, AdvancementProgress progress) {
        advancementProgressMap.put(advancementNode.holder(), progress);
    }

    @Override
    public void onSelectedTabChanged(@Nullable AdvancementHolder advancementHolder) {
        // Нам не нужно обрабатывать изменение выбранной вкладки для этой логики
    }

    @Override
    public void onAddAdvancementRoot(AdvancementNode advancement) {
        // Пустая реализация
    }

    @Override
    public void onRemoveAdvancementRoot(AdvancementNode advancement) {
        // Пустая реализация
    }

    @Override
    public void onAddAdvancementTask(AdvancementNode advancement) {
        // Пустая реализация
    }

    @Override
    public void onRemoveAdvancementTask(AdvancementNode advancement) {
        advancementProgressMap.remove(advancement.holder()); // Удаляем прогресс, если достижение удалено
    }

    @Override
    public void onAdvancementsCleared() {
        advancementProgressMap.clear();
    }

    /**
     * Получает прогресс для данного AdvancementHolder.
     * @param advancementHolder Объект AdvancementHolder.
     * @return Прогресс достижения, или null, если не найден.
     */
    @Nullable
    public AdvancementProgress getProgress(AdvancementHolder advancementHolder) {
        return advancementProgressMap.get(advancementHolder);
    }
}
