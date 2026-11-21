package net.sorukoma.quasium.client.gui.book;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.resources.ResourceLocation;
import net.sorukoma.quasium.mind.ClientMind;
import net.sorukoma.quasium.progression.ProgressionArticle;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class BookUnlocks implements ClientAdvancements.Listener {
    private final Map<ResourceLocation, AdvancementProgress> progress = new HashMap<>();
    private final Runnable onChange;

    public BookUnlocks(Runnable onChange) {
        this.onChange = onChange;
    }

    public void attach() {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null) connection.getAdvancements().setListener(this);
    }

    public void detach() {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null) connection.getAdvancements().setListener(null);
    }

    public boolean isUnlocked(ProgressionArticle article) {
        if (article.isResearch()) return ClientMind.researched(article.getId());
        if (!article.hasUnlockCondition()) return true;
        AdvancementProgress done = progress.get(ResourceLocation.tryParse(article.getUnlockCondition()));
        return done != null && done.isDone();
    }

    public Optional<DisplayInfo> unlockAdvancement(ProgressionArticle article) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        ResourceLocation id = article.hasUnlockCondition() ? ResourceLocation.tryParse(article.getUnlockCondition()) : null;
        if (connection == null || id == null) return Optional.empty();
        AdvancementHolder holder = connection.getAdvancements().get(id);
        return holder == null ? Optional.empty() : holder.value().display();
    }

    @Override
    public void onUpdateAdvancementProgress(AdvancementNode node, AdvancementProgress update) {
        progress.put(node.holder().id(), update);
        onChange.run();
    }

    @Override
    public void onAdvancementsCleared() {
        progress.clear();
        onChange.run();
    }

    @Override
    public void onSelectedTabChanged(@Nullable AdvancementHolder holder) {
    }

    @Override
    public void onAddAdvancementRoot(AdvancementNode node) {
    }

    @Override
    public void onRemoveAdvancementRoot(AdvancementNode node) {
    }

    @Override
    public void onAddAdvancementTask(AdvancementNode node) {
    }

    @Override
    public void onRemoveAdvancementTask(AdvancementNode node) {
    }
}
