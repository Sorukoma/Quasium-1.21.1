package net.sorukoma.quasium.progression.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.progression.ProgressionArticle;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class ProgressionArticleManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String FOLDER_NAME = "progression_articles";

    private Map<ResourceLocation, ProgressionArticle> articles = new HashMap<>();

    public ProgressionArticleManager() {
        super(GSON, FOLDER_NAME);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        articles.clear();
        for (Map.Entry<ResourceLocation, JsonElement> entry : pObject.entrySet()) {
            ResourceLocation id = entry.getKey();
            try {
                ProgressionArticle article = GSON.fromJson(entry.getValue(), ProgressionArticle.class);
                articles.put(id, article);
                LOGGER.info("Loaded progression article: {}", id);
            } catch (Exception e) {
                LOGGER.error("Failed to load progression article: {}", id, e);
            }
        }
        LOGGER.info("Loaded {} progression articles.", articles.size());
    }

    public Map<ResourceLocation, ProgressionArticle> getArticles() {
        return articles;
    }
}
