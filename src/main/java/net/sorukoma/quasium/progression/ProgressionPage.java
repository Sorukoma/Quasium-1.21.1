package net.sorukoma.quasium.progression;

import java.util.List;

public class ProgressionPage {
    private String text;
    private String recipe;
    private String anvil;
    private String ritual;
    private List<String> items = List.of();
    private List<String> args = List.of();

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getRecipe() {
        return recipe;
    }

    public String getAnvil() {
        return anvil;
    }

    public String getRitual() {
        return ritual;
    }

    public List<String> getItems() {
        return items == null ? List.of() : items;
    }

    public List<String> getArgs() {
        return args == null ? List.of() : args;
    }
}
