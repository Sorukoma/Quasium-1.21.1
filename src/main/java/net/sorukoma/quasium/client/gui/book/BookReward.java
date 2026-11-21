package net.sorukoma.quasium.client.gui.book;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;
import net.sorukoma.quasium.client.ClientMagicData;
import net.sorukoma.quasium.client.ClientProgressionData;
import net.sorukoma.quasium.mind.MindConfig;
import net.sorukoma.quasium.network.ClaimBookRewardPacket;
import net.sorukoma.quasium.progression.BookRewards;
import net.sorukoma.quasium.progression.ConfigArgs;
import net.sorukoma.quasium.progression.ProgressionArticle;

public final class BookReward {
    private final Button button;
    private ProgressionArticle shown;
    private ResourceLocation claiming;
    private int shownInsight;
    private int center;

    public BookReward(int height) {
        button = Button.builder(Component.empty(), pressed -> claim()).bounds(0, 0, 40, height).build();
    }

    public Button button() {
        return button;
    }

    public void place(int x, int y, int width) {
        center = x + width / 2;
        button.setY(y);
    }

    public void show(ProgressionArticle article, boolean unlocked, boolean lastPage, Font font) {
        shown = article;
        Item item = article == null || !unlocked ? Items.AIR : BookRewards.rewardOf(article);
        button.visible = item != Items.AIR && lastPage;
        if (!button.visible) return;
        shownInsight = ClientMagicData.insight();
        boolean waiting = article.getId().equals(claiming);
        if (!ClientProgressionData.isClaimed(article.getId())) {
            label(Component.translatable("quasium.progression.reward_button", item.getDefaultInstance().getHoverName()), font);
            button.setTooltip(null);
            button.active = !waiting;
            return;
        }
        int price = ConfigArgs.synced(MindConfig.REWARD_COPY_INSIGHT);
        boolean affordable = shownInsight >= price;
        label(Component.translatable("quasium.progression.reward_copy", price), font);
        button.setTooltip(affordable ? null : Tooltip.create(Component.translatable("quasium.progression.reward_copy_short", price, shownInsight)));
        button.active = affordable && !waiting;
    }

    public void answered() {
        claiming = null;
    }

    public boolean insightChanged() {
        return button.visible && shownInsight != ClientMagicData.insight();
    }

    private void label(Component text, Font font) {
        button.setMessage(text);
        button.setWidth(font.width(text) + 10);
        button.setX(center - button.getWidth() / 2);
    }

    private void claim() {
        if (shown == null) return;
        PacketDistributor.sendToServer(new ClaimBookRewardPacket(shown.getId()));
        claiming = shown.getId();
        button.active = false;
    }
}
