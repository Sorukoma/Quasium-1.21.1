package net.sorukoma.quasium.util;

import net.sorukoma.quasium.component.ModDataComponents;

import java.util.Map;

/**
 * Утилита маппинга флавор табака → RGB цвет частиц дыма.
 * Используется для цветного DUST-оверлея поверх стандартного белого дыма кальяна.
 */
public final class SmokeColorUtil {
    public static final int SICK = 0xB9C93A;

    private SmokeColorUtil() {}

    public static float sickness(float windowShare, int fromPct) {
        return Math.clamp((windowShare * 100f - fromPct) / (100f - fromPct), 0f, 1f);
    }

    public static int blend(int from, int to, float share) {
        int red = Math.round(((from >> 16) & 0xFF) * (1 - share) + ((to >> 16) & 0xFF) * share);
        int green = Math.round(((from >> 8) & 0xFF) * (1 - share) + ((to >> 8) & 0xFF) * share);
        int blue = Math.round((from & 0xFF) * (1 - share) + (to & 0xFF) * share);
        return (red << 16) | (green << 8) | blue;
    }

    /** Цвет дыма для конкретного флавора обычного табака. */
    public static int colorForFlavor(String flavor) {
        if (flavor == null) return 0x888888;
        return switch (flavor) {
            case "mint"       -> 0x66FF66; // зелёный
            case "lemon"      -> 0xFFF15A;
            case "lime"       -> 0x8CD63F;
            case "orange"     -> 0xFF9D2E;
            case "grapefruit" -> 0xFF6F61;
            case "berry"      -> 0xCC66FF; // фиолетовый
            case "ice"    -> 0x99DDFF;
            case "honey"  -> 0xFFAA22;
            case "nether" -> 0x992233;
            case "lavender" -> 0xB06CCF;
            case "cocoa" -> 0x6D3B25;
            case "apple" -> 0xD84A3A;
            case "watermelon" -> 0xF25C6B;
            case "grape" -> 0x7746B5;
            case "cherry" -> 0xB5163A;
            case "vanilla" -> 0xF1E0A6;
            case "coffee" -> 0x4B2E22;
            case "cinnamon" -> 0xB65A2A;
            case "coconut" -> 0xE8DFC8;
            case "rose" -> 0xF07FA5;
            case "anise" -> 0x9BCB75;
            default       -> 0xCCCCCC; // нейтральный светлый
        };
    }

    /** Цвет дыма для самозамеса по основе (base). */
    public static int colorForSelfMixedBase(String base) {
        if (base == null) return 0x888888;
        return switch (base) {
            case "tobacco"  -> 0x886644; // коричневый
            case "makhorka" -> 0xAA4444; // тёмно-красный
            default         -> 0x888888;
        };
    }

    /** Серый цвет для несовместимой смеси. */
    public static int incompatibleColor() {
        return 0x888888;
    }

    /**
     * Вычисляет доминирующий цвет дыма из содержимого чаши кальяна.
     * Если смесь несовместима — серый.
     * Если обычный табак — цвет доминирующего флавора (по проценту группы).
     * Если самозамес — цвет по основе.
     * Если несколько групп — цвет первой (доминирующей).
     */
    public static int colorForContents(ModDataComponents.HookahBowlContents contents) {
        if (contents == null) return 0xCCCCCC;
        if (!contents.isCompatible()) return incompatibleColor();

        var groups = contents.groups();
        if (groups.isEmpty()) return 0xCCCCCC;

        // Выбираем доминирующую группу по проценту
        Map<Integer, Integer> percentages = contents.getGroupPercentages();
        int bestIdx = 0;
        int bestPct = -1;
        for (int i = 0; i < groups.size(); i++) {
            Integer pct = percentages.get(i);
            if (pct != null && pct > bestPct) {
                bestPct = pct;
                bestIdx = i;
            }
        }

        ModDataComponents.BowlTobaccoGroup group = groups.get(bestIdx);
        if (group.isRegular()) {
            return colorForFlavor(group.flavor());
        } else if (group.isSelfMixed()) {
            String[] flavors = group.flavor().split("\\+");
            if (flavors.length == 0 || "mixed".equals(group.flavor())) return colorForSelfMixedBase(group.base());
            int red = 0;
            int green = 0;
            int blue = 0;
            for (String flavor : flavors) {
                int color = colorForFlavor(flavor);
                red += (color >> 16) & 0xFF;
                green += (color >> 8) & 0xFF;
                blue += color & 0xFF;
            }
            return ((red / flavors.length) << 16) | ((green / flavors.length) << 8) | (blue / flavors.length);
        }
        return 0xCCCCCC;
    }

    /** Разложить RGB int на компоненты [0..1] для DustParticleOptions / Vector3f. */
    public static float[] toNormalizedRGB(int rgb) {
        return new float[] {
                ((rgb >> 16) & 0xFF) / 255.0f,
                ((rgb >> 8) & 0xFF) / 255.0f,
                (rgb & 0xFF) / 255.0f
        };
    }

    /**
     * Плотность дыма (число частиц за спавн) в зависимости от количества мелассы в чаше.
     * Базовые 20 частиц + до +40 при максимуме мелассы (~20 мл на грамм табака).
     */
    public static int smokeDensity(ModDataComponents.HookahBowlContents contents) {
        if (contents == null || contents.groups().isEmpty()) return 20;
        double avgMolasses = contents.groups().stream()
                .mapToInt(ModDataComponents.BowlTobaccoGroup::molassesAmount)
                .average().orElse(0d);
        return 20 + (int) Math.round(Math.min(20.0, avgMolasses) * 2.0);
    }
}
