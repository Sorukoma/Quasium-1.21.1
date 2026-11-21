package net.sorukoma.quasium.client;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
public final class ClientMagicData {
    private static float lungs;
    private static int capacity = 100;
    @Nullable
    private static ResourceLocation spell;
    private static int cost;
    private static float charge = 1f;
    private static int holdTicks;
    private static int coughTicks;
    private static String pageLabel = "";
    private static String missingNotes = "";
    private static Set<String> scanned = Set.of();
    private static int insight;
    private static List<BlockPos> glowingOres = List.of();
    private static long glowUntil;

    private ClientMagicData() {
    }

    public static void setCastState(float newLungs, int newCapacity, @Nullable ResourceLocation newSpell, int newCost,
                                    float newCharge, int newHoldTicks, int newCoughTicks) {
        lungs = newLungs;
        capacity = Math.max(1, newCapacity);
        spell = newSpell;
        cost = newCost;
        charge = newCharge;
        holdTicks = newHoldTicks;
        coughTicks = newCoughTicks;
    }

    public static void setPageState(String label, String missing) {
        pageLabel = label;
        missingNotes = missing;
    }

    public static void setScanData(Set<String> newScanned, int newInsight) {
        scanned = Set.copyOf(newScanned);
        insight = newInsight;
    }

    public static void setGlowingOres(List<BlockPos> positions, long until) {
        glowingOres = List.copyOf(positions);
        glowUntil = until;
    }

    public static void reset() {
        setCastState(0f, 100, null, 0, 1f, 0, 0);
        setPageState("", "");
        setScanData(Set.of(), 0);
        setGlowingOres(List.of(), 0L);
    }

    public static float lungs() { return lungs; }
    public static int capacity() { return capacity; }
    @Nullable public static ResourceLocation spell() { return spell; }
    public static int cost() { return cost; }
    public static float charge() { return charge; }
    public static int holdTicks() { return holdTicks; }
    public static int coughTicks() { return coughTicks; }
    public static String pageLabel() { return pageLabel; }
    public static String missingNotes() { return missingNotes; }
    public static boolean isScanned(String key) { return scanned.contains(key); }
    public static int scannedCount() { return scanned.size(); }
    public static int insight() { return insight; }
    public static List<BlockPos> glowingOres() { return glowingOres; }
    public static long glowUntil() { return glowUntil; }
}
