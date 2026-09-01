package net.sorukoma.quasium.hookah;

/**
 * Варианты шахты модульного кальяна. Отличаются только акцентным цветом,
 * геометрия и механика идентичны — любая шахта совместима с любой колбой.
 */
public enum HookahShaftVariant {
    WHITE("white"),
    BLACK("black"),
    RED("red"),
    BLUE("blue"),
    AMBER("amber");

    private final String name;

    HookahShaftVariant(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static HookahShaftVariant byName(String name) {
        for (HookahShaftVariant v : values()) {
            if (v.name.equals(name)) return v;
        }
        return WHITE;
    }
}
