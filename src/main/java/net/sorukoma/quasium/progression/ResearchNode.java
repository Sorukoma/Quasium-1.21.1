package net.sorukoma.quasium.progression;

import java.util.List;

public class ResearchNode {
    private int tier = 1;
    private List<String> notes = List.of();
    private List<String> requires = List.of();
    private Integer kaif;
    private boolean lived;
    private String dream;
    private Integer insight;
    private Integer treatises;
    private String perk;
    private List<String> spells = List.of();
    private List<String> recipes = List.of();
    private Integer noteMl;
    private List<String> items = List.of();
    private List<String> gives = List.of();

    public int tier() {
        return Math.max(1, Math.min(4, tier));
    }

    public List<String> notes() {
        return notes == null ? List.of() : notes;
    }

    public List<String> requires() {
        return requires == null ? List.of() : requires;
    }

    public Integer kaif() {
        return kaif;
    }

    public boolean lived() {
        return lived;
    }

    public String dream() {
        return dream;
    }

    public Integer insight() {
        return insight;
    }

    public Integer treatises() {
        return treatises;
    }

    public String perk() {
        return perk;
    }

    public List<String> spells() {
        return spells == null ? List.of() : spells;
    }

    public List<String> recipes() {
        return recipes == null ? List.of() : recipes;
    }

    public Integer noteMl() {
        return noteMl;
    }

    public List<String> items() {
        return items == null ? List.of() : items;
    }

    public List<String> gives() {
        return gives == null ? List.of() : gives;
    }
}
