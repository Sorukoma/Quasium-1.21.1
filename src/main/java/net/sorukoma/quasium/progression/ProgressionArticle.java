package net.sorukoma.quasium.progression;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class ProgressionArticle {
    private transient ResourceLocation id;
    private String title;
    private String hint;
    private String category;
    private int order;
    private String icon;
    private String unlockCondition;
    private String scene;
    private String reward;
    private boolean secret;
    private ResearchNode research;
    private List<ProgressionPage> pages = List.of();

    public ResourceLocation getId() {
        return id;
    }

    public void setId(ResourceLocation id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public String getHint() {
        return hint;
    }

    public String getCategory() {
        return category;
    }

    public int getOrder() {
        return order;
    }

    public String getIcon() {
        return icon;
    }

    public String getUnlockCondition() {
        return unlockCondition;
    }

    public String getScene() {
        return scene;
    }

    public String getReward() {
        return reward;
    }

    public boolean isSecret() {
        return secret;
    }

    public ResearchNode getResearch() {
        return research;
    }

    public boolean isResearch() {
        return research != null;
    }

    public List<ProgressionPage> getPages() {
        return pages == null ? List.of() : pages;
    }

    public boolean hasUnlockCondition() {
        return unlockCondition != null && !unlockCondition.isEmpty();
    }
}
