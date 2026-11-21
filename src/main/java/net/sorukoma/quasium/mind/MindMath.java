package net.sorukoma.quasium.mind;

public final class MindMath {
    private MindMath() {
    }

    public static float toleranceMultiplier(float tolerance) {
        return 1f - Math.max(0f, tolerance) / 100f;
    }

    public static int round(float exact, float roll) {
        int whole = (int) Math.floor(exact);
        return whole + (roll < exact - whole ? 1 : 0);
    }

    public static int discounted(int amount, int discountPct) {
        return Math.round(amount * (100 - discountPct) / 100f);
    }

    public static int total(int[] weights) {
        int total = 0;
        for (int weight : weights) total += Math.max(0, weight);
        return total;
    }

    public static int pick(int[] weights, int roll) {
        int left = roll;
        for (int index = 0; index < weights.length; index++) {
            left -= Math.max(0, weights[index]);
            if (left < 0) return index;
        }
        return weights.length - 1;
    }
}
