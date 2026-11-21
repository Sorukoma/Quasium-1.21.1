package net.sorukoma.quasium.mind.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.sorukoma.quasium.client.ClientProgressionData;
import net.sorukoma.quasium.client.RecipeGate;
import net.sorukoma.quasium.client.gui.book.BookArt;
import net.sorukoma.quasium.mind.ClientMind;
import net.sorukoma.quasium.progression.ProgressionArticle;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@OnlyIn(Dist.CLIENT)
public final class ResearchLocks {
    public static final int RESERVED_HEIGHT = 22;
    private static final int UNMET = 0xFF9A2A22;
    private static final int MET = 0xFF2E6B2E;

    private ResearchLocks() {
    }

    public static boolean hides(ResourceLocation node) {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && RecipeGate.gating(player) && !ClientMind.researched(node);
    }

    public static Component needs(ResourceLocation node) {
        return Component.translatable("quasium.research.recipe_needs", title(node));
    }

    public static List<Component> tooltip(ResourceLocation node) {
        ProgressionArticle article = ClientProgressionData.getArticles().get(node);
        Component branch = article == null ? Component.literal(node.toString())
                : Component.translatable("quasium.progression.category." + article.getCategory());
        return List.of(needs(node), Component.translatable("quasium.research.recipe_needs_hint", branch));
    }

    public static List<Component> withTeacher(List<Component> lines, ResourceLocation recipe) {
        Optional<ProgressionArticle> teacher = ClientProgressionData.getArticles().values().stream()
                .filter(article -> article.isResearch() && article.getResearch().recipes().contains(recipe.toString())).findFirst();
        if (teacher.isEmpty()) return lines;
        List<Component> taught = new ArrayList<>(lines);
        taught.add(Component.translatable("quasium.research.recipe_sooner", Component.translatable(teacher.get().getTitle())));
        return taught;
    }

    public static void draw(GuiGraphics graphics, ResourceLocation node, int x, int y, int width) {
        Font font = Minecraft.getInstance().font;
        boolean met = ClientMind.researched(node);
        int textX = x;
        if (!met) {
            graphics.blitSprite(BookArt.LOCK, x, y - 2, BookArt.LOCK_W, BookArt.LOCK_H);
            textX += BookArt.LOCK_W + 3;
        }
        int lineY = y;
        for (FormattedCharSequence line : font.split(needs(node), width - (textX - x))) {
            graphics.drawString(font, line, textX, lineY, met ? MET : UNMET, false);
            lineY += font.lineHeight + 1;
        }
    }

    private static Component title(ResourceLocation node) {
        ProgressionArticle article = ClientProgressionData.getArticles().get(node);
        return article == null ? Component.literal(node.toString()) : Component.translatable(article.getTitle());
    }
}
