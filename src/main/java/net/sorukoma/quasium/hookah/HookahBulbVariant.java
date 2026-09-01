package net.sorukoma.quasium.hookah;

/**
 * Варианты колбы модульного кальяна. Все колбы одинаковы по геометрии/механике
 * и отличаются только акцентным цветом текстуры, чтобы любую колбу можно было
 * комбинировать с любой шахтой.
 */
public enum HookahBulbVariant {
    WHITE("white"),
    BLACK("black"),
    RED("red"),
    BLUE("blue"),
    AMBER("amber");

    private final String name;

    HookahBulbVariant(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static HookahBulbVariant byName(String name) {
        for (HookahBulbVariant v : values()) {
            if (v.name.equals(name)) return v;
        }
        return WHITE;
    }
}
