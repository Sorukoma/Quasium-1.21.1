package net.sorukoma.quasium.hookah;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.sorukoma.quasium.Config;
import net.sorukoma.quasium.component.ModDataComponents.BowlTobaccoGroup;
import net.sorukoma.quasium.component.ModDataComponents.HookahBowlContents;
import net.sorukoma.quasium.data.PlayerKaifData;
import net.sorukoma.quasium.util.HookahLogic;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public final class PuffEffects {
    private static final int TICKS_PER_SECOND = 20;
    private static final int NAUSEA_SECONDS = 5;

    private PuffEffects() {
    }

    public static void apply(ServerPlayer player, int puffTicks, HookahBowlContents contents) {
        if (!contents.isCompatible()) {
            player.sendSystemMessage(Component.translatable("quasium.message.incompatible_mix_effects"));
            nauseaFromNegativeKaif(player);
            return;
        }
        Map<Integer, Integer> groupPercentages = contents.getGroupPercentages();
        for (int groupIdx = 0; groupIdx < contents.groups().size(); groupIdx++) {
            Integer percentage = groupPercentages.get(groupIdx);
            if (percentage == null) continue;
            int durationSeconds = (int) Math.ceil(percentage * puffTicks / (double) TICKS_PER_SECOND / 100.0);
            int durationTicks = durationSeconds * TICKS_PER_SECOND;
            if (durationTicks <= 0) continue;
            BowlTobaccoGroup group = contents.groups().get(groupIdx);
            if (group.isRegular()) {
                applyRegular(player, group, percentage, durationTicks);
            } else {
                applySelfMixed(player, group, durationTicks);
            }
        }
    }

    public static void stack(LivingEntity smoker, Holder<MobEffect> effect, int ticks, int amplifier) {
        MobEffectInstance current = smoker.getEffect(effect);
        boolean sameLevel = current != null && current.getAmplifier() == amplifier && !current.isInfiniteDuration();
        int duration = sameLevel
                ? HookahLogic.stackedEffectTicks(current.getDuration(), ticks, Config.PUFF_EFFECT_CAP_SECONDS.get() * TICKS_PER_SECOND)
                : ticks;
        smoker.addEffect(new MobEffectInstance(effect, duration, amplifier));
    }

    public static void nauseaFromNegativeKaif(ServerPlayer player) {
        int kaif = PlayerKaifData.getKaifValue(player);
        int nauseaLevel = kaif < 0 ? -kaif / 10 : 0;
        if (nauseaLevel <= 0) return;
        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, TICKS_PER_SECOND * NAUSEA_SECONDS, Math.min(nauseaLevel - 1, 4)));
    }

    public static boolean containsCitrus(String flavor) {
        return flavor.contains("lemon") || flavor.contains("lime")
                || flavor.contains("orange") || flavor.contains("grapefruit");
    }

    private static void applyRegular(ServerPlayer player, BowlTobaccoGroup group, int percentage, int durationTicks) {
        Holder<MobEffect> effect = effectForFlavor(group.flavor());
        if (effect == null) return;
        stack(player, effect, durationTicks, percentage > 70 ? 1 : 0);
    }

    private static void applySelfMixed(ServerPlayer player, BowlTobaccoGroup group, int durationTicks) {
        String flavor = group.flavor();
        float essenceMultiplier = 1.0f + group.essenceAmount() * 0.15f;
        int mixTicks = Math.max(durationTicks, Math.round(durationTicks * essenceMultiplier));
        if (flavor.contains("mint") && containsCitrus(flavor)) stack(player, MobEffects.MOVEMENT_SPEED, mixTicks, 1);
        if (flavor.contains("berry") && flavor.contains("honey")) stack(player, MobEffects.REGENERATION, mixTicks, 0);
        int strengthBonus = group.strength() > 0.5f ? 1 : 0;
        for (MobEffectInstance template : group.effects()) {
            stack(player, template.getEffect(), mixTicks, template.getAmplifier() + strengthBonus);
        }
    }

    @Nullable
    private static Holder<MobEffect> effectForFlavor(String flavor) {
        return switch (flavor) {
            case "mint", "anise" -> MobEffects.MOVEMENT_SPEED;
            case "lemon", "lime", "orange", "grapefruit", "cinnamon" -> MobEffects.DAMAGE_BOOST;
            case "berry" -> MobEffects.JUMP;
            case "lavender", "rose" -> MobEffects.REGENERATION;
            case "cocoa", "coconut" -> MobEffects.DAMAGE_RESISTANCE;
            case "apple" -> MobEffects.HEALTH_BOOST;
            case "watermelon" -> MobEffects.WATER_BREATHING;
            case "grape" -> MobEffects.LUCK;
            case "cherry" -> MobEffects.NIGHT_VISION;
            case "vanilla" -> MobEffects.SLOW_FALLING;
            case "coffee" -> MobEffects.DIG_SPEED;
            default -> null;
        };
    }
}
