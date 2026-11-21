package net.sorukoma.quasium.client;

import net.minecraft.resources.ResourceLocation;
import net.sorukoma.quasium.progression.ProgressionArticle;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class ClientProgressionData {
    private static Map<ResourceLocation, ProgressionArticle> articles = Map.of();
    private static Set<String> claimed = Set.of();

    public static void setArticles(Collection<ProgressionArticle> received) {
        Map<ResourceLocation, ProgressionArticle> byId = new LinkedHashMap<>();
        for (ProgressionArticle article : received) {
            byId.put(article.getId(), article);
        }
        articles = byId;
    }

    public static Map<ResourceLocation, ProgressionArticle> getArticles() {
        return articles;
    }

    public static void setClaimed(Collection<String> received) {
        claimed = Set.copyOf(received);
    }

    public static boolean isClaimed(ResourceLocation article) {
        return claimed.contains(article.toString());
    }
}
