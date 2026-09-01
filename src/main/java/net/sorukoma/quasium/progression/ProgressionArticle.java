package net.sorukoma.quasium.progression;

import java.util.List;

public class ProgressionArticle {
    private String title;
    private List<ProgressionPage> pages;
    private String unlockCondition;
    private String hint;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<ProgressionPage> getPages() {
        return pages;
    }

    public void setPages(List<ProgressionPage> pages) {
        this.pages = pages;
    }

    public String getUnlockCondition() {
        return unlockCondition;
    }

    public void setUnlockCondition(String unlockCondition) {
        this.unlockCondition = unlockCondition;
    }

    public String getHint() {
        return hint;
    }

    public void setHint(String hint) {
        this.hint = hint;
    }
}
