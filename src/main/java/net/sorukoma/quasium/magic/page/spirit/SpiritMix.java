package net.sorukoma.quasium.magic.page.spirit;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.component.ModDataComponents;
import net.sorukoma.quasium.component.ModDataComponents.BowlTobaccoGroup;
import net.sorukoma.quasium.component.ModDataComponents.HookahBowlContents;
import net.sorukoma.quasium.selection.SelectionRegistry;
import net.sorukoma.quasium.selection.genetics.Genetics;
import net.sorukoma.quasium.selection.quality.LeafQuality;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SpiritMix {
    public static final String TOKEN = "spirit_mix";
    public static final String BASE = "spirit";
    public static final int WEIGHT = 30;
    public static final float STRENGTH = 0.9f;
    public static final ResourceLocation RESEARCH = ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "research_mirror_spiritism");
    private static final String FLAVOR = "spirit_";
    private static final Map<UUID, Integer> INHALED = new ConcurrentHashMap<>();

    private SpiritMix() {
    }

    public static String flavor(int stars) {
        return FLAVOR + Genetics.clamp(stars);
    }

    public static ItemStack pack(int leafQuality) {
        int stars = Genetics.clamp(leafQuality);
        ItemStack pack = new ItemStack(SpiritRegistry.SPIRIT_MIX.get());
        pack.set(ModDataComponents.ESSENCE_TYPE.get(), flavor(stars));
        pack.set(SelectionRegistry.LEAF_QUALITY.get(), stars);
        pack.set(SelectionRegistry.KAIF_PCT.get(), LeafQuality.kaifPct(stars));
        return pack;
    }

    public static boolean inhaled(ServerPlayer player, ItemStack bowl) {
        int stars = stars(bowl.get(ModDataComponents.HOOKAH_BOWL_CONTENTS.get()));
        if (stars <= 0) {
            INHALED.remove(player.getUUID());
            return false;
        }
        INHALED.put(player.getUUID(), stars);
        return true;
    }

    public static int stars(ServerPlayer player) {
        return INHALED.getOrDefault(player.getUUID(), 0);
    }

    public static void forget(UUID playerId) {
        INHALED.remove(playerId);
    }

    static int stars(@Nullable HookahBowlContents contents) {
        if (contents == null) return 0;
        int best = 0;
        for (int index : contents.slotAssignments().values()) {
            if (index < 0 || index >= contents.groups().size()) continue;
            BowlTobaccoGroup group = contents.groups().get(index);
            if (group.isSelfMixed() && BASE.equals(group.base())) best = Math.max(best, starsOf(group.flavor()));
        }
        return best;
    }

    static int starsOf(String flavor) {
        int digit = flavor.isEmpty() ? -1 : Character.digit(flavor.charAt(flavor.length() - 1), 10);
        return Genetics.clamp(digit);
    }
}
