package net.sorukoma.quasium.mind.research;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.sorukoma.quasium.mind.ClientMind;
import net.sorukoma.quasium.mind.MindConfig;
import net.sorukoma.quasium.mind.MindData;
import net.sorukoma.quasium.mind.MindMath;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public enum Perk {
    IRON_LUNGS,
    CHARCOAL_BURNER,
    SOMMELIER,
    STONE_FACE,
    ZEN,
    SMOKE_RINGS,
    ENLIGHTENED,
    DREAM_CONTROL,
    FOCUS_TWO,
    CONDENSATES;

    private static final String RING_READY_AT = "RingPerkReadyAt";

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public boolean ownedBy(Player player) {
        return player.level().isClientSide ? ClientMind.perk(id()) : MindData.has(player, MindData.PERKS, id());
    }

    public static boolean known(String id) {
        for (Perk perk : values()) {
            if (perk.id().equals(id)) return true;
        }
        return false;
    }

    public static int air(Player player, int base) {
        if (!IRON_LUNGS.ownedBy(player)) return base;
        return MindMath.round(base * (1f - MindConfig.IRON_LUNGS_PCT.getAsInt() / 100f), player.getRandom().nextFloat());
    }

    public static int ringOffset(ServerPlayer player, int offsetMs) {
        long now = player.level().getGameTime();
        if (!SMOKE_RINGS.ownedBy(player) || now < MindData.time(player, RING_READY_AT)) return offsetMs;
        MindData.setTime(player, RING_READY_AT, now + MindConfig.SMOKE_RINGS_COOLDOWN_SECONDS.getAsInt() * 20L);
        return 0;
    }

    public static float lungMultiplier(Player player) {
        return IRON_LUNGS.ownedBy(player) ? 1f + MindConfig.IRON_LUNGS_PCT.getAsInt() / 100f : 1f;
    }

    public static float coolingMultiplier(Player player) {
        return player != null && CHARCOAL_BURNER.ownedBy(player) ? 1f - MindConfig.CHARCOAL_BURNER_PCT.getAsInt() / 100f : 1f;
    }

    public static int focusBonus(@Nullable Player caster, int bonus) {
        if (caster == null || !FOCUS_TWO.ownedBy(caster)) return bonus;
        return Math.round(bonus * (100 + MindConfig.FOCUS_TWO_PCT.getAsInt()) / 100f);
    }
}
