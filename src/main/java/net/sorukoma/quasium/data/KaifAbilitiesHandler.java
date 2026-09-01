package net.sorukoma.quasium.data;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.List;

/**
 * Обработчик пассивных бонусов каифа (Kaif-abilities).
 * При достижении порогов каифа даёт игроку соответствующие эффекты.
 *
 * Пороги:
 *   kaif >= 20 : Haste I (ускорение копания/добычи)
 *   kaif >= 40 : Night Vision I
 *   kaif >= 60 : Regeneration I
 *   kaif >= 80 : Resistance I
 */
public class KaifAbilitiesHandler {

    private static final int MIN_KAIF_HASTE = 20;
    private static final int MIN_KAIF_NIGHT_VISION = 40;
    private static final int MIN_KAIF_REGENERATION = 60;
    private static final int MIN_KAIF_RESISTANCE = 80;

    private static final String KAIF_ABILITY_HASTE_KEY = "QuasiumKaifAbilityHaste";
    private static final String KAIF_ABILITY_NIGHT_VISION_KEY = "QuasiumKaifAbilityNightVision";
    private static final String KAIF_ABILITY_REGENERATION_KEY = "QuasiumKaifAbilityRegeneration";
    private static final String KAIF_ABILITY_RESISTANCE_KEY = "QuasiumKaifAbilityResistance";

    private KaifAbilitiesHandler() {}

    /**
     * Вызывается в onLivingTick для каждого серверного игрока.
     */
    public static void tick(ServerPlayer player) {
        int kaif = PlayerKaifData.getKaifValue(player);
        if (kaif == 0) return;

        applyOrRemoveAbility(player, kaif, MIN_KAIF_HASTE, MobEffects.DIG_SPEED,
                KAIF_ABILITY_HASTE_KEY,
                Component.translatable("quasium.ability.haste"),
                Component.translatable("quasium.ability.haste_desc"));

        applyOrRemoveAbility(player, kaif, MIN_KAIF_NIGHT_VISION, MobEffects.NIGHT_VISION,
                KAIF_ABILITY_NIGHT_VISION_KEY,
                Component.translatable("quasium.ability.night_vision"),
                Component.translatable("quasium.ability.night_vision_desc"));

        applyOrRemoveAbility(player, kaif, MIN_KAIF_REGENERATION, MobEffects.REGENERATION,
                KAIF_ABILITY_REGENERATION_KEY,
                Component.translatable("quasium.ability.regeneration"),
                Component.translatable("quasium.ability.regeneration_desc"));

        applyOrRemoveAbility(player, kaif, MIN_KAIF_RESISTANCE, MobEffects.DAMAGE_RESISTANCE,
                KAIF_ABILITY_RESISTANCE_KEY,
                Component.translatable("quasium.ability.resistance"),
                Component.translatable("quasium.ability.resistance_desc"));
    }

    private static void applyOrRemoveAbility(ServerPlayer player, int kaif, int threshold,
                                              Holder<MobEffect> effect, String key,
                                              Component name, Component desc) {
        boolean shouldHave = kaif >= threshold;
        boolean has = player.getPersistentData().getBoolean(key);

        if (shouldHave && !has) {
            player.addEffect(new MobEffectInstance(effect, 600, 0));
            player.getPersistentData().putBoolean(key, true);
            player.sendSystemMessage(Component.translatable("quasium.ability.gained", name));
        } else if (!shouldHave && has) {
            player.removeEffect(effect);
            player.getPersistentData().putBoolean(key, false);
        }
    }

    /**
     * Получить текстовое представление текущего ранга каифа для HUD.
     */
    public static RankInfo getKaifRank(int kaif) {
        if (kaif < -80) return new RankInfo("quasium.rank.overdose", 0xFF0000);
        if (kaif < -50) return new RankInfo("quasium.rank.severe_withdrawal", 0xAA0000);
        if (kaif < -20) return new RankInfo("quasium.rank.withdrawal", 0x888888);
        if (kaif < 0)   return new RankInfo("quasium.rank.sober", 0xAAAAAA);
        if (kaif < 20)  return new RankInfo("quasium.rank.relaxed", 0x88CC88);
        if (kaif < 40)  return new RankInfo("quasium.rank.warm", 0x88CCFF);
        if (kaif < 60)  return new RankInfo("quasium.rank.flight", 0xFFAA00);
        if (kaif < 80)  return new RankInfo("quasium.rank.trance", 0xFF66AA);
        if (kaif < 100) return new RankInfo("quasium.rank.euphoria", 0xFFDD00);
        return new RankInfo("quasium.rank.max", 0xFF4444);
    }

    /**
     * Получить список активных бонусов для отображения.
     */
    public static void getActiveAbilityNames(int kaif, List<Component> result) {
        if (kaif >= MIN_KAIF_HASTE) {
            result.add(Component.translatable("quasium.ability.haste"));
        }
        if (kaif >= MIN_KAIF_NIGHT_VISION) {
            result.add(Component.translatable("quasium.ability.night_vision"));
        }
        if (kaif >= MIN_KAIF_REGENERATION) {
            result.add(Component.translatable("quasium.ability.regeneration"));
        }
        if (kaif >= MIN_KAIF_RESISTANCE) {
            result.add(Component.translatable("quasium.ability.resistance"));
        }
    }

    public record RankInfo(String translationKey, int color) {}
}
