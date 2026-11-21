package net.sorukoma.quasium.ecology;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.api.KaifChangeEvent;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID)
public final class SmokeAura {
    private SmokeAura() {
    }

    public static boolean thick(Level level, BlockPos pos) {
        return SmokeDensity.get(level, pos) >= EcologyConfig.BONUS_THRESHOLD.getAsInt();
    }

    public static int bonusPct(Level level, BlockPos pos, ModConfigSpec.IntValue bonus) {
        return thick(level, pos) ? bonus.getAsInt() : 0;
    }

    public static int boosted(Level level, BlockPos pos, int amount, ModConfigSpec.IntValue bonus) {
        return SmogMath.withBonus(amount, bonusPct(level, pos, bonus), level.random.nextDouble());
    }

    @SubscribeEvent
    public static void onKaifChange(KaifChangeEvent event) {
        Player player = event.player();
        if (event.delta() <= 0 || player.level().isClientSide) return;
        event.setDelta(boosted(player.level(), player.blockPosition(), event.delta(), EcologyConfig.BONUS_KAIF_PCT));
    }
}
