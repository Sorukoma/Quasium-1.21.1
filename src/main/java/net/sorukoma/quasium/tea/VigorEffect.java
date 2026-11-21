package net.sorukoma.quasium.tea;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.data.PlayerKaifData;
import net.sorukoma.quasium.ecology.EcologyConfig;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID)
public class VigorEffect extends MobEffect {
    public static final int WITHDRAWAL_TICKS = 600;
    private static final int COLOR = 0xC8823C;
    private static final int SECOND = 20;

    public VigorEffect() {
        super(MobEffectCategory.BENEFICIAL, COLOR);
    }

    public static int kaifEveryTicks() {
        int kaif = EcologyConfig.TEA_KAIF.getAsInt();
        return kaif <= 0 ? 0 : Math.max(SECOND, EcologyConfig.VIGOR_SECONDS.getAsInt() * SECOND / kaif);
    }

    public static boolean pausesOverdose(Player player) {
        return player.hasEffect(TeaBlocks.VIGOR);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % SECOND == 0;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (!(entity instanceof ServerPlayer player)) return true;
        MobEffectInstance vigor = player.getEffect(TeaBlocks.VIGOR);
        if (vigor == null) return true;
        int left = vigor.getDuration();
        int every = kaifEveryTicks();
        if (every > 0 && left % every == 0) PlayerKaifData.addKaif(player, 1);
        int extra = EcologyConfig.VIGOR_WITHDRAWAL_MULTIPLIER.getAsInt() - 1;
        if (extra > 0 && left % WITHDRAWAL_TICKS == 0 && PlayerKaifData.getKaifValue(player) < 0) PlayerKaifData.addKaif(player, extra);
        return true;
    }

    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (event.getEffectInstance().is(MobEffects.CONFUSION) && event.getEntity().hasEffect(TeaBlocks.VIGOR)) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }
}
